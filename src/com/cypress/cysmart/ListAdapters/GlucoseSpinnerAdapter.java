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

package com.cypress.cysmart.ListAdapters;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.cypress.cysmart.DataModelClasses.GlucoseRecord;
import com.cypress.cysmart.R;

/**
 * Adapter class for Glucose Spinner
 *
 */
public class GlucoseSpinnerAdapter extends BaseAdapter {

    // Your sent context
    private Context mContext;
    // Your custom values for the spinner (User)
    private SparseArray<GlucoseRecord> mRecords;

    private final LayoutInflater mInflater;

    public GlucoseSpinnerAdapter(Context context, SparseArray<GlucoseRecord> records) {
        this.mRecords = records;
        mInflater = LayoutInflater.from(context);
        mContext = context;
    }

    public int getCount() {
        return mRecords.size();
    }

    public GlucoseRecord getItem(int position) {
        return mRecords.valueAt(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = mInflater.inflate(R.layout.listitem_glucose_spinner, parent, false);

            final ViewHolder holder = new ViewHolder();
            holder.recordSequenceNo = (TextView) view.findViewById(R.id.record_sequence_no);
            holder.recordTime = (TextView) view.findViewById(R.id.record_time);
            holder.recordTimeOffset = (TextView) view.findViewById(R.id.record_time_offset);
            view.setTag(holder);
        }
        final GlucoseRecord record = (GlucoseRecord) getItem(position);
        if (record == null)
            return view; // this may happen during closing the activity
        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.recordSequenceNo.setText(String.valueOf(record.sequenceNumber));
        holder.recordTime.setText(String.valueOf(record.time));
        holder.recordTimeOffset.setText(String.valueOf(record.timeOffset) + "mins");
        return view;
    }


    private class ViewHolder {
        private TextView recordSequenceNo;
        private TextView recordTime;
        private TextView recordTimeOffset;
    }

}