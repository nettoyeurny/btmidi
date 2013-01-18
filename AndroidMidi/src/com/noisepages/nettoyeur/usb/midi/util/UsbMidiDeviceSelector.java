package com.noisepages.nettoyeur.usb.midi.util;

import java.util.List;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import com.noisepages.nettoyeur.midi.R;
import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice;

public abstract class UsbMidiDeviceSelector extends DialogFragment {

	private final List<UsbMidiDevice> devices;
	
	public UsbMidiDeviceSelector(List<UsbMidiDevice> devices) {
		this.devices = devices;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String items[] = new String[devices.size()];
		for (int i = 0; i < devices.size(); ++i) {
			items[i] = devices.get(i).getCurrentDeviceInfo().toString();
		}
		return new Builder(getActivity())
			.setTitle(R.string.title_select_usb_midi_device)
			.setItems(items, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					onDeviceSelected(devices.get(which));
				}
			})
			.setCancelable(true)
			.create();
	}
	
	abstract protected void onDeviceSelected(UsbMidiDevice device);
}