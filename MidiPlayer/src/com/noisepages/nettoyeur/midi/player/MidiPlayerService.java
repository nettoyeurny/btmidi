/* Copyright (C) 2013 Peter Brinkmann
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.noisepages.nettoyeur.midi.player;

import java.io.IOException;
import java.io.InputStream;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.noisepages.nettoyeur.bluetooth.BluetoothSppConnection;
import com.noisepages.nettoyeur.bluetooth.BluetoothSppObserver;
import com.noisepages.nettoyeur.common.RawByteReceiver;
import com.noisepages.nettoyeur.midi.file.InvalidMidiDataException;


/**
 * Android service for playing MIDI files over Bluetooth.
 * 
 * @author Peter Brinkmann
 */
public class MidiPlayerService extends Service {

	private static final String TAG = "MidiPlayerService";
	private static final int ID = 1;

	private final Binder binder = new MidiPlayerServiceBinder();
	private BluetoothSppConnection btConnection = null;
	private MidiPlayerObserver observer = null;
	private volatile Uri uri = null;
	private volatile MidiSequencer sequencer = null;
	private volatile WakeLock wakeLock = null;

	private final RawByteReceiver rawByteReceiver = new RawByteReceiver() {
		@Override
		public void onBytesReceived(int nBytes, byte[] buffer) {
			if (btConnection == null) return;
			try {
				btConnection.write(buffer, 0, nBytes);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};
	
	private final BluetoothSppObserver sppObserver = new BluetoothSppObserver() {
		@Override
		public void onConnectionFailed() {
			stopForeground(true);
			if (observer != null) {
				observer.onConnectionFailed();
			}
		}

		@Override
		public void onConnectionLost() {
			stopForeground(true);
			pauseSong();
			wakeLock.release();
			if (observer != null) {
				observer.onConnectionLost();
			}
		}

		@Override
		public void onDeviceConnected(BluetoothDevice device) {
			wakeLock = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
			wakeLock.acquire();
			if (observer != null) {
				observer.onDeviceConnected(device);
			}
		}
	};

	public class MidiPlayerServiceBinder extends Binder {
		public MidiPlayerService getService() {
			return MidiPlayerService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onDestroy() {
		stop();
		super.onDestroy();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		try {
			btConnection = new BluetoothSppConnection(sppObserver, new RawByteReceiver() {
				@Override
				public void onBytesReceived(int nBytes, byte[] buffer) {
					// Do nothing.
				}
			}, 256);
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
	}
	
	/**
	 * Sets the receiver for Bluetooth and playback updates.
	 * 
	 * @param receiver
	 */
	public void setReceiver(MidiPlayerObserver receiver) {
		this.observer = receiver;
	}

	private void connect(String addr) throws IOException {
		btConnection.connect(addr);
	}

	/**
	 * Connects to the Bluetooth device and acquires foreground privileges.
	 * 
	 * @param addr address of Bluetooth device
	 * @param intent intent for returning to the managing activity; will be wrapped in a pending intent
	 * @param description message to appear in notification
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
	 * @return state of the Bluetooth connection
	 */
	public BluetoothSppConnection.State getState() {
		return btConnection.getConnectionState();
	}

	/**
	 * @return true if and only if the service is currently playing a MIDI file
	 */
	public boolean isPlaying() {
		return sequencer != null && sequencer.isPlaying();
	}

	/**
	 * Stops playback and disconnects Bluetooth.
	 */
	public void stop() {
		pauseSong();
		btConnection.stop();
		stopForeground(true);
	}

	/**
	 * @param uri URI to read MIDI data from
	 * @param is input stream to read MIDI data from
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	public void loadSong(Uri uri) throws InvalidMidiDataException, IOException { 
		InputStream is = getContentResolver().openInputStream(uri);
		sequencer = new MidiSequencer(is) {
			@Override
			protected void onPlaybackFinished() {
				if (observer == null) return;
				observer.onPlaybackFinished();
			}
		};
		this.uri = uri;
	}
	
	/**
	 * @return the current song URI
	 */
	public Uri getSongUri() {
		return uri;
	}
	
	/**
	 * Starts playback.
	 */
	public void startSong() {
		if (sequencer == null) return;
		sequencer.start(rawByteReceiver);
	}

	/**
	 * Pauses playback.
	 */
	public void pauseSong() {
		if (sequencer == null) return;
		sequencer.pause();
	}
	
	/**
	 * Pauses playback and rewinds.
	 */
	public void rewindSong() {
		if (sequencer == null) return;
		sequencer.rewind();
	}
}