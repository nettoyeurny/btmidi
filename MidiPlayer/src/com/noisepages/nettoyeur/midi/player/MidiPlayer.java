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
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Build;
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
import com.noisepages.nettoyeur.usb.ConnectionFailedException;
import com.noisepages.nettoyeur.usb.DeviceNotConnectedException;
import com.noisepages.nettoyeur.usb.InterfaceNotAvailableException;
import com.noisepages.nettoyeur.usb.UsbBroadcastHandler;
import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice;
import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice.UsbMidiOutput;
import com.noisepages.nettoyeur.usb.midi.util.UsbMidiOutputSelector;
import com.noisepages.nettoyeur.usb.util.AsyncDeviceInfoLookup;
import com.noisepages.nettoyeur.usb.util.UsbDeviceSelector;


/**
 * Simple activity for playing MIDI files over Bluetooth or USB.
 * 
 * @author Peter Brinkmann
 */
public class MidiPlayer extends Activity implements BluetoothSppObserver, OnClickListener {

	private static final int CONNECT = 1;

	private enum ConnectionType {
		NONE,
		BLUETOOTH,
		USB
	};
	
	private ConnectionType connectionType = ConnectionType.NONE;
	private MidiDevice midiDevice = null;
	private MidiSequence midiSequence = null;
	private FromWireConverter midiConverter = null;
	private Uri uri = null;
	private Toast toast = null;
	private Button connectBluetoothButton;
	private Button connectUsbButton;
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

	private boolean usbAvailable() {
		return Build.VERSION.SDK_INT >= 12;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (usbAvailable()) {
			setContentView(R.layout.main_usb);
			connectUsbButton = (Button) findViewById(R.id.connectUsbButton);
			connectUsbButton.setOnClickListener(this);
			installBroadcastHandler();
		} else {
			setContentView(R.layout.main);
		}
		connectBluetoothButton = (Button) findViewById(R.id.connectButton);
		playButton = (ImageButton) findViewById(R.id.playButton);
		rewindButton = (ImageButton) findViewById(R.id.rewindButton);
		uriView = (TextView) findViewById(R.id.uriView);
		connectBluetoothButton.setOnClickListener(this);
		playButton.setOnClickListener(this);
		rewindButton.setOnClickListener(this);
		uriView.setText(R.string.loading);
		readSequence();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	private void installBroadcastHandler() {
		UsbMidiDevice.installBroadcastHandler(this, new UsbBroadcastHandler() {
			@Override
			public void onPermissionGranted(UsbDevice device) {
				try {
					((UsbMidiDevice) midiDevice).open(MidiPlayer.this);
				} catch (ConnectionFailedException e) {
					toast("Connection failed");
					return;
				}
				new UsbMidiOutputSelector((UsbMidiDevice) midiDevice) {
					@Override
					protected void onOutputSelected(UsbMidiOutput output, UsbMidiDevice device, int iface, int index) {
						toast("Output selection: Interface " + iface + ", Output " + index);
						try {
							midiConverter = new FromWireConverter(output.getMidiOut());
							connectionType = ConnectionType.USB;
						} catch (DeviceNotConnectedException e) {
							midiDevice = null;
							toast("Device not connected");
						} catch (InterfaceNotAvailableException e) {
							midiDevice = null;
							toast("Interface not available");
						}
						updateWidgets();
					}
					
					@Override
					protected void onNoSelection(UsbMidiDevice device) {
						toast("No output selected");
					}
				}.show(getFragmentManager(), null);
			}

			@Override
			public void onPermissionDenied(UsbDevice device) {
				toast("USB permission denied");
			}

			@Override
			public void onDeviceDetached(UsbDevice device) {
				if (connectionType == ConnectionType.USB) {
					midiSequence.pause();
					midiDevice.close();
					midiDevice = null;
					midiConverter = null;
					connectionType = ConnectionType.NONE;
				}
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		updateWidgets();
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
		if (usbAvailable()) {
			UsbMidiDevice.uninstallBroadcastHandler(this);
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
			connectionType = ConnectionType.NONE;
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

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void connectUsbDevice() {
		if (midiDevice != null) {
			midiSequence.pause();
			midiDevice.close();
			midiDevice = null;
			connectionType = ConnectionType.NONE;
		}
		final List<UsbMidiDevice> devices = UsbMidiDevice.getMidiDevices(this);
		new AsyncDeviceInfoLookup() {
			@Override
			protected void onLookupComplete() {
				new UsbDeviceSelector<UsbMidiDevice>(devices) {
					
					@Override
					protected void onDeviceSelected(UsbMidiDevice device) {
						midiDevice = device;
						device.requestPermission(MidiPlayer.this);
					}
					
					@Override
					protected void onNoSelection() {
						toast("No device selected");
					}
				}.show(getFragmentManager(), null);
			}
		}.execute(devices.toArray(new UsbMidiDevice[devices.size()]));
	}

	@Override
	public void onDeviceConnected(BluetoothDevice device) {
		toast("Device connected: " + device);
		connectionType = ConnectionType.BLUETOOTH;
		updateWidgets();
	}

	@Override
	public void onConnectionFailed() {
		toast("Connection failed");
		connectionType = ConnectionType.NONE;
		updateWidgets();
	}

	@Override
	public void onConnectionLost() {
		toast("Connection terminated");
		connectionType = ConnectionType.NONE;
		updateWidgets();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.connectButton:
			if (connectionType != ConnectionType.BLUETOOTH) {
				startActivityForResult(new Intent(MidiPlayer.this, DeviceListActivity.class), CONNECT);
			} else {
				midiSequence.pause();
				midiDevice.close();
				midiDevice = null;
				connectionType = ConnectionType.NONE;
			}
			break;
		case R.id.connectUsbButton:
			if (connectionType != ConnectionType.USB) {
				connectUsbDevice();
			} else {
				midiSequence.pause();
				midiDevice.close();
				midiDevice = null;
				connectionType = ConnectionType.NONE;
			}
			break;
		case R.id.playButton:
			if (connectionType != ConnectionType.NONE && !midiSequence.isPlaying()) {
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
				connectBluetoothButton.setText(connectionType == ConnectionType.BLUETOOTH ? R.string.disconnect : R.string.connect);
				if (connectUsbButton != null) {
					connectUsbButton.setText(connectionType == ConnectionType.USB ? R.string.disconnect_usb : R.string.connect_usb);
				}
				playButton.setEnabled(connectionType != ConnectionType.NONE);
				rewindButton.setEnabled(connectionType != ConnectionType.NONE);
				playButton.setImageResource(midiSequence.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
				uriView.setText(uri == null ? "---" : uri.toString());
			}
		});
	}
}