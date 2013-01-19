/*
 * Copyright (C) 2013 Peter Brinkmann (peter.brinkmann@gmail.com)
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

package com.noisepages.nettoyeur.usb.midi;

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
import com.noisepages.nettoyeur.usb.UsbDeviceWithInfo;

/**
 * MIDI-specific wrapper for USB devices. Instances of this class will implicitly manage interfaces
 * and USB endpoints. MIDI connections (both input and output) go through the {@link MidiReceiver}
 * interface. MIDI inputs take receiver instances that they'll invoke on incoming events; MIDI
 * outputs provide receivers through which client code can send out MIDI events.
 * 
 * Note: Activities that use this class need to specify android:launchMode="singleTask" in their
 * manifest to make sure that the USB connection won't be lost on restart.
 * 
 * Acknowledgment: The implementation of the USB driver was done from scratch, but the idea of
 * writing a soft driver in Java came from Kaoru Shoji's USB-MIDI-Driver
 * (https://github.com/kshoji/USB-MIDI-Driver).
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 */
public class UsbMidiDevice extends UsbDeviceWithInfo {

	private final static String TAG = "UsbMidiDevice";
	
	private final List<UsbMidiInterface> interfaces = new ArrayList<UsbMidiDevice.UsbMidiInterface>();
	private volatile UsbDeviceConnection connection = null;

	/**
	 * MIDI-specific wrapper for USB interfaces within a USB devices. This class doesn't do much and mostly
	 * serves to organize inputs and outputs.
	 */
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

		/**
		 * @return an unmodifiable list of MIDI inputs belonging to this interface
		 */
		public List<UsbMidiInput> getInputs() {
			return Collections.unmodifiableList(inputs);
		}

		/**
		 * @return an unmodifiable list of MIDI outputs belonging to this interface
		 */
		public List<UsbMidiOutput> getOutputs() {
			return Collections.unmodifiableList(outputs);
		}

		/**
		 * Stops listening threads on all MIDI inputs belonging to this interface.
		 */
		public void stop() {
			for (UsbMidiInput input : inputs) {
				input.stop();
			}
		}
	}

	/**
	 * Wrapper for USB MIDI input endpoints.
	 */
	public class UsbMidiInput {
		private final UsbEndpoint inputEndpoint;
		private volatile FromWireConverter fromWire;
		private volatile int cable = -1;
		private volatile Thread inputThread = null;

		private UsbMidiInput(UsbEndpoint endpoint) {
			inputEndpoint = endpoint;
		}

		@Override
		public String toString() {
			return "in:" + inputEndpoint;
		}

		/**
		 * Sets the receiver for incoming MIDI events.
		 */
		public void setReceiver(MidiReceiver receiver) {
			fromWire = new FromWireConverter(receiver);
		}

		/**
		 * Sets the virtual cable to listen to; a value of -1 (the default) will
		 * enable reception from all virtual cables.
		 * 
		 * @param c virtual cable number, or -1 if listening on all cables
		 */
		public void setVirtualCable(int c) {
			cable = (c >= 0) ? (c << 4) & 0xf0 : -1;
		}

		/**
		 * Starts listening to this MIDI input; requires a receiver to be in place.
		 */
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

		/**
		 * Stops listening to this input.
		 */
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

	/**
	 * Wrapper for USB MIDI output endpoints.
	 */
	public class UsbMidiOutput {
		private final UsbEndpoint outputEndpoint;
		private volatile int cable;

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
			setVirtualCable(1);
		}

		@Override
		public String toString() {
			return "out:" + outputEndpoint;
		}

		/**
		 * Sets the virtual cable to write to; the default is 0.
		 * 
		 * @param c virtual cable number
		 */
		public void setVirtualCable(int c) {
			cable = (c << 4) & 0xf0;
		}

		public MidiReceiver getMidiOut() {
			return toWire;
		}
	}

	/**
	 * Scans the currently attached USB devices and returns those the look like MIDI devices. Note that there may be
	 * false positives since this method will list all devices with endpoints that look like MIDI endpoints. While
	 * this behavior is potentially awkward, it is preferable to the wholesale suppression of devices that are MIDI
	 * devices but fail to properly identify themselves as such (sadly, this is a common problem).
	 * 
	 * @param context the current context, e.g., the activity invoking this method
	 * @return list of (probable) MIDI devices
	 */
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

	/**
	 * Wraps a given USB device as a MIDI device if possible. If the device has no USB endpoints that look like MIDI
	 * endpoints, the result will be null. Otherwise, we'll just assume that the device is a MIDI device.
	 * 
	 * @param device to be wrapped
	 * @return MIDI device if possible, null otherwise
	 */
	public static UsbMidiDevice asMidiDevice(UsbDevice device) {
		UsbMidiDevice midiDevice = new UsbMidiDevice(device);
		return (!midiDevice.getInterfaces().isEmpty()) ? midiDevice : null;
	}
	
	private UsbMidiDevice(UsbDevice device) {
		super(device);
		int ifaceCount = device.getInterfaceCount();
		for (int i = 0; i < ifaceCount; ++i) {
			UsbInterface iface = device.getInterface(i);
			// We really ought to check interface class and subclass, but we don't since a lot of MIDI devices don't comply with the standard.
			// if (iface.getInterfaceClass() != 1 || iface.getInterfaceSubclass() != 3) continue;
			List<UsbEndpoint> inputs = new ArrayList<UsbEndpoint>();
			List<UsbEndpoint> outputs = new ArrayList<UsbEndpoint>();
			int epCount = iface.getEndpointCount();
			for (int j = 0; j < epCount; ++j) {
				UsbEndpoint ep = iface.getEndpoint(j);
				// If the endpoint looks like a MIDI endpoint, assume that it is one.
				if (ep.getMaxPacketSize() == 64 && (ep.getType() & UsbConstants.USB_ENDPOINT_XFERTYPE_MASK) == UsbConstants.USB_ENDPOINT_XFER_BULK) {
					if ((ep.getDirection() & UsbConstants.USB_ENDPOINT_DIR_MASK) == UsbConstants.USB_DIR_IN) {
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

	/**
	 * @return an unmodifiable list of MIDI interfaces belonging to this device.
	 */
	public List<UsbMidiInterface> getInterfaces() {
		return Collections.unmodifiableList(interfaces);
	}

	/**
	 * Opens a USB connection to this device.
	 * 
	 * @param context the current context, e.g., the activity invoking this method
	 */
	public void open(Context context) {
		UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		connection = manager.openDevice(device);
		for (UsbMidiInterface iface : interfaces) {
			connection.claimInterface(iface.getInterface(), true);
		}
	}

	/**
	 * Stops listening on all inputs and closes the current USB connection, if any.
	 */
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