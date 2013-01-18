package com.noisepages.nettoyeur.usb.midi.util;

import java.util.List;

import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice;

import android.os.AsyncTask;

public abstract class AsyncDeviceInfoLookup extends AsyncTask<UsbMidiDevice, Void, Void> {

	@Override
	protected Void doInBackground(UsbMidiDevice... params) {
		for (UsbMidiDevice device : params) {
			device.retrieveReadableDeviceInfo();
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		onLookupComplete();
	}

	public void retrieveDeviceInfo(List<UsbMidiDevice> devices) {
		execute(devices.toArray(new UsbMidiDevice[devices.size()]));
	}
	
	abstract protected void onLookupComplete();
}
