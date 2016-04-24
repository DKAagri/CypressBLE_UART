package com.cypress.cysmart.BLEServiceFragments;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;
import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.cypress.cysmart.BLEConnectionServices.BluetoothLeService.writeDreamweaverCsv;

/**
 * Fragment to display the RGB service
 */
public class RGBFragment extends Fragment {

    List<String[]> csvvalues=null;
    int csvindex=0;
    int _frequency=0;
    int _i=0, _r=0 , _g=0 , _b=0 , _f=0;
    int _phase =0,  _duty_cycle=0;

    // GATT service and characteristics
    private static BluetoothGattService mCurrentservice;
    private static BluetoothGattCharacteristic mReadCharacteristic;

    // Data view variables
    private ImageView mRGBcanavs;
    private ImageView mcolorpicker;
    private ViewGroup mViewContainer;


    // added for the Send button in TX BLE
    private EditText mTextTX;
    private String mHexTx;


    //added for mediacontroller and play button
    private Button btnPlay,btnStop;
    private static MediaPlayer mMediaPlayer;
    Timer timer;
    private TextView mTextTimestamp;
    private SeekBar mseekBar;

    private ImageView mColorindicator;
    private SeekBar mIntensityBar;
    private RelativeLayout mParentRelLayout;

    //ProgressDialog
    private ProgressDialog mProgressDialog;

    // Data variables
    private float mWidth;
    private float mHeight;
    private String mHexRed;
    private String mHexGreen;
    private String mHexBlue;

    private View mRootView;
    // Flag
    private boolean mIsReaded = false;
    private Bitmap mBitmap;
    private int mRed, mGreen, mBlue, mIntensity;
    /**
     * BroadcastReceiver for receiving the GATT server status
     */
    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDING) {
                    Logger.i("Bonding is in process....");
                    Utils.bondingProgressDialog(getActivity(), mProgressDialog, true);
                } else if (state == BluetoothDevice.BOND_BONDED) {
                    String dataLog = getResources().getString(R.string.dl_commaseparator)
                            + "[" + BluetoothLeService.getmBluetoothDeviceName() + "|"
                            + BluetoothLeService.getmBluetoothDeviceAddress() + "]" +
                            getResources().getString(R.string.dl_commaseparator) +
                            getResources().getString(R.string.dl_connection_paired);
                    Logger.datalog(dataLog);
                    Utils.bondingProgressDialog(getActivity(), mProgressDialog, false);
                    getGattData();
                } else if (state == BluetoothDevice.BOND_NONE) {
                    String dataLog = getResources().getString(R.string.dl_commaseparator)
                            + "[" + BluetoothLeService.getmBluetoothDeviceName() + "|"
                            + BluetoothLeService.getmBluetoothDeviceAddress() + "]" +
                            getResources().getString(R.string.dl_commaseparator) +
                            getResources().getString(R.string.dl_connection_unpaired);
                    Logger.datalog(dataLog);
                    Utils.bondingProgressDialog(getActivity(), mProgressDialog, false);
                }
            }
        }
    };

    public RGBFragment create(BluetoothGattService currentservice) {
        mCurrentservice = currentservice;
        RGBFragment fragment = new RGBFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            mRootView = inflater.inflate(R.layout.rgb_view_landscape, container,
                    false);
        } else {
            mRootView = inflater.inflate(R.layout.rgb_view_portrait, container,
                    false);
        }

        mMediaPlayer = MediaPlayer.create(getActivity(), R.raw.trip_to_the_forest);

        // getActivity().getActionBar().setTitle(R.string.rgb_led);
        setUpControls();
        setDefaultColorPickerPositionColor();
        setHasOptionsMenu(true);
        return mRootView;
    }


    private void setDefaultColorPickerPositionColor() {
        ViewTreeObserver observer = mcolorpicker.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mcolorpicker.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                int[] locations = new int[2];
                mcolorpicker.getLocationOnScreen(locations);
                int x = locations[0];
                int y = locations[1];
                if (x < mBitmap.getWidth() && y < mBitmap.getHeight()) {
                    int p = mBitmap.getPixel(x, y);
                    if (p != 0) {
                        mRed = Color.red(p);
                        mGreen = Color.green(p);
                        mBlue = Color.blue(p);
                        Logger.i("r--->" + mRed + "g-->" + mGreen + "b-->" + mBlue);
                        UIupdation();
                    }
                }
            }
        });
    }

    /**
     * Method to set up the GAMOT view
     */
    void setUpControls() {
        mParentRelLayout = (RelativeLayout) mRootView.findViewById(R.id.parent);
        mParentRelLayout.setClickable(true);

        csvvalues = readCsv();
        mTextTimestamp = (TextView) mRootView.findViewById(R.id.timeStamp);
        mRGBcanavs = (ImageView) mRootView.findViewById(R.id.imgrgbcanvas);
        mcolorpicker = (ImageView) mRootView.findViewById(R.id.imgcolorpicker);


        mColorindicator = (ImageView) mRootView.findViewById(R.id.txtcolorindicator);
        mViewContainer = (ViewGroup) mRootView.findViewById(R.id.viewgroup);

        mIntensityBar = (SeekBar) mRootView.findViewById(R.id.intencitychanger);
        mProgressDialog = new ProgressDialog(getActivity());

        BitmapDrawable mBmpdwbl = (BitmapDrawable) mRGBcanavs.getDrawable();
        mBitmap = mBmpdwbl.getBitmap();

        Drawable d = getResources().getDrawable(R.drawable.gamut);
        mRGBcanavs.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE
                        || event.getAction() == MotionEvent.ACTION_DOWN
                        || event.getAction() == MotionEvent.ACTION_UP) {

                    float x = event.getX();
                    float y = event.getY();
                    if (x >= 0 && y >= 0) {

                        int x1 = (int) x;
                        int y1 = (int) y;
                        if (x < mBitmap.getWidth() && y < mBitmap.getHeight()) {
                            int p = mBitmap.getPixel(x1, y1);
                            if (p != 0) {
                                if (x > mRGBcanavs.getMeasuredWidth())
                                    x = mRGBcanavs.getMeasuredWidth();
                                if (y > mRGBcanavs.getMeasuredHeight())
                                    y = mRGBcanavs.getMeasuredHeight();
                                setwidth(1.f / mRGBcanavs.getMeasuredWidth() * x);
                                setheight(1.f - (1.f / mRGBcanavs.getMeasuredHeight() * y));
                                mRed = Color.red(p);
                                mGreen = Color.green(p);
                                mBlue = Color.blue(p);
                                UIupdation();
                                mIsReaded = false;
                                moveTarget();
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        });

        mseekBar=(SeekBar) mRootView.findViewById(R.id.seekBar);
        mseekBar.setMax(csvvalues.size());

        mIntensity = mIntensityBar.getProgress();
        /*mTextalpha.setText(String.format("0x%02x", mIntensity));*/
        mIntensityBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress,   boolean fromUser) {
                mIntensity = progress;
                //UIupdation();
                mIsReaded = false;
                _i=progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                mIsReaded = false;
                BluetoothLeService.writeCharacteristicRGB(mReadCharacteristic,
                        mRed, mGreen, mBlue, mIntensity);

            }
        });


        btnStop=  (Button) mRootView.findViewById(R.id.buttonStopSong);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                    csvindex=0;
                }
                mMediaPlayer.stop();
            }
        });

        btnPlay=  (Button) mRootView.findViewById(R.id.buttonPlayAudio);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mMediaPlayer.start();

                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {

                        //if(!(csvvalues.get(csvindex)[2]).equalsIgnoreCase("0")){

                            if( ((int)(Double.parseDouble(csvvalues.get(csvindex)[2])))!=_f &&
                                    Integer.parseInt(csvvalues.get(csvindex)[5])!=_r  &&
                                    Integer.parseInt(csvvalues.get(csvindex)[6])!=_g &&
                                    Integer.parseInt(csvvalues.get(csvindex)[7])!=_b &&
                                    Integer.parseInt(csvvalues.get(csvindex)[4].replace("%", ""))!=_duty_cycle &&
                                    Integer.parseInt(csvvalues.get(csvindex)[8])!=_phase
                                    ){

                                _f=(int) (Double.parseDouble(csvvalues.get(csvindex)[2]));

                                if( (csvvalues.get(csvindex)[2]).equalsIgnoreCase("7.83")) {
                                    _frequency=68;
                                }else{
                                    _frequency= (int) (Double.parseDouble(csvvalues.get(csvindex)[2])*10-10);
                                }
                                /*
                                if( (csvvalues.get(csvindex)[2]).equalsIgnoreCase("7.83")) {
                                    _frequency=68;
                                }else{
                                    _frequency= (int) (Double.parseDouble(csvvalues.get(csvindex)[2])*10-10);
                                }*/

                                //_i=Integer.parseInt(csvvalues.get(csvindex)[3])/10;
                                _r=Integer.parseInt(csvvalues.get(csvindex)[5]);
                                _g=Integer.parseInt(csvvalues.get(csvindex)[6]);
                                _b=Integer.parseInt(csvvalues.get(csvindex)[7]);

                                _duty_cycle= Integer.parseInt(csvvalues.get(csvindex)[4].replace("%", ""));
                                _phase = Integer.parseInt(csvvalues.get(csvindex)[8]);

                                Logger.v("Frequency :"+ _f + " altered freq :"+ _frequency+" rgb:"+_r+" "+_g+" "+_b);

                                writeDreamweaverCsv(mReadCharacteristic,
                                        _frequency,
                                        _i,
                                        _duty_cycle,
                                        _r, _g, _b,
                                        _phase);
                            }

                        //}

                        setTimeStampText((csvvalues.get(csvindex)[0]).toString() ,
                                Integer.parseInt(csvvalues.get(csvindex)[1]),
                                String.format("#%02x%02x%02x%02x", (255) ,(2^(8- _r)),(2^(8- _g)),(2^(8- _b))));

                        csvindex++;
                    }
                }, 0, 1000);

            }
        });
    }

/*
    private String rgbToHex(int R, int G, int B) {return toHex(R)+toHex(G)+toHex(B)}

    private String toHex(int n) {
        n = Integer.parseInt(n,10);
        if () return "00";
        n = Math.max(0,Math.min(n,255));
        return "0123456789ABCDEF".charAt((n-n%16)/16)
                + "0123456789ABCDEF".charAt(n%16);
    }
*/

    private void setTimeStampText(final String timestamp, final int progress , final String hexColor){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextTimestamp.setText(timestamp);
                mseekBar.setProgress(progress);
                Logger.v("hexColor :"+ hexColor);
                try {
                    mColorindicator.setBackgroundColor(Color.parseColor(hexColor));
                }catch(Exception e){
                    Logger.e(" exception mcolcor indicator " + e );
                }
            }
        });
    }

    @Override
    public void onResume() {
        getActivity().registerReceiver(mGattUpdateReceiver,Utils.makeGattUpdateIntentFilter());
        getGattData();
        Utils.setUpActionBar(getActivity(),getResources().getString(R.string.rgb_led));
        super.onResume();
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mGattUpdateReceiver);
        super.onDestroy();
    }


    private void UIupdation() {
       // String hexColor = String.format("#%02x%02x%02x%02x", mIntensity, mRed, mGreen, mBlue);
       // mColorindicator.setBackgroundColor(Color.parseColor(hexColor));
       /* mTextalpha.setText(String.format("0x%02x", mIntensity));*/

      //  mHexRed = String.format("0x%02x", mRed);
       /* mTextred.setText(mHexRed);*/

      //  mHexGreen = String.format("0x%02x", mGreen);
       /* mTextgreen.setText(mHexGreen);*/

      //  mHexBlue = String.format("0x%02x", mBlue);
       /* mTextblue.setText(mHexBlue);*/

      /*  mTextalpha.setText(String.format("0x%02x", mIntensity));*/

        /*
        try {
            Logger.i("Writing value-->" + mRed + " " + mGreen + " " + mBlue + " " + mIntensity);
            BluetoothLeService.writeCharacteristicRGB(
                    mReadCharacteristic,
                    mRed,
                    mGreen,
                    mBlue,
                    mIntensity);
        } catch (Exception e) {
            Logger.e("exception");
        }*/

    }

    /**
     * Method to get required characteristics from service
     */
    void getGattData() {
        List<BluetoothGattCharacteristic> gattCharacteristics = mCurrentservice
                .getCharacteristics();
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            String uuidchara = gattCharacteristic.getUuid().toString();
            if (uuidchara.equalsIgnoreCase(GattAttributes.RGB_LED) || uuidchara.equalsIgnoreCase(GattAttributes.RGB_LED_CUSTOM)) {
                mReadCharacteristic = gattCharacteristic;
                break;
            }
        }
    }

    /**
     * Moving the color picker object
     */

    void moveTarget() {
        float x = getwidth() * mRGBcanavs.getMeasuredWidth();
        float y = (1.f - getheigth()) * mRGBcanavs.getMeasuredHeight();
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mcolorpicker.getLayoutParams();
        layoutParams.leftMargin = (int) (mRGBcanavs.getLeft() + x
                - Math.floor(mcolorpicker.getMeasuredWidth() / 2) - mViewContainer
                .getPaddingLeft());
        layoutParams.topMargin = (int) (mRGBcanavs.getTop() + y
                - Math.floor(mcolorpicker.getMeasuredHeight() / 2) - mViewContainer
                .getPaddingTop());
        mcolorpicker.setLayoutParams(layoutParams);
    }

    private float getwidth() {
        return mWidth;
    }

    private float getheigth() {
        return mHeight;
    }

    private void setwidth(float sat) {
        mWidth = sat;
    }

    private void setheight(float val) {
        mHeight = val;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.global, menu);
        MenuItem graph = menu.findItem(R.id.graph);
        MenuItem log = menu.findItem(R.id.log);
        MenuItem search = menu.findItem(R.id.search);
        MenuItem pairCache = menu.findItem(R.id.pairing);
        if (Utils.getBooleanSharedPreference(getActivity(), Constants.PREF_PAIR_CACHE_STATUS)) {
            pairCache.setChecked(true);
        } else {
            pairCache.setChecked(false);
        }
        search.setVisible(false);
        graph.setVisible(false);
        log.setVisible(true);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mRootView = inflater.inflate(R.layout.rgb_view_landscape, null);
            ViewGroup rootViewG = (ViewGroup) getView();
            // Remove all the existing views from the root view.
            rootViewG.removeAllViews();
            rootViewG.addView(mRootView);
            setUpControls();
            setDefaultColorPickerPositionColor();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mRootView = inflater.inflate(R.layout.rgb_view_portrait, null);
            ViewGroup rootViewG = (ViewGroup) getView();
            // Remove all the existing views from the root view.
            rootViewG.removeAllViews();
            rootViewG.addView(mRootView);
            setUpControls();
            setDefaultColorPickerPositionColor();

        }

    }



    public final List<String[]> readCsv() {
        List<String[]> questionList = new ArrayList<String[]>();

        try {
            InputStream csvStream= getResources().openRawResource(R.raw.trip_to_the_forest_file);
            Logger.v("Csv file read as stream ");
            InputStreamReader csvStreamReader = new InputStreamReader(csvStream);
            CSVReader csvReader = new CSVReader(csvStreamReader);
            String[] line;

            // throw away the header
            csvReader.readNext();

            while ((line = csvReader.readNext()) != null) {
                questionList.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return questionList;
    }
}
