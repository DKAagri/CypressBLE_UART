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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.BLEProfileDataParserClasses.GlucoseParser;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.CustomSpinner;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.UUIDDatabase;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.DataModelClasses.GlucoseRecord;
import com.cypress.cysmart.ListAdapters.GlucoseSpinnerAdapter;
import com.cypress.cysmart.R;

import java.util.List;
import java.util.UUID;

/**
 * Fragment to show the glucose service
 */
public class GlucoseService extends Fragment implements View.OnClickListener {

    // GATT Service and characteristics
    private static BluetoothGattService mService;
    private static BluetoothGattCharacteristic mNotifyCharacteristic;

    //ProgressDialog
    private ProgressDialog mProgressDialog;

    // Data view variables
    private TextView mGlucoseConcentration;
    private TextView mGlucoseRecordTime;
    private TextView mGlucoseType;
    private TextView mGlucoseSampleLocation;
    private TextView mGlucoseConcentrationUnit;
    private TextView mNoRecord;
    private Button mReadLastRecord;
    private Button mReadAllRecord;
    private Button mDeleteAll;
    private Button mClear;
    private CustomSpinner mGlucoseSpinner;
    private ImageButton mAdditionalInfo;
    private GlucoseSpinnerAdapter mAdapter, mNewAdapter;

    private GlucoseParser mGlucoseParser = new GlucoseParser();
    Bundle bundle = new Bundle();

    private static BluetoothGattCharacteristic mGlucoseMeasurementCharacteristic;
    private static BluetoothGattCharacteristic mGlucoseMeasurementContextCharacteristic;
    private static BluetoothGattCharacteristic mRecordAccessControlPointCharacteristic;

    public static SparseArray<GlucoseRecord> mReceivedGlucoseMeasurementData = new SparseArray<GlucoseRecord>();
    /**
     * BroadcastReceiver for receiving the GATT server status
     */
    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();

            // Services Discovered from GATT Server
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (extras.containsKey(Constants.EXTRA_GLUCOSE_MEASUREMENT)) {

                    if(GlucoseParser.mMeasurementRecords.size() == 0){
                        mNoRecord.setVisibility(View.VISIBLE);
                    } else {
                        mNoRecord.setVisibility(View.INVISIBLE);
                    }
                    for (int i = 0; i < GlucoseParser.mMeasurementRecords.size(); i++) {
                        mReceivedGlucoseMeasurementData.put(GlucoseParser.mMeasurementRecords.keyAt(i), GlucoseParser.mMeasurementRecords.valueAt(i));
                    }
                    try {
                        mAdapter.notifyDataSetChanged();
                        mGlucoseSpinner.setSelection(mReceivedGlucoseMeasurementData.size());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            //Bond state receiver
            else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                if (state == BluetoothDevice.BOND_BONDING) {
                    // Bonding...
                    Logger.i("Bonding is in process....");
                } else if (state == BluetoothDevice.BOND_BONDED) {
                    String dataLog = getResources().getString(R.string.dl_commaseparator)
                            + "[" + BluetoothLeService.getmBluetoothDeviceName() + "|"
                            + BluetoothLeService.getmBluetoothDeviceAddress() + "]" +
                            getResources().getString(R.string.dl_commaseparator) +
                            getResources().getString(R.string.dl_connection_paired);
                    Logger.datalog(dataLog);
                    getAllCharacteristic();
                } else if (state == BluetoothDevice.BOND_NONE) {
                    String dataLog = getResources().getString(R.string.dl_commaseparator)
                            + "[" + BluetoothLeService.getmBluetoothDeviceName() + "|"
                            + BluetoothLeService.getmBluetoothDeviceAddress() + "]" +
                            getResources().getString(R.string.dl_commaseparator) +
                            getResources().getString(R.string.dl_connection_unpaired);
                    Logger.datalog(dataLog);
                }
            } else if (action.equals(BluetoothLeService.ACTION_WRITE_COMPLETED)) {
                if (mProgressDialog != null && mProgressDialog.isShowing())
                    mProgressDialog.dismiss();
            }

        }
    };

    public GlucoseService create(BluetoothGattService service) {
        GlucoseService fragment = new GlucoseService();
        mService = service;
        if (service != null) {
            mGlucoseMeasurementCharacteristic = mService.getCharacteristic(UUIDDatabase.UUID_GLUCOSE_MEASUREMENT);
            mGlucoseMeasurementContextCharacteristic = mService.getCharacteristic(UUIDDatabase.UUID_GLUCOSE_MEASUREMENT_CONTEXT);
            mRecordAccessControlPointCharacteristic = mService.getCharacteristic(UUIDDatabase.UUID_RECORD_ACCESS_CONTROL_POINT);
        }
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.glucose_measurement,
                container, false);
        mGlucoseConcentration = (TextView) rootView
                .findViewById(R.id.glucose_measure);
        mGlucoseRecordTime = (TextView) rootView
                .findViewById(R.id.recording_time_data);
        mGlucoseType = (TextView) rootView.findViewById(R.id.glucose_type);
        mGlucoseSampleLocation = (TextView) rootView
                .findViewById(R.id.glucose_sample_location);
        mGlucoseConcentrationUnit = (TextView) rootView
                .findViewById(R.id.glucose_unit);
        mNoRecord = (TextView) rootView
                .findViewById(R.id.no_record);
        mNoRecord.setVisibility(View.VISIBLE);
        mReadLastRecord = (Button) rootView
                .findViewById(R.id.glucose_read_last);
        mReadLastRecord.setOnClickListener(this);
        mReadAllRecord = (Button) rootView
                .findViewById(R.id.glucose_read_all);
        mReadAllRecord.setOnClickListener(this);
        mDeleteAll = (Button) rootView
                .findViewById(R.id.glucose_delete_all);
        mDeleteAll.setOnClickListener(this);
        mClear = (Button) rootView
                .findViewById(R.id.glucose_clear);
        mClear.setOnClickListener(this);
        mAdditionalInfo = (ImageButton) rootView
                .findViewById(R.id.add_info_icon);
        mAdditionalInfo.setOnClickListener(this);
        mProgressDialog = new ProgressDialog(getActivity());
        mGlucoseSpinner = (CustomSpinner) rootView.findViewById(R.id.record_spinner);
        //mGlucoseSpinner.setVisibility(View.INVISIBLE);
        mAdapter = new GlucoseSpinnerAdapter(getActivity(), mReceivedGlucoseMeasurementData);
        mGlucoseSpinner.setAdapter(mAdapter);
        mGlucoseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                GlucoseRecord record = mReceivedGlucoseMeasurementData.valueAt(position);
                mGlucoseConcentration.setText(String.format("%f", record.glucoseConcentration));
                mGlucoseRecordTime.setText(String.valueOf(record.time));
                mGlucoseType.setText(String.valueOf(record.type));
                mGlucoseSampleLocation.setText(String.valueOf(record.sampleLocation));
                mGlucoseConcentrationUnit.setText(record.unit == GlucoseRecord.UNIT_kgpl ? R.string.glucose_concentration_kgl : R.string.glucose_concentration_mole);

                if (record.context) {
                    mAdditionalInfo.setVisibility(View.VISIBLE);
                    bundle.putString(Constants.GLS_CARB_ID, record.carbohydrateId);
                    bundle.putString(Constants.GLS_CARB_UNITS, record.carbohydrateUnits);
                    bundle.putString(Constants.GLS_EXERCISE_DURATION, record.exerciseDuration);
                    bundle.putString(Constants.GLS_EXERCISE_INTENSITY, record.exerciseIntensity);
                    bundle.putString(Constants.GLS_MEAL, record.meal);
                    bundle.putString(Constants.GLS_HEALTH, record.health);
                    bundle.putString(Constants.GLS_TESTER, record.tester);
                    bundle.putString(Constants.GLS_MEDICATION_ID, record.medicationId);
                    bundle.putFloat(Constants.GLS_MEDICATION_QUANTITY, record.medicationQuantity);
                    bundle.putString(Constants.GLS_MEDICATION_UNIT, record.medicationUnit);
                    bundle.putString(Constants.GLS_HBA1C, record.HbA1c);
                } else {
                    mAdditionalInfo.setVisibility(View.INVISIBLE);
                    //Toast.makeText(getActivity(),"No context Information", Toast.LENGTH_SHORT).show();;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {
            }
        });

        setHasOptionsMenu(true);
        return rootView;
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.glucose_read_last:
                mGlucoseParser.getLastRecord(mRecordAccessControlPointCharacteristic);
                if(mReceivedGlucoseMeasurementData.size() != 0) {
                    mNoRecord.setVisibility(View.INVISIBLE);
                }
                break;
            case R.id.glucose_read_all:
                mGlucoseParser.getAllRecords(mRecordAccessControlPointCharacteristic);
                if(mReceivedGlucoseMeasurementData.size() != 0) {
                    mNoRecord.setVisibility(View.INVISIBLE);
                }
                break;
            case R.id.glucose_delete_all:
                clearUI();
                mGlucoseParser.deleteAllRecords(mRecordAccessControlPointCharacteristic);
                break;
            case R.id.glucose_clear:
                clearUI();
                break;
            case R.id.add_info_icon:
                GlucoseAdditionalInfo glucoseInfoFragment = new GlucoseAdditionalInfo();
                glucoseInfoFragment.setArguments(bundle);
                displayView(glucoseInfoFragment, Constants.GLUCOSE_ADDITIONAL_FRAGMENT_TAG);
                break;
            default:
                break;
        }
    }

    public void clearUI() {
        mNoRecord.setVisibility(View.VISIBLE);
        mGlucoseParser.clear();
        mReceivedGlucoseMeasurementData.clear();
        mAdapter.notifyDataSetInvalidated();
        mAdditionalInfo.setVisibility(View.INVISIBLE);
        mGlucoseConcentration.setText("");
        mGlucoseConcentrationUnit.setText("");
        mGlucoseRecordTime.setText("");
        mGlucoseType.setText("");
        mGlucoseSampleLocation.setText("");
    }

    public void updateUI() {
        mGlucoseParser.clear();
        mAdapter.notifyDataSetInvalidated();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (BluetoothLeService.getBondedState()) {
            getAllCharacteristic();
        } else {
            BluetoothLeService.bondDevice();
        }

        getActivity().registerReceiver(mGattUpdateReceiver,
                Utils.makeGattUpdateIntentFilter());
        /**
         * Initializes ActionBar as required
         */
        Utils.setUpActionBar(getActivity(),
                getResources().getString(R.string.glucose_fragment));

    }

    @Override
    public void onDestroy() {
        stopBroadcastAllNotifications();
        getActivity().unregisterReceiver(mGattUpdateReceiver);
        clearUI();
        super.onDestroy();
    }

    private void stopBroadcastAllNotifications() {
        List<BluetoothGattCharacteristic> gattCharacteristics = mService
                .getCharacteristics();
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            String uuidchara = gattCharacteristic.getUuid().toString();
            final int charaProp = gattCharacteristic.getProperties();
            if (uuidchara.equalsIgnoreCase(GattAttributes.GLUCOSE_MEASUREMENT)) {
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    BluetoothLeService.setCharacteristicNotification(gattCharacteristic,
                            false);
                }
            }
            if (uuidchara.equalsIgnoreCase(GattAttributes.GLUCOSE_MEASUREMENT_CONTEXT)) {
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    BluetoothLeService.setCharacteristicNotification(gattCharacteristic,
                            false);
                }
            }
            if (uuidchara.equalsIgnoreCase(GattAttributes.RECORD_ACCESS_CONTROL_POINT)) {
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                    BluetoothLeService.setCharacteristicIndication(gattCharacteristic,
                            false);
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Method to get all Characteristic with report reference
     */
    private void getAllCharacteristic() {
        List<BluetoothGattCharacteristic> gattCharacteristics = mService
                .getCharacteristics();
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            UUID uuidchara = gattCharacteristic.getUuid();
            if (uuidchara.equals(UUIDDatabase.UUID_RECORD_ACCESS_CONTROL_POINT)
                    || uuidchara.equals(UUIDDatabase.UUID_GLUCOSE_MEASUREMENT)
                    || uuidchara.equals(UUIDDatabase.UUID_GLUCOSE_MEASUREMENT_CONTEXT)) {
                BluetoothLeService.mGlucoseCharacteristics.add(gattCharacteristic);
            }
        }
        BluetoothLeService.enableAllGlucoseCharacteristics();
        showAlertMessage();
    }

    private void showAlertMessage() {
        mProgressDialog.setTitle(getResources().getString(
                R.string.alert_message_prepare_title));
        mProgressDialog.setMessage(getResources().getString(
                R.string.alert_message_prepare_message)
                + "\n"
                + getResources().getString(R.string.alert_message_wait));
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
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


    /**
     * Used for replacing the main content of the view with provided fragments
     *
     * @param fragment
     */
    void displayView(Fragment fragment, String tagName) {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().add(R.id.container, fragment, tagName)
                .addToBackStack(null).commit();
    }
}
