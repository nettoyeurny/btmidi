/**
 * For information on usage and redistribution, and for a DISCLAIMER OF ALL
 * WARRANTIES, see the file, "LICENSE.txt," in this distribution.
 */

package com.noisepages.nettoyeur.usbmiditest;

import java.util.List;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.noisepages.nettoyeur.midi.MidiReceiver;
import com.noisepages.nettoyeur.usb.DeviceNotConnectedException;
import com.noisepages.nettoyeur.usb.UsbBroadcastHandler;
import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice;
import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice.UsbMidiInput;
import com.noisepages.nettoyeur.usb.midi.util.UsbMidiInputSelector;
import com.noisepages.nettoyeur.usb.util.AsyncDeviceInfoLookup;
import com.noisepages.nettoyeur.usb.util.UsbDeviceSelector;

public class UsbMidiTest extends Activity {

	private TextView mainText;
	private UsbMidiDevice midiDevice = null;
	private Handler handler;

	private final MidiReceiver midiReceiver = new MidiReceiver() {
		@Override
		public void onRawByte(byte value) {
			update("raw byte: " + value);
		}

		@Override
		public void onProgramChange(int channel, int program) {
			update("program change: " + channel + ", " + program);
		}

		@Override
		public void onPolyAftertouch(int channel, int key, int velocity) {
			update("poly aftertouch: " + channel + ", " + key + ", " + velocity);
		}

		@Override
		public void onPitchBend(int channel, int value) {
			update("pitch bend: " + channel + ", " + value);
		}

		@Override
		public void onNoteOn(int channel, int key, int velocity) {
			update("note on: " + channel + ", " + key + ", " + velocity);
		}

		@Override
		public void onNoteOff(int channel, int key, int velocity) {
			update("note off: " + channel + ", " + key + ", " + velocity);
		}

		@Override
		public void onControlChange(int channel, int controller, int value) {
			update("control change: " + channel + ", " + controller + ", " + value);
		}

		@Override
		public void onAftertouch(final int channel, final int velocity) {
			update("aftertouch: " + channel + ", " + velocity);
		}
	};

	private void update(final String n) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				mainText.setText(mainText.getText() + "\n\n" + n);
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handler = new Handler();
		setContentView(R.layout.activity_main);
		mainText = (TextView) findViewById(R.id.mainText);
		mainText.setMovementMethod(new ScrollingMovementMethod());
		UsbMidiDevice.installBroadcastHandler(this, new UsbBroadcastHandler() {
			
			@Override
			public void onPermissionGranted(UsbDevice device) {
				if (midiDevice == null || !midiDevice.matches(device)) return;
				midiDevice.open(UsbMidiTest.this);
				new UsbMidiInputSelector(midiDevice) {

					@Override
					protected void onInputSelected(UsbMidiInput input, UsbMidiDevice device, int iface, int index) {
						update("\n\nInput: Interface " + iface + ", Index " + index);
						input.setReceiver(midiReceiver);
						try {
							input.start();
						} catch (DeviceNotConnectedException e) {
							mainText.setText("MIDI device has been disconnected.");
						}
					}

					@Override
					protected void onNoSelection(UsbMidiDevice device) {
						update("\n\nNo inputs available.");
					}
				}.show(getFragmentManager(), null);
			}

			@Override
			public void onPermissionDenied(UsbDevice device) {
				if (midiDevice == null || !midiDevice.matches(device)) return;
				mainText.setText("Permission denied for device " + midiDevice.getCurrentDeviceInfo() + ".");
				midiDevice = null;
			}

			@Override
			public void onDeviceDetached(UsbDevice device) {
				if (midiDevice == null || !midiDevice.matches(device)) return;
				midiDevice.close();
				midiDevice = null;
				mainText.setText("USB MIDI device detached.");
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (midiDevice != null) {
			midiDevice.close();
		}
		UsbMidiDevice.uninstallPermissionHandler(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.connect_item:
			if (midiDevice == null) {
				chooseMidiDevice();
			} else {
				midiDevice.close();
				midiDevice = null;
				mainText.setText("USB MIDI connection closed.");
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void chooseMidiDevice() {
		final List<UsbMidiDevice> devices = UsbMidiDevice.getMidiDevices(this);
		new AsyncDeviceInfoLookup() {

			@Override
			protected void onLookupComplete() {
				new UsbDeviceSelector<UsbMidiDevice>(devices) {

					@Override
					protected void onDeviceSelected(UsbMidiDevice device) {
						midiDevice = device;
						mainText.setText("Selected device: " + device.getCurrentDeviceInfo());
						midiDevice.requestPermission(UsbMidiTest.this);
					}

					@Override
					protected void onNoSelection() {
						mainText.setText("No USB MIDI device selected.");
					}
				}.show(getFragmentManager(), null);
			}
		}.execute(devices.toArray(new UsbMidiDevice[devices.size()]));
	}
}
