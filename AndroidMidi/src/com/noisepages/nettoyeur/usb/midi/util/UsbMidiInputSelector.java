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

public abstract class UsbMidiInputSelector extends DialogFragment {

	private final UsbMidiDevice device;
	
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
		return new AlertDialog.Builder(getActivity())
		    .setTitle(R.string.title_select_usb_midi_input)
		    .setItems(items.toArray(new String[items.size()]), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					int iface = 0;
					int index = which;
					while (index >= device.getInterfaces().get(iface).getInputs().size()) {
						index -= device.getInterfaces().get(iface).getInputs().size();
						++iface;
					}
					onInputSelected(device.getInterfaces().get(iface).getInputs().get(index), iface, index);
				}

			})
			.setCancelable(true)
		    .create();
	}
	
	abstract protected void onInputSelected(UsbMidiInput input, int iface, int index);
}
