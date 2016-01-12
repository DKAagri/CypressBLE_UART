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
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;

/**
 * Fragment to display the Device information service
 */
public class GlucoseAdditionalInfo extends Fragment {

    // GATT Service and Characteristics
    private static BluetoothGattService mService;
    private static BluetoothGattCharacteristic mReadCharacteristic;
    //ProgressDialog
    private ProgressDialog mProgressDialog;

    private TextView mCarbohydrateId;
    private TextView mCarbohydrateUnit;
    private TextView mMeal;
    private TextView mTester;
    private TextView mHealth;
    private TextView mExerciseDuration;
    private TextView mExerciseIntensity;
    private TextView mMedicationId;
    private TextView mMedication;
    private TextView mHba1c;


    public GlucoseAdditionalInfo create(BluetoothGattService service) {
        GlucoseAdditionalInfo fragment = new GlucoseAdditionalInfo();
        mService = service;
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.glucose_additional_info, container, false);
        mCarbohydrateId = (TextView) rootView.findViewById(R.id.gls_carb_id);
        mCarbohydrateUnit = (TextView) rootView.findViewById(R.id.gls_carb_unit);
        mMeal = (TextView) rootView.findViewById(R.id.gls_meal);
        mTester = (TextView) rootView.findViewById(R.id.gls_tester);
        mHealth = (TextView) rootView.findViewById(R.id.gls_health);
        mExerciseDuration = (TextView) rootView.findViewById(R.id.gls_exercise_duration);
        mExerciseIntensity = (TextView) rootView.findViewById(R.id.gls_exercise_intensity);
        mMedicationId = (TextView) rootView.findViewById(R.id.gls_medication_id);
        mMedication = (TextView) rootView.findViewById(R.id.gls_medication_unit);
        mHba1c = (TextView) rootView.findViewById(R.id.gls_hba1c);
        getDataBundle(getArguments());
        setHasOptionsMenu(true);
        return rootView;
    }

    private void getDataBundle(Bundle extras) {
        mCarbohydrateId.setText(extras.getString(Constants.GLS_CARB_ID));
        mCarbohydrateUnit.setText(extras.getString(Constants.GLS_CARB_UNITS));
        mMeal.setText(extras.getString(Constants.GLS_MEAL));
        mTester.setText(extras.getString(Constants.GLS_TESTER));
        mHealth.setText(extras.getString(Constants.GLS_HEALTH));
        mExerciseDuration.setText(extras.getString(Constants.GLS_EXERCISE_DURATION));
        mExerciseIntensity.setText(extras.getString(Constants.GLS_EXERCISE_INTENSITY));
        mMedicationId.setText(extras.getString(Constants.GLS_MEDICATION_ID));
        mMedication.setText(String.format("%f", extras.getFloat(Constants.GLS_MEDICATION_QUANTITY)) + extras.getString(Constants.GLS_MEDICATION_UNIT));
        mHba1c.setText(extras.getString(Constants.GLS_HBA1C));
    }

    /**
     * Prepare Broadcast receiver to broadcast read characteristics
     *
     * @param gattCharacteristic
     */

    void prepareBroadcastDataRead(
            BluetoothGattCharacteristic gattCharacteristic) {
        if ((gattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            BluetoothLeService.readCharacteristic(gattCharacteristic);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
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

}
