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

package com.noisepages.nettoyeur.usb.midi.util;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.noisepages.nettoyeur.midi.R;
import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice;
import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice.UsbMidiInput;

/**
 * Simple dialog for selecting a MIDI input of a given USB MIDI device.
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 */
public abstract class UsbMidiInputSelector extends DialogFragment {

  private final UsbMidiDevice device;

  /**
   * Constructor.
   * 
   * @param device from which to select an input.
   */
  public UsbMidiInputSelector(UsbMidiDevice device) {
    this.device = device;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    List<String> items = new ArrayList<String>();
    for (int i = 0; i < device.getInterfaces().size(); ++i) {
      for (int j = 0; j < device.getInterfaces().get(i).getInputs().size(); ++j) {
        items.add("Interface " + i + ", Input " + j);
      }
    }
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setCancelable(true);
    if (!items.isEmpty()) {
      builder.setTitle(R.string.title_select_usb_midi_input).setItems(
          items.toArray(new String[items.size()]), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              int iface = 0;
              int index = which;
              while (index >= device.getInterfaces().get(iface).getInputs().size()) {
                index -= device.getInterfaces().get(iface).getInputs().size();
                ++iface;
              }
              onInputSelected(device.getInterfaces().get(iface).getInputs().get(index), device,
                  iface, index);
            }

          });
    } else {
      builder.setTitle(R.string.title_no_usb_midi_input_available).setPositiveButton(
          android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              onNoSelection(device);
            }
          });
    }
    return builder.create();
  }


  @Override
  public void onCancel(android.content.DialogInterface dialog) {
    onNoSelection(device);
  }

  /**
   * Handle selected MIDI inputs in this method.
   * 
   * @param input selection
   * @param device that the selection belongs to
   * @param iface index of the interface that the selected input belongs to
   * @param index index of the selected input within its interface
   */
  protected abstract void onInputSelected(UsbMidiInput input, UsbMidiDevice device, int iface,
      int index);

  /**
   * Handle cancellation as well as devices without inputs.
   * 
   * @param device that this dialog belongs to
   */
  protected abstract void onNoSelection(UsbMidiDevice device);
}
