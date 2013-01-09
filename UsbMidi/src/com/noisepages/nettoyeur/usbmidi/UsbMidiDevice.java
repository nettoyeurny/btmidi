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

package com.noisepages.nettoyeur.usbmidi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.noisepages.nettoyeur.midi.FromWireConverter;
import com.noisepages.nettoyeur.midi.MidiReceiver;
import com.noisepages.nettoyeur.midi.RawByteReceiver;
import com.noisepages.nettoyeur.midi.ToWireConverter;

public class UsbMidiDevice {

	private final static String TAG = "UsbMidiDevice";

	private final UsbDevice device;
	private final List<UsbMidiInterface> interfaces = new ArrayList<UsbMidiDevice.UsbMidiInterface>();
	private volatile UsbDeviceConnection connection = null;

	public class UsbMidiInterface {
		private final UsbInterface iface;
		private final List<UsbMidiInput> inputs;
		private final List<UsbMidiOutput> outputs;

		private UsbMidiInterface(UsbInterface iface, List<UsbMidiInput> inputs, List<UsbMidiOutput> outputs) {
			this.iface = iface;
			this.inputs = inputs;
			this.outputs = outputs;
		}

		private UsbInterface getInterface() {
			return iface;
		}

		public String toString() {
			return iface.toString();
		}

		public List<UsbMidiInput> getInputs() {
			return Collections.unmodifiableList(inputs);
		}

		public List<UsbMidiOutput> getOutputs() {
			return Collections.unmodifiableList(outputs);
		}

		public void stop() {
			for (UsbMidiInput input : inputs) {
				input.stop();
			}
		}
	}

	public class UsbMidiInput {
		private final UsbEndpoint inputEndpoint;
		private volatile FromWireConverter fromWire;
		private volatile int cable = -1;
		private volatile Thread inputThread = null;

		private UsbMidiInput(UsbEndpoint ep) {
			inputEndpoint = ep;
		}

		@Override
		public String toString() {
			return "in:" + inputEndpoint;
		}

		public void setReceiver(MidiReceiver r) {
			fromWire = new FromWireConverter(r);
		}

		public void setVirtualCable(int c) {
			cable = (c >= 0) ? (c << 4) & 0xf0 : -1;
		}

		public void start() {
			if (fromWire == null) {
				throw new IllegalStateException("no receiver");
			}
			if (connection == null) return;
			stop();
			inputThread = new Thread() {
				private final byte[] inputBuffer = new byte[64];
				private final byte[] tmpBuffer = new byte[3];
				@Override
				public void run() {
					while (!interrupted()) {
						int nRead = connection.bulkTransfer(inputEndpoint, inputBuffer, inputBuffer.length, 100);
						for (int i = 0; i < nRead; i += 4) {
							byte b = inputBuffer[i];
							if (cable >= 0 && (b & 0xf0) != cable) continue;
							b &= 0x0f;
							if (b >= 0x08) {
								int n = 0;
								tmpBuffer[n++] = inputBuffer[i + 1];
								if (b != 0x0f) {
									tmpBuffer[n++] = inputBuffer[i + 2];
									if (b != 0x0c && b != 0x0d) {
										tmpBuffer[n++] = inputBuffer[i + 3];
									}
								}
								fromWire.onBytesReceived(n, tmpBuffer);
							}
						}
					}
				}
			};
			inputThread.start();
		}

		public void stop() {
			if (inputThread != null) {
				inputThread.interrupt();
				try {
					inputThread.join();
				} catch (InterruptedException e) {
					// Do nothing.
				}
				inputThread = null;
			}
		}
	}

	public class UsbMidiOutput implements MidiReceiver {
		private final UsbEndpoint outputEndpoint;
		private volatile int cable = 0;

		private final ToWireConverter toWire = new ToWireConverter(new RawByteReceiver() {
			private final byte[] outBuffer = new byte[4];
			@Override
			public void onBytesReceived(int nBytes, byte[] buffer) {
				if (connection == null) return;
				if (nBytes == 0) return;
				if (nBytes > 3) {
					Log.w(TAG, "bad buffer size: " + nBytes);
					return;
				}
				Arrays.fill(outBuffer, (byte) 0);
				if (nBytes > 1) {
					outBuffer[0] = (byte) (cable | (buffer[0] >> 4));
				} else {
					outBuffer[0] = (byte) (cable | 0x0f);
				}
				for (int i = 0; i < nBytes; ++i) {
					outBuffer[i + 1] = buffer[i];
				}
				connection.bulkTransfer(outputEndpoint, outBuffer, 4, 0);
			}
		});

		private UsbMidiOutput(UsbEndpoint ep) {
			outputEndpoint = ep;
		}

		@Override
		public String toString() {
			return "out:" + outputEndpoint;
		}

		public void setVirtualCable(int c) {
			cable = (c << 4) & 0xf0;
		}

		@Override
		public void onNoteOff(int ch, int note, int vel) {
			toWire.onNoteOff(ch, note, vel);
		}

		@Override
		public void onNoteOn(int ch, int note, int vel) {
			toWire.onNoteOn(ch, note, vel);
		}

		@Override
		public void onPolyAftertouch(int ch, int note, int vel) {
			toWire.onPolyAftertouch(ch, note, vel);
		}

		@Override
		public void onControlChange(int ch, int ctl, int val) {
			toWire.onControlChange(ch, ctl, val);
		}

		@Override
		public void onProgramChange(int ch, int pgm) {
			toWire.onProgramChange(ch, pgm);
		}

		@Override
		public void onAftertouch(int ch, int vel) {
			toWire.onAftertouch(ch, vel);
		}

		@Override
		public void onPitchBend(int ch, int val) {
			toWire.onPitchBend(ch, val);
		}

		@Override
		public void onRawByte(int value) {
			toWire.onRawByte(value);
		}
	}

	public static List<UsbMidiDevice> getMidiDevices(Context context) {
		List<UsbMidiDevice> midiDevices = new ArrayList<UsbMidiDevice>();
		UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		for (UsbDevice device : manager.getDeviceList().values()) {
			UsbMidiDevice midiDevice = new UsbMidiDevice(device);
			if (!midiDevice.getInterfaces().isEmpty()) {
				midiDevices.add(midiDevice);
			}
		}
		return midiDevices;
	}

	public UsbMidiDevice(UsbDevice device) {
		this.device = device;
		int ifaceCount = device.getInterfaceCount();
		for (int i = 0; i < ifaceCount; ++i) {
			UsbInterface iface = device.getInterface(i);
			Log.i(TAG, "device: " + device.getDeviceName() + ", class: " + iface.getInterfaceClass() + ", subclass: " + iface.getInterfaceSubclass());
			if (iface.getInterfaceClass() != 1 || iface.getInterfaceSubclass() != 3) continue;  // Not MIDI?
			List<UsbEndpoint> inputs = new ArrayList<UsbEndpoint>();
			List<UsbEndpoint> outputs = new ArrayList<UsbEndpoint>();
			int epCount = iface.getEndpointCount();
			for (int j = 0; j < epCount; ++j) {
				UsbEndpoint ep = iface.getEndpoint(j);
				if (ep.getMaxPacketSize() == 64 && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
					if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
						inputs.add(ep);
					} else {
						outputs.add(ep);
					}
				}
			}
			if (!inputs.isEmpty() || !outputs.isEmpty()) {
				addInterface(iface, inputs, outputs);
			}
		}
	}

	private void addInterface(UsbInterface iface, List<UsbEndpoint> inputs, List<UsbEndpoint> outputs) {
		List<UsbMidiInput> midiInputs = new ArrayList<UsbMidiDevice.UsbMidiInput>();
		List<UsbMidiOutput> midiOutputs = new ArrayList<UsbMidiDevice.UsbMidiOutput>();
		for (UsbEndpoint ep : inputs) {
			midiInputs.add(new UsbMidiInput(ep));
		}
		for (UsbEndpoint ep: outputs) {
			midiOutputs.add(new UsbMidiOutput(ep));
		}
		interfaces.add(new UsbMidiInterface(iface, midiInputs, midiOutputs));
	}

	@Override
	public String toString() {
		return device.toString();
	}

	public UsbDevice getDevice() {
		return device;
	}

	public List<UsbMidiInterface> getInterfaces() {
		return Collections.unmodifiableList(interfaces);
	}

	public void open(Context context) {
		UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		connection = manager.openDevice(device);
		for (UsbMidiInterface iface : interfaces) {
			connection.claimInterface(iface.getInterface(), true);
		}
	}

	public void close() {
		if (connection == null) return;
		for (UsbMidiInterface iface : interfaces) {
			iface.stop();
			connection.releaseInterface(iface.getInterface());
		}
		connection.close();
		connection = null;
	}
}