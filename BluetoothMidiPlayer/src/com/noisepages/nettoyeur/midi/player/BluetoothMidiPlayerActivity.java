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

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.noisepages.nettoyeur.bluetooth.BluetoothSppConnection;
import com.noisepages.nettoyeur.bluetooth.DeviceListActivity;


/**
 * Simple activity for playing MIDI files over Bluetooth.
 * 
 * @author Peter Brinkmann
 */
public class BluetoothMidiPlayerActivity extends Activity implements BluetoothMidiPlayerReceiver, OnClickListener {

	private static final int CONNECT = 1;

	private BluetoothMidiPlayerService midiService = null;
	private Button connectButton;
	private ImageButton playButton;
	private ImageButton rewindButton;
	private TextView uriView;
	private Toast toast = null;

	private final ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			midiService = ((BluetoothMidiPlayerService.BluetoothSppBinder) service).getService();
			midiService.setReceiver(BluetoothMidiPlayerActivity.this);
			Uri uri = getIntent().getData();
			if (uri != null) {
				try {
					midiService.loadSong(uri);
				} catch (Exception e) {
					toast(e.toString());
					finish();
				}
			}
			updateWidgets();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// this method will never be called
		}
	};

	private void toast(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (toast == null) {
					toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
				}
				toast.setText(msg);
				toast.show();
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		connectButton = (Button) findViewById(R.id.connectButton);
		playButton = (ImageButton) findViewById(R.id.playButton);
		rewindButton = (ImageButton) findViewById(R.id.rewindButton);
		uriView = (TextView) findViewById(R.id.uriView);
		connectButton.setOnClickListener(this);
		playButton.setOnClickListener(this);
		rewindButton.setOnClickListener(this);
		uriView.setText(R.string.loading);
		bindService(new Intent(this, BluetoothMidiPlayerService.class), connection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (midiService != null) {
			midiService.setReceiver(null);
			unbindService(connection);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case CONNECT:
			if (resultCode == Activity.RESULT_OK) {
				String address = data.getExtras().getString(DeviceListActivity.DEVICE_ADDRESS);
				try {
					midiService.connect(address, new Intent(getApplicationContext(), BluetoothMidiPlayerActivity.class), "Return to MIDI Player");
				} catch (IOException e) {
					toast(e.getMessage());
				}
			}
			break;
		}
	}

	@Override
	public void onDeviceConnected(BluetoothDevice device) {
		startService(serviceIntent());
		toast("Device connected: " + device);
		updateWidgets();
	}

	@Override
	public void onConnectionFailed() {
		toast("Connection failed");
		updateWidgets();
	}

	@Override
	public void onConnectionLost() {
		stopService(serviceIntent());
		toast("Connection terminated");
		updateWidgets();
	}

	@Override
	public void onPlaybackFinished() {
		toast("Playback finished");
		updateWidgets();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.connectButton:
			if (!isConnected()) {
				startActivityForResult(new Intent(BluetoothMidiPlayerActivity.this, DeviceListActivity.class), CONNECT);
			} else {
				midiService.stop();
			}
			break;
		case R.id.playButton:
			if (!midiService.isPlaying()) {
				midiService.startSong();
			} else {
				midiService.pauseSong();
			}
			break;
		case R.id.rewindButton:
			midiService.rewindSong();
			break;
		default:
			break;
		}
		updateWidgets();
	}

	private void updateWidgets() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				boolean connected = isConnected();
				connectButton.setText(connected ? R.string.disconnect : R.string.connect);
				playButton.setEnabled(connected);
				rewindButton.setEnabled(connected);
				playButton.setImageResource(midiService.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
				Uri uri = midiService.getSongUri();
				uriView.setText(uri == null ? "---" : uri.toString());
			}
		});
	}
	
	private boolean isConnected() {
		return midiService.getState() != BluetoothSppConnection.State.NONE;
	}

	private Intent serviceIntent() {
		return new Intent(this, BluetoothMidiPlayerService.class);
	}
}