/*
 * Copyright (C) 2013 Peter Brinkmann (peter.brinkmann@gmail.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.noisepages.nettoyeur.bluetooth.util;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;

import com.noisepages.nettoyeur.bluetooth.BluetoothDisabledException;
import com.noisepages.nettoyeur.bluetooth.BluetoothUnavailableException;
import com.noisepages.nettoyeur.midi.R;

/**
 * Simple dialog for selecting a device from the list of paired Bluetooth devices; much like
 * {@link DeviceListActivity} but easier to use. If you are aiming for compatibility with
 * pre-Honeycomb devices, however, you still need to use DeviceListActivity.
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public abstract class BluetoothDeviceSelector extends DialogFragment {

  private final List<BluetoothDevice> devices;
  private final String[] deviceLabels;

  /**
   * Constructor.
   * 
   * @throws BluetoothUnavailableException if the hardware does not support Bluetooth
   * @throws BluetoothDisabledException if Bluetooth is disabled
   */
  public BluetoothDeviceSelector() throws BluetoothUnavailableException, BluetoothDisabledException {
    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    if (btAdapter == null) {
      throw new BluetoothUnavailableException();
    }
    if (!btAdapter.isEnabled()) {
      throw new BluetoothDisabledException();
    }
    devices = new ArrayList<BluetoothDevice>(btAdapter.getBondedDevices());
    deviceLabels = new String[devices.size()];
    for (int i = 0; i < deviceLabels.length; ++i) {
      deviceLabels[i] = devices.get(i).getName() + "\n" + devices.get(i).getAddress();
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setCancelable(true);
    if (!devices.isEmpty()) {
      builder.setTitle(R.string.title_select_bluetooth_device).setItems(deviceLabels,
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              onDeviceSelected(devices.get(which));
            }

          });
    } else {
      builder.setTitle(R.string.title_no_paired_devices).setPositiveButton(android.R.string.ok,
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              onNoSelection();
            }
          });
    }
    return builder.create();
  }


  @Override
  public void onCancel(android.content.DialogInterface dialog) {
    onNoSelection();
  }

  /**
   * Callback for handling device selections.
   * 
   * @param device the selected device
   */
  protected abstract void onDeviceSelected(BluetoothDevice device);

  /**
   * Callback for handling cancellation.
   */
  protected abstract void onNoSelection();
}
