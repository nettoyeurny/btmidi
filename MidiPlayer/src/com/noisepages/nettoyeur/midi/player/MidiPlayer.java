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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.noisepages.nettoyeur.bluetooth.BluetoothDisabledException;
import com.noisepages.nettoyeur.bluetooth.BluetoothSppObserver;
import com.noisepages.nettoyeur.bluetooth.BluetoothUnavailableException;
import com.noisepages.nettoyeur.bluetooth.midi.BluetoothMidiDevice;
import com.noisepages.nettoyeur.bluetooth.util.DeviceListActivity;
import com.noisepages.nettoyeur.midi.FromWireConverter;
import com.noisepages.nettoyeur.midi.MidiDevice;
import com.noisepages.nettoyeur.midi.MidiReceiver;
import com.noisepages.nettoyeur.midi.file.InvalidMidiDataException;


/**
 * Simple activity for playing MIDI files over Bluetooth.
 * 
 * @author Peter Brinkmann
 */
public class MidiPlayer extends Activity implements BluetoothSppObserver, OnClickListener {

	private static final int CONNECT = 1;

	private boolean connected = false;
	private MidiDevice midiDevice = null;
	private MidiSequence midiSequence = null;
	private FromWireConverter midiConverter = null;
	private Uri uri = null;
	private Toast toast = null;
	private Button connectButton;
	private ImageButton playButton;
	private ImageButton rewindButton;
	private TextView uriView;

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
		readSequence();
	}

	private void readSequence() {
		uri = getIntent().getData();
		if (uri != null) {
			try {
				InputStream is = getContentResolver().openInputStream(uri);
				midiSequence = new MidiSequence(is) {
					@Override
					protected void onPlaybackFinished() {
						toast("Playback finished");
						updateWidgets();
					}
				};
			} catch (FileNotFoundException e) {
				toast(e.getMessage());
				finish();
			} catch (InvalidMidiDataException e) {
				toast(e.getMessage());
				finish();
			} catch (IOException e) {
				toast(e.getMessage());
				finish();
			}
		} else {
			toast("No URI to read MIDI data from");
			finish();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (midiDevice != null) {
			midiSequence.pause();
			midiDevice.close();
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case CONNECT:
			if (resultCode == Activity.RESULT_OK) {
				connectBluetoothMidi(data.getExtras().getString(DeviceListActivity.DEVICE_ADDRESS));
			}
			break;
		}
	}

	private void connectBluetoothMidi(String address) {
		if (midiDevice != null) {
			midiSequence.pause();
			midiDevice.close();
			midiDevice = null;
			midiConverter = null;
		}
		try {
			BluetoothMidiDevice device;
			device = new BluetoothMidiDevice(this, new MidiReceiver.DummyReceiver());
			device.connect(address);
			midiDevice = device;
			midiConverter = new FromWireConverter(device.getMidiOut());
		} catch (BluetoothUnavailableException e) {
			toast(e.getMessage());
		} catch (BluetoothDisabledException e) {
			toast(e.getMessage());
		} catch (IOException e) {
			toast(e.getMessage());
		}
	}

	@Override
	public void onDeviceConnected(BluetoothDevice device) {
		toast("Device connected: " + device);
		connected = true;
		updateWidgets();
	}

	@Override
	public void onConnectionFailed() {
		toast("Connection failed");
		connected = false;
		updateWidgets();
	}

	@Override
	public void onConnectionLost() {
		toast("Connection terminated");
		connected = false;
		updateWidgets();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.connectButton:
			if (!connected) {
				startActivityForResult(new Intent(MidiPlayer.this, DeviceListActivity.class), CONNECT);
			} else {
				midiSequence.pause();
				midiDevice.close();
				midiDevice = null;
				connected = false;
			}
			break;
		case R.id.playButton:
			if (connected && !midiSequence.isPlaying()) {
				midiSequence.start(midiConverter);
			} else {
				midiSequence.pause();
			}
			break;
		case R.id.rewindButton:
			midiSequence.rewind();
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
				connectButton.setText(connected ? R.string.disconnect : R.string.connect);
				playButton.setEnabled(connected);
				rewindButton.setEnabled(connected);
				playButton.setImageResource(midiSequence.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
				uriView.setText(uri == null ? "---" : uri.toString());
			}
		});
	}
}