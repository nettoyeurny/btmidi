/*
 * Copyright (C) 2011 Peter Brinkmann (peter.brinkmann@gmail.com)
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

package com.noisepages.nettoyeur.bluetooth.midi;

import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.noisepages.nettoyeur.bluetooth.BluetoothSppConnection;
import com.noisepages.nettoyeur.bluetooth.BluetoothSppReceiver;
import com.noisepages.nettoyeur.bluetooth.R;


/**
 * Service for MIDI over Bluetooth using SPP.  Note that these methods choose sanity
 * over compliance with the MIDI standard, i.e., channel numbers start at 0, and pitch
 * bend values are centered at 0.
 * 
 * @author Peter Brinkmann
 */
public class BluetoothMidiService extends Service {

	private static final String TAG = "BluetoothMidiService";
	private static final int ID = 1;
	
	private static enum State {
		NOTE_OFF,
		NOTE_ON,
		POLY_TOUCH,
		CONTROL_CHANGE,
		PROGRAM_CHANGE,
		AFTERTOUCH,
		PITCH_BEND,
		NONE
	}

	private final Binder binder = new BluetoothMidiBinder();
	private BluetoothSppConnection btConnection = null;
	private BluetoothMidiReceiver receiver = null;
	private State midiState = State.NONE;
	private int channel;
	private int firstByte;

	private final BluetoothSppReceiver sppReceiver = new BluetoothSppReceiver() {
		@Override
		public void onBytesReceived(int nBytes, byte[] buffer) {
			for (int i = 0; i < nBytes; i++) {
				processByte(buffer[i]);
			}
		}

		@Override
		public void onConnectionFailed() {
			stopForeground(true);
			receiver.onConnectionFailed();
		}

		@Override
		public void onConnectionLost() {
			stopForeground(true);
			receiver.onConnectionLost();
		}

		@Override
		public void onDeviceConnected(BluetoothDevice device) {
			receiver.onDeviceConnected(device);
		}
	};

	public class BluetoothMidiBinder extends Binder {
		public BluetoothMidiService getService() {
			return BluetoothMidiService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		stop();
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		stop();
		super.onDestroy();
	}

	/**
	 * This method must be called before any other methods are called.  It sets the receiver for
	 * handling events read from the Bluetooth input stream.
	 * 
	 * @param receiver
	 * @throws IOException thrown if Bluetooth is unavailable or disabled
	 */
	public void init(BluetoothMidiReceiver receiver) throws IOException {
		stop();
		btConnection = new BluetoothSppConnection(sppReceiver, 32);
		this.receiver = receiver;
	}

	/**
	 * Attempts to connect to the given Bluetooth device.
	 * 
	 * @param addr String representation of the MAC address of the Bluetooth device
	 * @throws IOException
	 */
	public void connect(String addr) throws IOException {
		if (btConnection == null) {
			throw new IllegalStateException("BluetoothMidiService has not been initialized");
		}
		btConnection.connect(addr);
	}

	/**
	 * Attempts to connect to the given Bluetooth device with foreground privileges.
	 * 
	 * @param addr String representation of the MAC address of the Bluetooth device
	 * @param intent intent to be wrapped in a pending intent and fired when the user selects the notification
	 * @param description description of the notification
	 * @throws IOException 
	 */
	public void connect(String addr, Intent intent, String description) throws IOException {
		connect(addr);
		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
		Notification notification = new Notification(R.drawable.din5, TAG, System.currentTimeMillis());
		notification.setLatestEventInfo(this, TAG, description, pi);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		startForeground(ID, notification);
	}

	/**
	 * @return the current state of the Bluetooth connection
	 */
	public BluetoothSppConnection.State getState() {
		if (btConnection == null) {
			throw new IllegalStateException("BluetoothMidiService has not been initialized");
		}
		return btConnection.getState();
	}

	/**
	 * Stops all Bluetooth threads and closes the Bluetooth connection.
	 */
	public void stop() {
		if (btConnection != null) {
			btConnection.stop();
		}
		stopForeground(true);
	}

	/**
	 * Sends a note off event to the Bluetooth device.
	 * 
	 * @param ch channel starting at 0
	 * @param note
	 * @param vel
	 * @throws IOException
	 */
	public void sendNoteOff(int ch, int note, int vel) throws IOException {
		write(0x80, ch, note, vel);
	}

	/**
	 * Sends a note on event to the Bluetooth device.
	 * 
	 * @param ch channel starting at 0
	 * @param note
	 * @param vel
	 * @throws IOException
	 */
	public void sendNoteOn(int ch, int note, int vel) throws IOException {
		write(0x90, ch, note, vel);
	}

	/**
	 * Sends a polyphonic aftertouch event to the Bluetooth device.
	 * 
	 * @param ch channel starting at 0
	 * @param note
	 * @param vel
	 * @throws IOException
	 */
	public void sendPolyAftertouch(int ch, int note, int vel) throws IOException {
		write(0xa0, ch, note, vel);
	}

	/**
	 * Sends a control change event to the Bluetooth device.
	 * 
	 * @param ch channel starting at 0
	 * @param ctl
	 * @param val
	 * @throws IOException
	 */
	public void sendControlChange(int ch, int ctl, int val) throws IOException {
		write(0xb0, ch, ctl, val);
	}

	/**
	 * Sends a program change event to the Bluetooth device.
	 * 
	 * @param ch channel starting at 0
	 * @param pgm
	 * @throws IOException
	 */
	public void sendProgramChange(int ch, int pgm) throws IOException {
		write(0xc0, ch, pgm);
	}

	/**
	 * Sends a channel aftertouch event to the Bluetooth device.
	 * 
	 * @param ch channel starting at 0
	 * @param vel
	 * @throws IOException
	 */
	public void sendAftertouch(int ch, int vel) throws IOException {
		write(0xd0, ch, vel);
	}

	/**
	 * Sends a pitch bend event to the Bluetooth device.
	 * 
	 * @param ch channel starting at 0
	 * @param val pitch bend value centered at 0, ranging from -8192 to 8191
	 * @throws IOException
	 */
	public void sendPitchbend(int ch, int val) throws IOException {
		val += 8192;
		write(0xe0, ch, (val & 0x7f), (val >> 7));
	}

	/**
	 * Sends a raw MIDI byte to the Bluetooth device.
	 * 
	 * @param value MIDI byte to send to device; only the LSB will be sent
	 * @throws IOException
	 */
	public void sendRawByte(int value) throws IOException {
		writeBytes((byte) value);
	}

	private void write(int msg, int ch, int a) throws IOException {
		writeBytes(firstByte(msg, ch), (byte) a);
	}

	private void write(int msg, int ch, int a, int b) throws IOException {
		writeBytes(firstByte(msg, ch), (byte) a, (byte) b);
	}

	private byte firstByte(int msg, int ch) {
		return (byte)(msg | (ch & 0x0f));
	}

	private void writeBytes(byte... out) throws IOException {
		if (btConnection == null) {
			throw new IllegalStateException("BluetoothMidiService has not been initialized");
		}
		btConnection.write(out, 0, out.length);
	}

	private void processByte(int b) {
		if (b < 0) {
			midiState = State.values()[(b >> 4) & 0x07];
			if (midiState != State.NONE) {
				channel = b & 0x0f;
				firstByte = -1;
			} else {
				receiver.onRawByte(b);
			}
		} else {
			switch (midiState) {
			case NOTE_OFF:
				if (firstByte < 0) {
					firstByte = b;
				} else {
					receiver.onNoteOff(channel, firstByte, b);
					firstByte = -1;
				}
				break;
			case NOTE_ON:
				if (firstByte < 0) {
					firstByte = b;
				} else {
					receiver.onNoteOn(channel, firstByte, b);
					firstByte = -1;
				}
				break;
			case POLY_TOUCH:
				if (firstByte < 0) {
					firstByte = b;
				} else {
					receiver.onPolyAftertouch(channel, firstByte, b);
					firstByte = -1;
				}
				break;
			case CONTROL_CHANGE:
				if (firstByte < 0) {
					firstByte = b;
				} else {
					receiver.onControlChange(channel, firstByte, b);
					firstByte = -1;
				}
				break;
			case PROGRAM_CHANGE:
				receiver.onProgramChange(channel, b);
				break;
			case AFTERTOUCH:
				receiver.onAftertouch(channel, b);
				break;
			case PITCH_BEND:
				if (firstByte < 0) {
					firstByte = b;
				} else {
					receiver.onPitchBend(channel, ((b << 7) | firstByte) - 8192);
					firstByte = -1;
				}
				break;
			default:
				receiver.onRawByte(b);
				break;
			}
		}
	}
}
