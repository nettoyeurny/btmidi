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

import java.io.InputStream;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

import com.noisepages.nettoyeur.midi.FromWireConverter;
import com.noisepages.nettoyeur.midi.MidiDevice;
import com.noisepages.nettoyeur.midi.MidiReceiver;


public class MidiPlayerService extends Service {

	public enum ConnectionType {
		NONE,
		BLUETOOTH,
		USB,
	}

	private static final CharSequence TAG = "MidiPlayerService";
	private static final int ID = 1;
	
	private volatile ConnectionType connectionType = ConnectionType.NONE;
	private volatile MidiDevice midiDevice = null;
	private volatile MidiSequence midiSequence = null;
	private volatile FromWireConverter midiConverter = null;
	private volatile Uri uri = null;
	
	private final Binder binder = new MidiPlayerServiceBinder();
	
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
	public boolean onUnbind(Intent intent) {
		reset();
		return super.onUnbind(intent);
	}

	public void reset() {
		if (isInitialized()) {
			pause();
		}
		if (midiDevice != null) {
			midiDevice.close();
			midiDevice = null;
			midiConverter = null;
		}
		connectionType = ConnectionType.NONE;
		stopForeground(true);
	}

	public boolean isInitialized() {
		return midiSequence != null;
	}

	public ConnectionType getConnectionType() {
		return connectionType;
	}
	
	public boolean loadMidiSequence(Uri uri, final MidiSequenceObserver observer) {
		if (isInitialized()) {
			pause();
		}
		try {
			InputStream is = getContentResolver().openInputStream(uri);
			midiSequence = new MidiSequence(is, new MidiSequenceObserver() {
				@Override
				public void onPlaybackFinished(MidiSequence sequence) {
					observer.onPlaybackFinished(sequence);
					stopForeground(true);
				}
			});
			this.uri = uri;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public long getDuration() {
		return isInitialized() ? midiSequence.duration : 0;
	}
	
	public Uri getUri() {
		return uri;
	}
	
	public void connectBluetooth(MidiDevice device, MidiReceiver receiver) {
		reset();
		midiDevice = device;
		midiConverter = new FromWireConverter(receiver);
		connectionType = ConnectionType.BLUETOOTH;
	}
	
	public void connectUsb(MidiDevice device, MidiReceiver receiver) {
		reset();
		midiDevice = device;
		midiConverter = new FromWireConverter(receiver);
		connectionType = ConnectionType.USB;
	}
	
	public boolean isPlaying() {
		return isInitialized() && midiSequence.isPlaying();
	}
	
	@SuppressWarnings("deprecation")
	public void start(Intent intent) {
		if (!isInitialized()) {
			throw new IllegalStateException("MidiPlayerService not initialized");
		}
		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
		Notification notification = new Notification(R.drawable.din5, TAG, System.currentTimeMillis());
		notification.setLatestEventInfo(this, TAG, "Return to MidiPlayer", pi);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		startForeground(ID, notification);
		midiSequence.start(midiConverter);
	}

	public void pause() {
		if (!isInitialized()) {
			throw new IllegalStateException("MidiPlayerService not initialized");
		}
		midiSequence.pause();
		stopForeground(true);
	}

	public void rewind() {
		if (!isInitialized()) {
			throw new IllegalStateException("MidiPlayerService not initialized");
		}
		midiSequence.rewind();
		stopForeground(true);
	}
}