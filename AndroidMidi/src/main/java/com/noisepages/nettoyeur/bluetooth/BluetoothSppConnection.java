/*
 * 
 * Modifications Copyright (C) 2011 Peter Brinkmann (peter.brinkmann@gmail.com)
 * 
 * Copyright (C) 2009 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.noisepages.nettoyeur.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.noisepages.nettoyeur.common.RawByteReceiver;


/**
 * This class provides low-level functionality for managing a Bluetooth connection using SPP. It is
 * intended to be used by a class that implements higher-level functionality on top of SPP, such as
 * BluetoothMidiDevice.
 * 
 * @author Peter Brinkmann
 */
public class BluetoothSppConnection {

  private static final String TAG = "BluetoothSppManager";
  private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // UUID
                                                                                                // for
                                                                                                // SPP;
                                                                                                // don't
                                                                                                // change
                                                                                                // this.

  public static enum State {
    NONE, CONNECTING, CONNECTED
  }

  private final BluetoothAdapter btAdapter;
  private final BluetoothSppObserver sppObserver;
  private final RawByteReceiver sppReceiver;
  private final int bufferSize;
  private volatile State connectionState = State.NONE;
  private ConnectThread connectThread = null;
  private ConnectedThread connectedThread = null;

  /**
   * Constructor.
   * 
   * @param observer handling Bluetooth-related events
   * @param receiver handling incoming data from Bluetooth
   * @param bufferSize buffer size for the input stream
   * @throws BluetoothUnavailableException
   * @throws BluetoothDisabledException
   */
  public BluetoothSppConnection(BluetoothSppObserver observer, RawByteReceiver receiver,
      int bufferSize) throws BluetoothUnavailableException, BluetoothDisabledException {
    btAdapter = BluetoothAdapter.getDefaultAdapter();
    if (btAdapter == null) {
      throw new BluetoothUnavailableException();
    }
    if (!btAdapter.isEnabled()) {
      throw new BluetoothDisabledException();
    }
    this.sppObserver = observer;
    this.sppReceiver = receiver;
    this.bufferSize = bufferSize;
  }

  /**
   * @return current connection state
   */
  public State getConnectionState() {
    return connectionState;
  }

  /**
   * Stop all threads and close SPP connection.
   */
  public synchronized void stop() {
    cancelThreads();
    setState(State.NONE);
  }

  /**
   * Connect to a Bluetooth device with the given MAC address, using the standard UUID for SPP.
   * 
   * @param addr string representation of MAC address of the Bluetooth device
   * @throws IOException
   */
  public void connect(String addr) throws IOException {
    connect(addr, SPP_UUID);
  }

  /**
   * Connect to a Bluetooth device with the given address and UUID.
   * 
   * @param addr string representation of MAC address of the Bluetooth device
   * @param uuid UUID of the Bluetooth device
   * @throws IOException
   */
  public synchronized void connect(String addr, UUID uuid) throws IOException {
    cancelThreads();
    BluetoothDevice device = btAdapter.getRemoteDevice(addr);
    connectThread = new ConnectThread(device, uuid);
    connectThread.start();
    setState(State.CONNECTING);
  }

  /**
   * Write bytes to the output stream of the SPP connection.
   * 
   * @param out buffer containing the bytes to be sent to the Bluetooth device
   * @param offset index of first byte to be sent
   * @param count number of bytes to be sent
   * @throws IOException
   */
  public void write(byte[] out, int offset, int count) throws IOException {
    ConnectedThread thread;
    synchronized (this) {
      if (connectionState != State.CONNECTED) {
        throw new BluetoothNotConnectedException();
      }
      thread = connectedThread;
    }
    thread.write(out, offset, count);
  }

  private synchronized void connected(BluetoothSocket socket, BluetoothDevice device)
      throws IOException {
    connectThread = null;
    cancelConnectedThread();
    connectedThread = new ConnectedThread(socket);
    connectedThread.start();
    sppObserver.onDeviceConnected(device);
    setState(State.CONNECTED);
  }

  private void connectionFailed() {
    setState(State.NONE);
    sppObserver.onConnectionFailed();
  }

  private void connectionLost() {
    setState(State.NONE);
    sppObserver.onConnectionLost();
  }

  private void cancelThreads() {
    if (connectThread != null) {
      connectThread.cancel();
      connectThread = null;
    }
    cancelConnectedThread();
  }

  private void cancelConnectedThread() {
    if (connectedThread != null) {
      connectedThread.cancel();
      connectedThread = null;
    }
  }

  private void setState(State state) {
    connectionState = state;
  }

  private class ConnectThread extends Thread {
    private final BluetoothSocket socket;
    private final BluetoothDevice device;

    private ConnectThread(BluetoothDevice device, UUID uuid) throws IOException {
      this.device = device;
      this.socket = device.createRfcommSocketToServiceRecord(uuid);
    }

    @Override
    public void run() {
      try {
        socket.connect();
        connected(socket, device);
      } catch (IOException e1) {
        connectionFailed();
        try {
          socket.close();
        } catch (IOException e2) {
          Log.e(TAG, "Unable to close socket after connection failure", e2);
        }
      }
    }

    private void cancel() {
      try {
        socket.close();
      } catch (IOException e) {
        Log.e(TAG, "Unable to close socket", e);
      }
    }
  }

  private class ConnectedThread extends Thread {
    private final BluetoothSocket socket;
    private final InputStream inStream;
    private final OutputStream outStream;

    private ConnectedThread(BluetoothSocket socket) throws IOException {
      this.socket = socket;
      inStream = socket.getInputStream();
      outStream = socket.getOutputStream();
    }

    @Override
    public void run() {
      byte[] buffer = new byte[bufferSize];
      int nBytes;
      while (true) {
        try {
          nBytes = inStream.read(buffer);
          sppReceiver.onBytesReceived(nBytes, buffer);
        } catch (IOException e) {
          connectionLost();
          break;
        }
      }
    }

    private void write(byte[] buffer, int offset, int count) throws IOException {
      outStream.write(buffer, offset, count);
    }

    private void cancel() {
      try {
        socket.close();
      } catch (IOException e) {
        Log.e(TAG, "Unable to close socket", e);
      }
    }
  }
}
