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
import android.util.Log;

import com.noisepages.nettoyeur.bluetooth.BluetoothSppConnection;
import com.noisepages.nettoyeur.bluetooth.BluetoothSppObserver;
import com.noisepages.nettoyeur.bluetooth.R;
import com.noisepages.nettoyeur.midi.FromWireConverter;
import com.noisepages.nettoyeur.midi.MidiReceiver;
import com.noisepages.nettoyeur.midi.RawByteReceiver;
import com.noisepages.nettoyeur.midi.ToWireConverter;


/**
 * Service for MIDI over Bluetooth using SPP.
 * 
 * @author Peter Brinkmann
 */
public class BluetoothMidiService extends Service implements MidiReceiver {

	private static final String TAG = "BluetoothMidiService";
	private static final int ID = 1;
	
	private final Binder binder = new BluetoothMidiBinder();
	private BluetoothSppConnection btConnection = null;
	private BluetoothSppObserver observer = null;

	private final ToWireConverter toWire = new ToWireConverter(new RawByteReceiver() {
		@Override
		public void onBytesReceived(int nBytes, byte[] buffer) {
			if (btConnection == null) {
				throw new IllegalStateException("BluetoothMidiService has not been initialized");
			}
			try {
				btConnection.write(buffer, 0, buffer.length);
			} catch (IOException e) {
				Log.e(TAG, e.toString());
			}
		}
	});
	
	private final BluetoothSppObserver sppObserver = new BluetoothSppObserver() {
		@Override
		public void onConnectionFailed() {
			stopForeground(true);
			observer.onConnectionFailed();
		}

		@Override
		public void onConnectionLost() {
			stopForeground(true);
			observer.onConnectionLost();
		}

		@Override
		public void onDeviceConnected(BluetoothDevice device) {
			observer.onDeviceConnected(device);
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
	public void init(BluetoothSppObserver observer, MidiReceiver receiver) throws IOException {
		stop();
		this.observer = observer;
		btConnection = new BluetoothSppConnection(sppObserver, new FromWireConverter(receiver), 32);
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
	 */
	@Override
	public void onNoteOff(int ch, int note, int vel) {
		toWire.onNoteOff(ch, note, vel);
	}

	/**
	 * Sends a note on event to the Bluetooth device.
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
	 * Sends a polyphonic aftertouch event to the Bluetooth device.
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
	 * Sends a control change event to the Bluetooth device.
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
	 * Sends a program change event to the Bluetooth device.
	 * 
	 * @param ch channel starting at 0
	 * @param pgm
	 */
	@Override
	public void onProgramChange(int ch, int pgm) {
		toWire.onProgramChange(ch, pgm);
	}

	/**
	 * Sends a channel aftertouch event to the Bluetooth device.
	 * 
	 * @param ch channel starting at 0
	 * @param vel
	 */
	@Override
	public void onAftertouch(int ch, int vel) {
		toWire.onAftertouch(ch, vel);
	}

	/**
	 * Sends a pitch bend event to the Bluetooth device.
	 * 
	 * @param ch channel starting at 0
	 * @param val pitch bend value centered at 0, ranging from -8192 to 8191
	 */
	@Override
	public void onPitchBend(int ch, int val) {
		toWire.onPitchBend(ch, val);
	}

	/**
	 * Sends a raw MIDI byte to the Bluetooth device.
	 * 
	 * @param value MIDI byte to send to device; only the LSB will be sent
	 */
	@Override
	public void onRawByte(int value) {
		toWire.onRawByte(value);
	}
}
