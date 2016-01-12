/*
 * Copyright Cypress Semiconductor Corporation, 2014-2015 All rights reserved.
 *
 * This software, associated documentation and materials ("Software") is
 * owned by Cypress Semiconductor Corporation ("Cypress") and is
 * protected by and subject to worldwide patent protection (UnitedStates and foreign), United States copyright laws and international
 * treaty provisions. Therefore, unless otherwise specified in a separate license agreement between you and Cypress, this Software
 * must be treated like any other copyrighted material. Reproduction,
 * modification, translation, compilation, or representation of this
 * Software in any other form (e.g., paper, magnetic, optical, silicon)
 * is prohibited without Cypress's express written permission.
 *
 * Disclaimer: THIS SOFTWARE IS PROVIDED AS-IS, WITH NO WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO,
 * NONINFRINGEMENT, IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE. Cypress reserves the right to make changes
 * to the Software without notice. Cypress does not assume any liability
 * arising out of the application or use of Software or any product or
 * circuit described in the Software. Cypress does not authorize its
 * products for use as critical components in any products where a
 * malfunction or failure may reasonably be expected to result in
 * significant injury or death ("High Risk Product"). By including
 * Cypress's product in a High Risk Product, the manufacturer of such
 * system or application assumes all risk of such use and in doing so
 * indemnifies Cypress against all liability.
 *
 * Use of this Software may be limited by and subject to the applicable
 * Cypress software license agreement.
 *
 *
 */

package com.cypress.cysmart.BLEServiceFragments;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.DecimalTextWatcher;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;


/**
 * Fragment to display the Cycling speed cadence
 */
public class CSCService extends Fragment {

    // GATT Service and Characteristics
    private static BluetoothGattService mService;
    private static BluetoothGattCharacteristic mNotifyCharacteristic;


    // Data field variables
    private TextView mDistanceRan;
    private TextView mCadence;
    private TextView mCaloriesBurnt;
    private TextView mDistanceUnit;
    private Chronometer mTimer;
    private EditText mWeightEdittext;
    private EditText mRadiusEdittext;

    //ProgressDialog
    private ProgressDialog mProgressDialog;

    private double mWeightInt;
    private String mWeightString;
    public static double mRadiusInt = 0.0;
    private String mRadiusString;

    /**
     * aChart variables
     */
    private LinearLayout mGraphLayoutParent;
    private double mGraphLastXValue = 0;
    private double mPreviosTime = 0;
    private double mCurrentTime = 0;
    private GraphicalView mChart;
    private XYSeries mDataSeries;

    //Constants
    private static final int MAX_WEIGHT = 200;
    private static final int ZERO = 0;
    private static final float WEIGHT_ONE = 1;

    private static final float MINIMUM_RADIUS = 300;
    private static final float MAXIMUM_RADIUS = 725;

    /**
     * BroadcastReceiver for receiving the GATT server status
     */
    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();

            // GATT Data available
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                // Check CSC value
                if (extras.containsKey(Constants.EXTRA_CSC_VALUE)) {
                    ArrayList<String> received_csc_data = intent
                            .getStringArrayListExtra(Constants.EXTRA_CSC_VALUE);
                    displayLiveData(received_csc_data);
                }
            }
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDING) {
                    // Bonding...
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

    public CSCService create(BluetoothGattService service) {
        mService = service;
        return new CSCService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.cyclingspeed_n_cadence,
                container, false);
        mDistanceRan = (TextView) rootView.findViewById(R.id.cycling_distance);
        mCadence = (TextView) rootView.findViewById(R.id.cadence);
        mDistanceUnit = (TextView) rootView.findViewById(R.id.distance_unit);
        mCaloriesBurnt = (TextView) rootView.findViewById(R.id.calories_burnt);
        mTimer = (Chronometer) rootView.findViewById(R.id.time_counter);
        mWeightEdittext = (EditText) rootView.findViewById(R.id.weight_data);
        mWeightEdittext.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        mWeightEdittext.addTextChangedListener(new DecimalTextWatcher(mWeightEdittext));
        mRadiusEdittext = (EditText) rootView.findViewById(R.id.radius_data);
        mRadiusEdittext.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        mRadiusEdittext.addTextChangedListener(new DecimalTextWatcher(mRadiusEdittext));
        mProgressDialog = new ProgressDialog(getActivity());

        // Setting up chart
        setupChart(rootView);

        // Start/Stop listener
        Button mStartStopBtn = (Button) rootView.findViewById(R.id.start_stop_btn);
        mStartStopBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Button mBtn = (Button) v;
                String buttonText = mBtn.getText().toString();
                String startText = getResources().getString(
                        R.string.blood_pressure_start_btn);
                String stopText = getResources().getString(
                        R.string.blood_pressure_stop_btn);
                mWeightString = mWeightEdittext.getText().toString();
                mRadiusString = mRadiusEdittext.getText().toString();
                try {
                    mWeightInt = Double.parseDouble(mWeightString);
                    mRadiusInt = Double.parseDouble(mRadiusString);
                } catch (NumberFormatException e) {
                    mRadiusInt = ZERO;
                    mWeightInt = ZERO;
                }

                //Weight Entered validation
                if ((mWeightString.equalsIgnoreCase("") || mWeightString.equalsIgnoreCase(".")) && buttonText.equalsIgnoreCase(startText)) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.csc_weight_toast_empty), Toast.LENGTH_SHORT).show();
                    mCaloriesBurnt.setText("0.00");
                }

                if ((mWeightString.equalsIgnoreCase("0.")
                        || mWeightString.equalsIgnoreCase("0")) && buttonText.equalsIgnoreCase(startText)) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.csc_weight_toast_zero), Toast.LENGTH_SHORT).show();
                }

                if (mWeightInt < WEIGHT_ONE && mWeightInt > ZERO && buttonText.equalsIgnoreCase(startText)) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.csc_weight_toast_zero), Toast.LENGTH_SHORT).show();
                    mCaloriesBurnt.setText("0.00");
                }

                //Radius Entered Validation
                if ((mRadiusString.equalsIgnoreCase("") || mRadiusString.equalsIgnoreCase(".")) && buttonText.equalsIgnoreCase(startText)) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.csc_radius_toast_empty), Toast.LENGTH_SHORT).show();
                }

                if ((mRadiusString.equalsIgnoreCase("0.")
                        || mRadiusString.equalsIgnoreCase("0")) && buttonText.equalsIgnoreCase(startText)) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.csc_radius_toast_less), Toast.LENGTH_SHORT).show();
                }

                if (mRadiusInt > ZERO && mRadiusInt < MINIMUM_RADIUS && buttonText.equalsIgnoreCase(startText)) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.csc_radius_toast_less), Toast.LENGTH_SHORT).show();
                }

                if ((mRadiusInt > MAXIMUM_RADIUS && buttonText.equalsIgnoreCase(startText))) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.csc_radius_toast_greater), Toast.LENGTH_SHORT).show();
                }

                if (mWeightInt <= MAX_WEIGHT) {
                    if (buttonText.equalsIgnoreCase(startText)) {
                        mBtn.setText(stopText);
                        mCaloriesBurnt.setText("0.00");
                        mWeightEdittext.setEnabled(false);
                        mRadiusEdittext.setEnabled(false);
                        getGattData();
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
                        mTimer.start();
                        mTimer.setBase(SystemClock.elapsedRealtime());
                        mTimer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                            @Override
                            public void onChronometerTick(Chronometer chronometer) {
                                showCaloriesBurnt();
                            }
                        });
                    } else {
                        mWeightEdittext.setEnabled(true);
                        mRadiusEdittext.setEnabled(true);
                        mBtn.setText(startText);
                        stopBroadcastDataNotify(mNotifyCharacteristic);
                        mTimer.stop();
                    }
                } else {
                    if (buttonText.equalsIgnoreCase(startText)) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.csc_weight_toast_greater), Toast.LENGTH_SHORT).show();
                        mBtn.setText(stopText);
                        mCaloriesBurnt.setText("0.00");
                        mWeightEdittext.setEnabled(false);
                        mRadiusEdittext.setEnabled(false);
                        getGattData();
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
                        mTimer.start();
                        mTimer.setBase(SystemClock.elapsedRealtime());
                        mTimer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                            @Override
                            public void onChronometerTick(Chronometer chronometer) {
                                showCaloriesBurnt();
                            }
                        });
                    } else {
                        mWeightEdittext.setEnabled(true);
                        mRadiusEdittext.setEnabled(true);
                        mBtn.setText(startText);
                        stopBroadcastDataNotify(mNotifyCharacteristic);
                        mCaloriesBurnt.setText("0.00");
                        mTimer.stop();
                    }
                }

            }
        });
        setHasOptionsMenu(true);
        return rootView;
    }

    /**
     * Display live cycling data
     */
    private void displayLiveData(final ArrayList<String> csc_data) {
        if (csc_data != null) {
            try {
                double distance = Double.parseDouble(csc_data.get(0));
                if (distance < 1000) { // 1 km in m
                    mDistanceRan.setText(String.format("%.0f", distance));
                    mDistanceUnit.setText(R.string.csc_distance_unit_m);
                } else {
                    mDistanceRan.setText(String.format("%.2f", distance / 1000.0f));
                    mDistanceUnit.setText(R.string.csc_distance_unit_km);
                }
                mCadence.setText(csc_data.get(1));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            if (mCurrentTime == 0) {
                mGraphLastXValue = 0;
                mCurrentTime = Utils.getTimeInSeconds();
            } else {
                mPreviosTime = mCurrentTime;
                mCurrentTime = Utils.getTimeInSeconds();
                mGraphLastXValue = mGraphLastXValue + (mCurrentTime - mPreviosTime) / 1000;
            }
            try {
                float val = Float.valueOf(csc_data.get(1));
                mDataSeries.add(mGraphLastXValue, val);
                mChart.repaint();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private float showElapsedTime() {
        final float SECOND = 1000;
        float MINUTE = 60 * SECOND;
        float elapsedMillis = SystemClock.elapsedRealtime() - mTimer.getBase();
        elapsedMillis = elapsedMillis / MINUTE;
        return elapsedMillis;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mGattUpdateReceiver,
                Utils.makeGattUpdateIntentFilter());
        Utils.setUpActionBar(getActivity(),
                getResources().getString(R.string.csc_fragment));
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mGattUpdateReceiver);
        if (mNotifyCharacteristic != null) {
            stopBroadcastDataNotify(mNotifyCharacteristic);
        }
        super.onDestroy();

    }

    /**
     * Stopping Broadcast receiver to broadcast notify characteristics
     *
     * @param gattCharacteristic
     */
    void stopBroadcastDataNotify(
            BluetoothGattCharacteristic gattCharacteristic) {
        if (gattCharacteristic != null) {
            if ((gattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                Logger.d("Stopped notification");
                BluetoothLeService.setCharacteristicNotification(
                        gattCharacteristic, false);
            }

        }

    }

    /**
     * Preparing Broadcast receiver to broadcast notify characteristics
     *
     * @param gattCharacteristic
     */
    void prepareBroadcastDataNotify(
            BluetoothGattCharacteristic gattCharacteristic) {
        if ((gattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic = gattCharacteristic;
            BluetoothLeService.setCharacteristicNotification(gattCharacteristic,
                    true);
        }

    }

    /**
     * Method to get required characteristics from service
     */
    void getGattData() {
        List<BluetoothGattCharacteristic> gattCharacteristics = mService
                .getCharacteristics();
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            String uuidchara = gattCharacteristic.getUuid().toString();
            if (uuidchara.equalsIgnoreCase(GattAttributes.CSC_MEASUREMENT)) {
                prepareBroadcastDataNotify(gattCharacteristic);
            }
        }
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
        graph.setVisible(true);
        log.setVisible(true);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        switch (item.getItemId()) {
            case R.id.graph:
                if (mGraphLayoutParent.getVisibility() != View.VISIBLE) {
                    mGraphLayoutParent.setVisibility(View.VISIBLE);

                } else {
                    mGraphLayoutParent.setVisibility(View.GONE);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Setting up the aChart Third party library
     *
     * @param parent
     */
    private void setupChart(View parent) {
        /**
         * Setting graph titles
         */
        String graphTitle = getResources().getString(R.string.csc_fragment);
        String graphXAxis = getResources().getString(R.string.health_temperature_time);
        String graphYAxis = getResources().getString(R.string.csc_cadence_graph);


        // Creating an  XYSeries for running speed
        mDataSeries = new XYSeries(graphTitle);


        // Creating a dataset to hold each series
        XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();

        // Adding temperature Series to the dataset
        mDataset.addSeries(mDataSeries);


        // Creating XYSeriesRenderer to customize
        XYSeriesRenderer mRenderer = new XYSeriesRenderer();
        mRenderer.setColor(getResources().getColor(R.color.main_bg_color));
        mRenderer.setPointStyle(PointStyle.CIRCLE);
        mRenderer.setFillPoints(true);
        mRenderer.setLineWidth(5);

        // Creating a XYMultipleSeriesRenderer to customize the whole chart
        XYMultipleSeriesRenderer mMultiRenderer = new XYMultipleSeriesRenderer();
        int deviceDPi = getResources().getDisplayMetrics().densityDpi;
        switch (getResources().getDisplayMetrics().densityDpi) {
            case DisplayMetrics.DENSITY_XHIGH:
                mMultiRenderer.setMargins(new int[]{Constants.GRAPH_MARGIN_40, Constants.GRAPH_MARGIN_90,
                        Constants.GRAPH_MARGIN_25, Constants.GRAPH_MARGIN_10});
                mMultiRenderer.setAxisTitleTextSize(Constants.TEXT_SIZE_XHDPI);
                mMultiRenderer.setChartTitleTextSize(Constants.TEXT_SIZE_XHDPI);
                mMultiRenderer.setLabelsTextSize(Constants.TEXT_SIZE_XHDPI);
                mMultiRenderer.setLegendTextSize(Constants.TEXT_SIZE_XHDPI);
                break;
            case DisplayMetrics.DENSITY_HIGH:
                mMultiRenderer.setMargins(new int[]{Constants.GRAPH_MARGIN_30, Constants.GRAPH_MARGIN_50,
                        Constants.GRAPH_MARGIN_25, Constants.GRAPH_MARGIN_10});
                mMultiRenderer.setAxisTitleTextSize(Constants.TEXT_SIZE_HDPI);
                mMultiRenderer.setChartTitleTextSize(Constants.TEXT_SIZE_HDPI);
                mMultiRenderer.setLabelsTextSize(Constants.TEXT_SIZE_HDPI);
                mMultiRenderer.setLegendTextSize(Constants.TEXT_SIZE_HDPI);
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                mMultiRenderer.setMargins(new int[]{Constants.GRAPH_MARGIN_50, Constants.GRAPH_MARGIN_100,
                        Constants.GRAPH_MARGIN_35, Constants.GRAPH_MARGIN_20});
                mMultiRenderer.setAxisTitleTextSize(Constants.TEXT_SIZE_XXHDPI);
                mMultiRenderer.setChartTitleTextSize(Constants.TEXT_SIZE_XXHDPI);
                mMultiRenderer.setLabelsTextSize(Constants.TEXT_SIZE_XXHDPI);
                mMultiRenderer.setLegendTextSize(Constants.TEXT_SIZE_XXHDPI);
                break;

            default:
                if (deviceDPi > DisplayMetrics.DENSITY_XXHIGH && deviceDPi <=
                        DisplayMetrics.DENSITY_XXXHIGH) {
                    mMultiRenderer.setMargins(new int[]{Constants.GRAPH_MARGIN_70, Constants.GRAPH_MARGIN_130,
                            Constants.GRAPH_MARGIN_35, Constants.GRAPH_MARGIN_20});
                    mMultiRenderer.setAxisTitleTextSize(Constants.TEXT_SIZE_XXXHDPI);
                    mMultiRenderer.setChartTitleTextSize(Constants.TEXT_SIZE_XXXHDPI);
                    mMultiRenderer.setLabelsTextSize(Constants.TEXT_SIZE_XXXHDPI);
                    mMultiRenderer.setLegendTextSize(Constants.TEXT_SIZE_XXXHDPI);
                } else {
                    mMultiRenderer.setMargins(new int[]{Constants.GRAPH_MARGIN_30, Constants.GRAPH_MARGIN_50,
                            Constants.GRAPH_MARGIN_25, Constants.GRAPH_MARGIN_10});
                    mMultiRenderer.setAxisTitleTextSize(Constants.TEXT_SIZE_LDPI);
                    mMultiRenderer.setChartTitleTextSize(Constants.TEXT_SIZE_LDPI);
                    mMultiRenderer.setLabelsTextSize(Constants.TEXT_SIZE_LDPI);
                    mMultiRenderer.setLegendTextSize(Constants.TEXT_SIZE_LDPI);
                }
                break;
        }
        mMultiRenderer.setXTitle(graphXAxis);
        mMultiRenderer.setLabelsColor(Color.BLACK);
        mMultiRenderer.setYTitle(graphYAxis);
        mMultiRenderer.setYAxisMin(0);
        mMultiRenderer.setXAxisMin(0);
        mMultiRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
        mMultiRenderer.setPanEnabled(true, true);
        mMultiRenderer.setZoomEnabled(false, false);
        mMultiRenderer.setGridColor(Color.LTGRAY);
        mMultiRenderer.setLabelsColor(Color.BLACK);
        mMultiRenderer.setYLabelsColor(0, Color.DKGRAY);
        mMultiRenderer.setYLabelsAlign(Paint.Align.RIGHT);
        mMultiRenderer.setXLabelsColor(Color.DKGRAY);
        mMultiRenderer.setYLabelsColor(0, Color.BLACK);
        mMultiRenderer.setXLabelsColor(Color.BLACK);
        mMultiRenderer.setApplyBackgroundColor(true);
        mMultiRenderer.setBackgroundColor(Color.WHITE);
        mMultiRenderer.setGridColor(Color.BLACK);
        mMultiRenderer.setShowGrid(true);
        mMultiRenderer.setShowLegend(false);

        // Adding mRenderer to multipleRenderer
        mMultiRenderer.addSeriesRenderer(mRenderer);

        // Getting a reference to LinearLayout of the MainActivity Layout
        mGraphLayoutParent = (LinearLayout) parent.findViewById(R.id.chart_container);


        mChart = ChartFactory.getLineChartView(getActivity(),
                mDataset, mMultiRenderer);


        // Adding the Line Chart to the LinearLayout
        mGraphLayoutParent.addView(mChart);

    }

    private void showCaloriesBurnt() {
        try {
            Number numWeight = NumberFormat.getInstance().parse(mWeightString);
            mWeightInt = numWeight.floatValue();
            double caloriesBurntInt = (((showElapsedTime()) * mWeightInt) * 8);
            caloriesBurntInt = caloriesBurntInt / 1000;
            NumberFormat formatter = NumberFormat.getNumberInstance();
            formatter.setMinimumFractionDigits(4);
            formatter.setMaximumFractionDigits(4);
            String finalBurn = formatter.format(caloriesBurntInt);
            mCaloriesBurnt.setText(finalBurn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
