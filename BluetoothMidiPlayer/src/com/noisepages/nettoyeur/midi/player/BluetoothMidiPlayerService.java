/* Copyright (C) 2011 Peter Brinkmann
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
import java.util.Iterator;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import com.noisepages.nettoyeur.bluetooth.BluetoothSppConnection;
import com.noisepages.nettoyeur.bluetooth.BluetoothSppObserver;
import com.noisepages.nettoyeur.bluetooth.R;
import com.noisepages.nettoyeur.midi.RawByteReceiver;
import com.noisepages.nettoyeur.midi.file.InvalidMidiDataException;
import com.noisepages.nettoyeur.midi.player.MidiFileSequencer.CompoundMidiEvent;


/**
 * Android service for playing MIDI files over Bluetooth.
 * 
 * @author Peter Brinkmann
 */
public class BluetoothMidiPlayerService extends Service {

	private static final String TAG = "BluetoothMidiPlayerService";
	private static final int ID = 1;

	private final Binder binder = new BluetoothSppBinder();
	private BluetoothSppConnection btConnection = null;
	private volatile Uri uri = null;
	private volatile MidiFileSequencer sequencer = null;
	private volatile BluetoothMidiPlayerReceiver receiver = null;
	private volatile HandlerThread handlerThread = null;
	private volatile Iterator<CompoundMidiEvent> events = null;
	private volatile WakeLock wakeLock;

	private class MidiRunnable implements Runnable {
		private byte[] buffer;
		private CompoundMidiEvent currentEvent;
		private final long t0;
		private final Iterator<CompoundMidiEvent> events;
		private final Handler handler;
		
		MidiRunnable(Iterator<CompoundMidiEvent> events, HandlerThread thread) {
			this.handler = new Handler(thread.getLooper());
			this.events = events;
			currentEvent = events.next();
			t0 = SystemClock.uptimeMillis() - currentEvent.timeInMillis + 250;
		}
		
		void scheduleNext() {
			buffer = currentEvent.midiBytes;
			handler.postAtTime(this, t0 + currentEvent.timeInMillis);
		}

		@Override
		public void run() {
			try {
				btConnection.write(buffer, 0, buffer.length);
				if (events.hasNext()) {
					currentEvent = events.next();
					scheduleNext();
				} else {
					pauseSong();
				}
			} catch (IOException e) {
				Log.e(TAG, e.toString());
				pauseSong();
			}
		}
	}
	
	private final RawByteReceiver rawByteReceiver = new RawByteReceiver() {
		@Override
		public void onBytesReceived(int nBytes, byte[] buffer) {
			// Do nothing.
		}
	};
	
	private final BluetoothSppObserver sppObserver = new BluetoothSppObserver() {
		@Override
		public void onConnectionFailed() {
			stopForeground(true);
			if (receiver != null) {
				receiver.onConnectionFailed();
			}
		}

		@Override
		public void onConnectionLost() {
			stopForeground(true);
			pauseSong();
			wakeLock.release();
			if (receiver != null) {
				receiver.onConnectionLost();
			}
		}

		@Override
		public void onDeviceConnected(BluetoothDevice device) {
			wakeLock = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
			wakeLock.acquire();
			if (receiver != null) {
				receiver.onDeviceConnected(device);
			}
		}
	};

	public class BluetoothSppBinder extends Binder {
		public BluetoothMidiPlayerService getService() {
			return BluetoothMidiPlayerService.this;
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
			btConnection = new BluetoothSppConnection(sppObserver, rawByteReceiver, 256);
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
	}
	
	/**
	 * Sets the receiver for Bluetooth and playback updates.
	 * 
	 * @param receiver
	 */
	public void setReceiver(BluetoothMidiPlayerReceiver receiver) {
		this.receiver = receiver;
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
		return btConnection.getState();
	}

	/**
	 * @return true if and only if the service is currently playing a MIDI file
	 */
	public boolean isPlaying() {
		return handlerThread != null;
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
		sequencer = new MidiFileSequencer(is);
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
		pauseSong();
		if (events == null || !events.hasNext()) {
			resetAllControllers();
			events = sequencer.iterator();
			if (!events.hasNext()) {
				if (receiver != null) {
					receiver.onPlaybackFinished();
					return;
				}
			}
		}
		handlerThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_AUDIO);
		handlerThread.start();
		MidiRunnable midiRunnable = new MidiRunnable(events, handlerThread);
		midiRunnable.scheduleNext();
	}

	/**
	 * Pauses playback.
	 */
	public void pauseSong() {
		if (handlerThread == null) return;
		handlerThread.quit();
		handlerThread = null;
		allNotesOff();
		if (receiver != null) {
			receiver.onPlaybackFinished();
		}
	}
	
	/**
	 * Pauses playback and rewinds.
	 */
	public void rewindSong() {
		pauseSong();
		events = null;
	}

	private void allNotesOff() {
		allChannels((byte) 0x7b, (byte) 0);
	}
	
	private void resetAllControllers() {
		allChannels((byte) 0x79, (byte) 0);
	}

	private void allChannels(byte controller, byte v) {
		byte[] buffer = new byte[] {0, controller, v};
		try {
			for (int c = 0; c < 16; c++) {
				buffer[0] = (byte) (0xb0 | c);
				btConnection.write(buffer, 0, buffer.length);
			}
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
	}
}