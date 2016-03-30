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
package com.cypress.cysmart.CommonFragments;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.BLEServiceFragments.BatteryInformationService;
import com.cypress.cysmart.BLEServiceFragments.CapsenseService;
import com.cypress.cysmart.BLEServiceFragments.DeviceInformationService;
import com.cypress.cysmart.BLEServiceFragments.FindMeService;
import com.cypress.cysmart.BLEServiceFragments.RGBFragment;
import com.cypress.cysmart.CommonUtils.CarouselLinearLayout;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.UUIDDatabase;
import com.cypress.cysmart.GATTDBFragments.GattServicesFragment;
import com.cypress.cysmart.OTAFirmwareUpdate.OTAFirmwareUpgradeFragment;
import com.cypress.cysmart.R;
import com.cypress.cysmart.RDKEmulatorView.RemoteControlEmulatorFragment;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class CarouselFragment extends Fragment {

    public final static String EXTRA_FRAG_DEVICE_ADDRESS = "com.cypress.cysmart.fragments.CarouselFragment.EXTRA_FRAG_DEVICE_ADDRESS";
    private final static HashMap<String, BluetoothGattService> mBleHashMap = new HashMap<String, BluetoothGattService>();
    /**
     * Argument keys passed between fragments
     */
    private final static String EXTRA_FRAG_POS = "com.cypress.cysmart.fragments.CarouselFragment.EXTRA_FRAG_POS";
    private final static String EXTRA_FRAG_SCALE = "com.cypress.cysmart.fragments.CarouselFragment.EXTRA_FRAG_SCALE";
    private final static String EXTRA_FRAG_NAME = "com.cypress.cysmart.fragments.CarouselFragment.EXTRA_FRAG_NAME";
    private final static String EXTRA_FRAG_UUID = "com.cypress.cysmart.fragments.CarouselFragment.EXTRA_FRAG_UUID";
    /**
     * BluetoothGattCharacteristic Notify
     */
    public static BluetoothGattCharacteristic mNotifyCharacteristic;
    /**
     * BluetoothGattCharacteristic Read
     */
    public static BluetoothGattCharacteristic mReadCharacteristic;
    /**
     * BluetoothGattService current
     */
    private static BluetoothGattService mService;
    /**
     * Current UUID
     */
    private static UUID mCurrentUUID;
    /**
     * BluetoothGattCharacteristic List length
     */
    int mGattCharacteristicsLength = 0;
    /**
     * BluetoothGattCharacteristic current
     */
    BluetoothGattCharacteristic mCurrentCharacteristic;
    /**
     * CarouselView Image is actually a button
     */
    private Button mCarouselButton;

    /**
     * Fragment new Instance creation with arguments
     *
     * @param pos
     * @param scale
     * @param name
     * @param uuid
     * @param service
     * @return CarouselFragment
     */
    public static Fragment newInstance(int pos,
                                       float scale, String name, String uuid, BluetoothGattService service) {
        CarouselFragment mFragment = new CarouselFragment();
        if (service.getInstanceId() > 0) {
            uuid = uuid + service.getInstanceId();
        }
        mBleHashMap.put(uuid, service);
        Bundle mBundle = new Bundle();
        mBundle.putInt(EXTRA_FRAG_POS, pos);
        mBundle.putFloat(EXTRA_FRAG_SCALE, scale);
        mBundle.putString(EXTRA_FRAG_NAME, name);
        mBundle.putString(EXTRA_FRAG_UUID, uuid);
        mFragment.setArguments(mBundle);
        return mFragment;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.carousel_fragment_item, container, false);

        final int pos = this.getArguments().getInt(EXTRA_FRAG_POS);
        final String mName = this.getArguments().getString(EXTRA_FRAG_NAME);
        final String mUuid = this.getArguments().getString(EXTRA_FRAG_UUID);

        TextView mTv = (TextView) rootView.findViewById(R.id.text);
        mTv.setText(mName);

        if (mName.equalsIgnoreCase(getResources().getString(
                R.string.profile_control_unknown_service))) {
            mService = mBleHashMap.get(mUuid);
            mCurrentUUID = mService.getUuid();

            TextView mTvUUID = (TextView) rootView.findViewById(R.id.text_uuid);
            mTvUUID.setText(mCurrentUUID.toString());
        }

        mCarouselButton = (Button) rootView.findViewById(R.id.content);
        mCarouselButton.setBackgroundResource(pos);
        mCarouselButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // Getting the Mapped service from the UUID
                mService = mBleHashMap.get(mUuid);
                mCurrentUUID = mService.getUuid();


                // Device information service
                if (mService.getUuid().equals(UUIDDatabase.UUID_DEVICE_INFORMATION_SERVICE)) {
                    DeviceInformationService deviceInformationMeasurementFragment = new DeviceInformationService()
                            .create(mService);
                    displayView(deviceInformationMeasurementFragment, getResources().getString(R.string.device_info));

                }
                // Battery service
                else if (mService.getUuid().equals(UUIDDatabase.UUID_BATTERY_SERVICE)) {
                    BatteryInformationService batteryInfoFragment = new BatteryInformationService()
                            .create(mService);
                    displayView(batteryInfoFragment, getResources().getString(R.string.battery_info_fragment));
                }

                // Find Me
                else if (mService.getUuid().equals(UUIDDatabase.UUID_IMMEDIATE_ALERT_SERVICE)) {
                    FindMeService findMeService = new FindMeService().create(
                            mService,
                            ServiceDiscoveryFragment.mGattServiceFindMeData, mName);
                    displayView(findMeService, getResources().getString(R.string.findme_fragment));
                }
                // Proximity
                else if (mService.getUuid().equals(UUIDDatabase.UUID_LINK_LOSS_SERVICE)
                        || mService.getUuid().equals(UUIDDatabase.UUID_TRANSMISSION_POWER_SERVICE)) {
                    FindMeService findMeService = new FindMeService().create(
                            mService,
                            ServiceDiscoveryFragment.mGattServiceProximityData, mName);
                    displayView(findMeService, getResources().getString(R.string.proximity_fragment));
                }
                // CapSense
                else if (mService.getUuid().equals(UUIDDatabase.UUID_CAPSENSE_SERVICE)
                        || mService.getUuid().equals(UUIDDatabase.UUID_CAPSENSE_SERVICE_CUSTOM)) {
                    List<BluetoothGattCharacteristic> gattCapSenseCharacteristics = mService
                            .getCharacteristics();
                    CapsenseService capSensePager = new CapsenseService()
                            .create(mService, gattCapSenseCharacteristics.size());
                    displayView(capSensePager, getResources().getString(R.string.capsense));
                }
                // GattDB
                else if (mService.getUuid().equals(UUIDDatabase.UUID_GENERIC_ATTRIBUTE_SERVICE)
                        || mService.getUuid().equals(UUIDDatabase.UUID_GENERIC_ACCESS_SERVICE)) {
                    GattServicesFragment gattsericesfragment = new GattServicesFragment()
                            .create();
                    displayView(gattsericesfragment, getResources().getString(R.string.gatt_db));
                }
                // RGB Service
                else if (mService.getUuid().equals(UUIDDatabase.UUID_RGB_LED_SERVICE)
                        || mService.getUuid().equals(UUIDDatabase.UUID_RGB_LED_SERVICE_CUSTOM)) {
                    RGBFragment rgbfragment = new RGBFragment().create(mService);
                    displayView(rgbfragment, getResources().getString(R.string.rgb_led));
                }


                // HID(Remote Control Emulator) Service
                else if (mService.getUuid().equals(UUIDDatabase.UUID_HID_SERVICE)) {
                    String connectedDeviceName = BluetoothLeService.getmBluetoothDeviceName();
                    String remoteName = getResources().getString(R.string.rdk_emulator_view);
                    if (connectedDeviceName.indexOf(remoteName) != -1) {
                        if (Constants.RDK_ENABLED) {
                            RemoteControlEmulatorFragment remoteControlEmulatorFragment =
                                    new RemoteControlEmulatorFragment()
                                            .create(mService);
                            displayView(remoteControlEmulatorFragment, getResources().getString(R.string.rdk_emulator_view));
                        } else {
                            showWarningMessage();
                        }

                    } else {
                        showWarningMessage();
                    }

                }
                // OTA Firmware Update Service
                else if (mService.getUuid().equals(UUIDDatabase.UUID_OTA_UPDATE_SERVICE)) {
                    if (Constants.OTA_ENABLED) {
//                        OTAUpgradeTypeSelectionFragment firmwareTypeService = new OTAUpgradeTypeSelectionFragment()
//                                .create(service, false);
                        OTAFirmwareUpgradeFragment firmwareUpgradeFragment = new OTAFirmwareUpgradeFragment().
                                create(mService);
                        displayView(firmwareUpgradeFragment, getResources().getString(R.string.ota_upgrade));
                    } else {
                        showWarningMessage();
                    }

                } else {
                    showWarningMessage();
                }
            }
        });
        CarouselLinearLayout root = (CarouselLinearLayout) rootView
                .findViewById(R.id.root);
        float scale = this.getArguments().getFloat(EXTRA_FRAG_SCALE);
        root.setScaleBoth(scale);
        return rootView;

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

    void showWarningMessage() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                getActivity());
        // set title
        alertDialogBuilder
                .setTitle(R.string.alert_message_unknown_title);
        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.alert_message_unkown)
                .setCancelable(false)
                .setPositiveButton(R.string.alert_message_yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog, int id) {
                                GattServicesFragment gattsericesfragment = new GattServicesFragment()
                                        .create();
                                displayView(gattsericesfragment, getResources().getString(R.string.gatt_db));
                            }
                        })
                .setNegativeButton(R.string.alert_message_no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

}
