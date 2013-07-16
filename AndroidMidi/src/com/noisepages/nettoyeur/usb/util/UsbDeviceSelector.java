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

package com.noisepages.nettoyeur.usb.util;

import java.util.List;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import com.noisepages.nettoyeur.midi.R;
import com.noisepages.nettoyeur.usb.UsbDeviceWithInfo;

public abstract class UsbDeviceSelector<T extends UsbDeviceWithInfo> extends DialogFragment {

  private final List<T> devices;

  public UsbDeviceSelector(List<T> devices) {
    this.devices = devices;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Builder builder = new Builder(getActivity()).setCancelable(true);
    if (!devices.isEmpty()) {
      String items[] = new String[devices.size()];
      for (int i = 0; i < devices.size(); ++i) {
        items[i] = devices.get(i).getCurrentDeviceInfo().toString();
      }
      builder.setTitle(R.string.title_select_usb_midi_device).setItems(items,
          new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              onDeviceSelected(devices.get(which));
            }
          });
    } else {
      builder.setTitle(R.string.title_no_usb_devices_available).setPositiveButton(
          android.R.string.ok, new OnClickListener() {
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
   * Handle the selected device in this method.
   * 
   * @param device selection
   */
  protected abstract void onDeviceSelected(T device);

  /**
   * Handle cancellation as well as empty device lists.
   */
  protected abstract void onNoSelection();
}
