/*
 * Copyright (C) 2012 Peter Brinkmann (peter.brinkmann@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.noisepages.nettoyeur.usbmiditest;

import java.util.List;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import com.noisepages.nettoyeur.midi.MidiReceiver;
import com.noisepages.nettoyeur.usb.PermissionHandler;
import com.noisepages.nettoyeur.usb.UsbDeviceWithInfo;
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
		public void onRawByte(int value) {
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
				mainText.setText(n);
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
		UsbMidiDevice.installPermissionHandler(this, new PermissionHandler() {

			@Override
			public void onPermissionGranted(UsbDevice device) {
				midiDevice.open(UsbMidiTest.this);
				new UsbMidiInputSelector(midiDevice) {

					@Override
					protected void onInputSelected(UsbMidiInput input, int iface, int index) {
						mainText.setText(mainText.getText() + "\n\nInput: Interface " + iface + ", Index " + index);
						input.setReceiver(midiReceiver);
						input.start();

					}
				}.show(getFragmentManager(), null);
			}

			@Override
			public void onPermissionDenied(UsbDevice device) {
				mainText.setText("Permission denied for device " + midiDevice.getCurrentDeviceInfo() + ".");
			}
		});
		chooseUsbMidiDevice();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (midiDevice != null) {
			midiDevice.close();
		}
		UsbMidiDevice.uninstallPermissionHandler(this);
	}

	private void chooseUsbMidiDevice() {
		final List<UsbMidiDevice> devices = UsbMidiDevice.getMidiDevices(this);
		if (!devices.isEmpty()) {
			new AsyncDeviceInfoLookup<UsbDeviceWithInfo>() {

				@Override
				protected void onLookupComplete() {
					new UsbDeviceSelector<UsbMidiDevice>(devices) {

						@Override
						protected void onDeviceSelected(UsbMidiDevice device) {
							midiDevice = device;
							mainText.setText("Selected device: " + device.getCurrentDeviceInfo());
							midiDevice.requestPermission(UsbMidiTest.this);
						}
					}.show(getFragmentManager(), null);
				}
			}.execute(devices.toArray(new UsbMidiDevice[devices.size()]));
		} else {
			mainText.setText("No USB MIDI devices found.");
		}
	}
}
