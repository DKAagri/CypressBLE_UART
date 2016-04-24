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

package com.cypress.cysmart.BLEProfileDataParserClasses;

import android.bluetooth.BluetoothGattCharacteristic;

import com.cypress.cysmart.BLEServiceFragments.CSCService;
import com.cypress.cysmart.CommonUtils.Logger;

import java.util.ArrayList;

/**
 * Class used for parsing Cycling speed and cadence related information
 * CRANK_REVOLUTION_DATA_PRESENT
 * Wheel_REVOLUTION_DATA_PRESENT
 */
public class CSCParser {

    private static ArrayList<String> mCSCInfo = new ArrayList<String>();
    private static String mCyclingExtraDistance;
    private static String mCyclingExtraSpeed;
    private static String mCyclingDistance;
    private static String mCyclingCadence;
    private static String mGearRatio;
    private static int mFirstWheelRevolutions = -1;
    private static int mLastWheelRevolutions = -1;
    private static int mLastWheelEventTime = -1;
    private static float mWheelCadence = -1F;
    private static int mLastCrankRevolutions = -1;
    private static int mLastCrankEventTime = -1;
    private static final int WHEEL_CONST = 65535;

    private static final int FORMAT_TYPE_20 = 20;
    private static final int FORMAT_TYPE_18 = 18;

    private static final float FLOAT_CONST_1024 = 1024F;
    private static final float FLOAT_CONST_1000 = 1000F;
    private static final float FLOAT_CONST_1000000 = 1000000F;
    private static final float FLOAT_CONST_60 = 60F;

    private static final int ARRAYLIST_INDEX_0 = 0;
    private static final int ARRAYLIST_INDEX_1 = 1;
    private static final int ARRAYLIST_INDEX_2 = 2;
    private static final int ARRAYLIST_INDEX_3 = 3;
    private static final int ARRAYLIST_INDEX_4 = 4;

    private static final byte WHEEL_REVOLUTIONS_DATA_PRESENT = 0x01; // 1 bit
    private static final byte CRANK_REVOLUTION_DATA_PRESENT = 0x02; // 1 bit

    /**
     * Get the Running Speed and Cadence
     *
     * @param characteristic
     * @return ArrayList<String>
     */
    public static ArrayList<String> getCyclingSpeednCadence(
            BluetoothGattCharacteristic characteristic) {
        // Decode the new data
        int offset = 0;
        final int flags = characteristic.getValue()[offset]; // 1 byte
        offset += 1;

        final boolean wheelRevPresent = (flags & WHEEL_REVOLUTIONS_DATA_PRESENT) > 0;
        final boolean crankRevPreset = (flags & CRANK_REVOLUTION_DATA_PRESENT) > 0;

        if (wheelRevPresent) {
            int wheelRevolutions = characteristic.getIntValue(FORMAT_TYPE_20, offset).intValue();
            int offsetComp = offset + 4;
            int lastWheelEventTime = characteristic.getIntValue(FORMAT_TYPE_18, offsetComp).intValue();
            offset = offsetComp + 2;
            onWheelMeasurementReceived(wheelRevolutions, lastWheelEventTime);
        }
        if (crankRevPreset) {
            int crankParam1 = characteristic.getIntValue(FORMAT_TYPE_18, offset).intValue();
            int crankParamComp = offset + 2;
            int crankParam2 = characteristic.getIntValue(FORMAT_TYPE_18, crankParamComp).intValue();
            onCrankMeasurementReceived(crankParam1, crankParam2);
        }
        return mCSCInfo;
    }

    private static void onWheelMeasurementReceived(final int wheelRevolutions, final int lastWheelEventTime) {
        double WHEEL_CIRCUMFERENCE =  (2 * 3.14 * CSCService.mRadiusInt);

        if (mFirstWheelRevolutions < 0) {
            mFirstWheelRevolutions = wheelRevolutions;
        }
        if (mLastWheelEventTime == lastWheelEventTime) {
            return;
        }
        if (mLastWheelRevolutions >= 0) {
            float timeDifference = 0;
            if (lastWheelEventTime < mLastWheelEventTime) {
                timeDifference = (float) ((WHEEL_CONST + lastWheelEventTime) - mLastWheelEventTime) / FLOAT_CONST_1024;
            } else {
                timeDifference = (float) (lastWheelEventTime - mLastWheelEventTime) / FLOAT_CONST_1024;
            }
            final float distanceDifference = (float) (WHEEL_CIRCUMFERENCE * (wheelRevolutions - mLastWheelRevolutions)) / FLOAT_CONST_1000;
            final float totalDistance = ((float) wheelRevolutions * (float) WHEEL_CIRCUMFERENCE) / FLOAT_CONST_1000;
            final float distance = (float) (wheelRevolutions - mFirstWheelRevolutions) * (float) WHEEL_CIRCUMFERENCE / FLOAT_CONST_1000;
            final float speed = distanceDifference / timeDifference;
            mWheelCadence = (FLOAT_CONST_60 * (float) (wheelRevolutions - mLastWheelRevolutions)) / timeDifference;
            mCyclingDistance = "" + totalDistance;
            mCSCInfo.add(ARRAYLIST_INDEX_0, mCyclingDistance);
            mCyclingExtraSpeed = "" + speed;
            mCSCInfo.add(ARRAYLIST_INDEX_3, mCyclingExtraSpeed);
            mCyclingExtraDistance = "" + distance;
            mCSCInfo.add(ARRAYLIST_INDEX_4, mCyclingExtraDistance);
            Logger.d("WheelValues are " + mCyclingDistance + " " + mCyclingExtraSpeed + " " + mCyclingExtraDistance);
        }
        mLastWheelRevolutions = wheelRevolutions;
        mLastWheelEventTime = lastWheelEventTime;
    }

    private static void onCrankMeasurementReceived(int crankRevolutions, int lastCrankEventTime) {
        if (mLastCrankEventTime == lastCrankEventTime) {
            return;
        }
        if (mLastCrankRevolutions >= 0) {
            float timeDifference = 0;
            if (lastCrankEventTime < mLastCrankEventTime) {
                timeDifference = (float) ((WHEEL_CONST + lastCrankEventTime) - mLastCrankEventTime) / FLOAT_CONST_1024;
            } else {
                timeDifference = (float) (lastCrankEventTime - mLastCrankEventTime) / FLOAT_CONST_1024;
            }
            final float crankCadence = (FLOAT_CONST_60 * (float) (crankRevolutions - mLastCrankRevolutions)) / timeDifference;
            if (crankCadence > 0.0F) {
                float GEAR_RATIO = mWheelCadence / crankCadence;
                mGearRatio = "" + GEAR_RATIO;
                mCSCInfo.add(ARRAYLIST_INDEX_2, mGearRatio);
                mCyclingCadence = "" + (int) crankCadence;
                mCSCInfo.add(ARRAYLIST_INDEX_1, mCyclingCadence);
                Logger.d("Crank Values are " + mGearRatio + " " + mCyclingCadence);
            }
        }
        mLastCrankRevolutions = crankRevolutions;
        mLastCrankEventTime = lastCrankEventTime;
    }

}
