package com.noisepages.nettoyeur.usbmidi;

import java.util.ArrayList;
import java.util.Arrays;
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

public class UsbMidiInterface implements MidiReceiver {
	
	private final static String TAG = "UsbMidiInterface";
	
	private final UsbDevice device;
	private final UsbInterface iface;
	private final UsbEndpoint inputEndpoint;
	private final UsbEndpoint outputEndpoint;
	private volatile UsbDeviceConnection connection = null;
	private volatile FromWireConverter fromWire;
	private volatile int cable;
	private volatile Thread inputThread = null;
	
	private final ToWireConverter toWire = new ToWireConverter(new RawByteReceiver() {
		private final byte[] outBuffer = new byte[4];
		@Override
		public void onBytesReceived(int nBytes, byte[] buffer) {
			if (outputEndpoint == null || connection == null) return;
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
			Log.i(TAG, Arrays.toString(outBuffer));
			connection.bulkTransfer(outputEndpoint, outBuffer, 4, 0);
		}
	});
	
	public static List<UsbMidiInterface> getMidiInterfaces(Context context) {
		List<UsbMidiInterface> midiInterfaces = new ArrayList<UsbMidiInterface>();
		UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		for (UsbDevice device : manager.getDeviceList().values()) {
			int ifaceCount = device.getInterfaceCount();
			for (int i = 0; i < ifaceCount; ++i) {
				UsbInterface iface = device.getInterface(i);
				if (iface.getInterfaceClass() != 1 || iface.getInterfaceSubclass() != 3) continue;  // Not MIDI?
				UsbEndpoint input = null;
				UsbEndpoint output = null;
				int epCount = iface.getEndpointCount();
				for (int j = 0; j < epCount; ++j) {
					UsbEndpoint ep = iface.getEndpoint(j);
					if (ep.getMaxPacketSize() == 64 && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
						if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
							input = ep;
						} else {
							output = ep;
						}
					}
				}
				if (input != null || output != null) {
					midiInterfaces.add(new UsbMidiInterface(device, iface, input, output));
				}
			}
			
		}
		return midiInterfaces;
	}
	
	private UsbMidiInterface(UsbDevice device, UsbInterface iface, UsbEndpoint inputEndpoint, UsbEndpoint outputEndpoint) {
		this.device = device;
		this.iface = iface;
		this.inputEndpoint = inputEndpoint;
		this.outputEndpoint = outputEndpoint;
	}
	
	public UsbDevice getDevice() {
		return device;
	}
	
	public boolean hasInput() {
		return inputEndpoint != null;
	}
	
	public boolean hasOutput() {
		return outputEndpoint != null;
	}
	
	public void open(Context context, int cable, MidiReceiver receiver) {
		UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		connection = manager.openDevice(device);
		connection.claimInterface(iface, true);
		this.cable = (cable << 4) & 0xf0;
		fromWire = (receiver != null) ? new FromWireConverter(receiver) : null;
	}
	
	public void close() {
		if (connection == null) return;
		stop();
		connection.releaseInterface(iface);
		connection.close();
		connection = null;
	}
	
	public void start() {
		if (inputEndpoint == null || connection == null) return;
		stop();
		inputThread = new Thread() {
			private final byte[] inputBuffer = new byte[64];
			private final byte[] tmpBuffer = new byte[3];
			@Override
			public void run() {
				while (!interrupted()) {
					int nRead = connection.bulkTransfer(inputEndpoint, inputBuffer, inputBuffer.length, 100);
					Log.i(TAG, "nRead: " + nRead + ", buffer: " + Arrays.toString(inputBuffer));
					for (int i = 0; i < nRead; i += 4) {
						byte b = inputBuffer[i];
						if ((b & 0xf0) != cable) continue;
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
							FromWireConverter conv = fromWire;
							if (conv != null) {
								conv.onBytesReceived(n, tmpBuffer);
							}
						}
					}
				}
			}
		};
		inputThread.start();
	}
	
	public void stop() {
		if (connection == null) return;
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
	
	/**
	 * Sends a note off event to the USB device.
	 * 
	 * @param ch channel starting at 0
	 * @param note
	 * @param vel
	 */
	@Override
	public void onNoteOff(int ch, int note, int vel) {
		toWire.onNoteOff(ch, note, vel);
	}

	/**
	 * Sends a note on event to the USB device.
	 * 
	 * @param ch channel starting at 0
	 * @param note
	 * @param vel
	 */
	@Override
	public void onNoteOn(int ch, int note, int vel) {
		toWire.onNoteOn(ch, note, vel);
	}

	/**
	 * Sends a polyphonic aftertouch event to the USB device.
	 * 
	 * @param ch channel starting at 0
	 * @param note
	 * @param vel
	 */
	@Override
	public void onPolyAftertouch(int ch, int note, int vel) {
		toWire.onPolyAftertouch(ch, note, vel);
	}

	/**
	 * Sends a control change event to the USB device.
	 * 
	 * @param ch channel starting at 0
	 * @param ctl
	 * @param val
	 */
	@Override
	public void onControlChange(int ch, int ctl, int val) {
		toWire.onControlChange(ch, ctl, val);
	}

	/**
	 * Sends a program change event to the USB device.
	 * 
	 * @param ch channel starting at 0
	 * @param pgm
	 */
	@Override
	public void onProgramChange(int ch, int pgm) {
		toWire.onProgramChange(ch, pgm);
	}

	/**
	 * Sends a channel aftertouch event to the USB device.
	 * 
	 * @param ch channel starting at 0
	 * @param vel
	 */
	@Override
	public void onAftertouch(int ch, int vel) {
		toWire.onAftertouch(ch, vel);
	}

	/**
	 * Sends a pitch bend event to the USB device.
	 * 
	 * @param ch channel starting at 0
	 * @param val pitch bend value centered at 0, ranging from -8192 to 8191
	 */
	@Override
	public void onPitchBend(int ch, int val) {
		toWire.onPitchBend(ch, val);
	}

	/**
	 * Sends a raw MIDI byte to the USB device.
	 * 
	 * @param value MIDI byte to send to device; only the LSB will be sent
	 */
	@Override
	public void onRawByte(int value) {
		toWire.onRawByte(value);
	}
}