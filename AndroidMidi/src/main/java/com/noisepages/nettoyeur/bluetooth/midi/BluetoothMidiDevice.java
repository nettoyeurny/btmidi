/*
 * Copyright (C) 2013 Peter Brinkmann (peter.brinkmann@gmail.com)
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

package com.noisepages.nettoyeur.bluetooth.midi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.noisepages.nettoyeur.bluetooth.BluetoothDisabledException;
import com.noisepages.nettoyeur.bluetooth.BluetoothSppConnection;
import com.noisepages.nettoyeur.bluetooth.BluetoothSppObserver;
import com.noisepages.nettoyeur.bluetooth.BluetoothUnavailableException;
import com.noisepages.nettoyeur.common.RawByteReceiver;
import com.noisepages.nettoyeur.midi.FromWireConverter;
import com.noisepages.nettoyeur.midi.MidiDevice;
import com.noisepages.nettoyeur.midi.MidiReceiver;
import com.noisepages.nettoyeur.midi.ToWireConverter;


/**
 * Basic support for MIDI In/Out over Bluetooth.
 * 
 * @author Peter Brinkmann
 */
public class BluetoothMidiDevice implements MidiDevice {

  private final BluetoothSppConnection btConnection;
  private final ToWireConverter toWire = new ToWireConverter(new RawByteReceiver() {
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private boolean inBlock = false;

    @Override
    public void onBytesReceived(int nBytes, byte[] buffer) {
      if (inBlock) {
        outputStream.write(buffer, 0, nBytes);
      } else {
        try {
          btConnection.write(buffer, 0, nBytes);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    @Override
    public boolean beginBlock() {
      outputStream.reset();
      inBlock = true;
      return true;
    }

    @Override
    public void endBlock() {
      if (inBlock) {
        try {
          btConnection.write(outputStream.toByteArray(), 0, outputStream.size());
        } catch (IOException e) {
          e.printStackTrace();
        }
        inBlock = false;
      } else {
        throw new IllegalStateException("Not in block mode");
      }
    }
  });

  /**
   * Constructor.
   * 
   * @param observer for handling Bluetooth connection events
   * @param receiver for handling events from the Bluetooth input stream
   * @throws BluetoothDisabledException
   * @throws BluetoothUnavailableException
   */
  public BluetoothMidiDevice(BluetoothSppObserver observer, MidiReceiver receiver)
      throws BluetoothUnavailableException, BluetoothDisabledException {
    btConnection = new BluetoothSppConnection(observer, new FromWireConverter(receiver), 64);
  }

  /**
   * Attempts to connect to the given Bluetooth device.
   * 
   * @param addr String representation of the MAC address of the Bluetooth device
   * @throws IOException
   */
  public void connect(String addr) throws IOException {
    btConnection.connect(addr);
  }

  /**
   * Stops all Bluetooth threads and closes the Bluetooth connection.
   */
  @Override
  public void close() {
    btConnection.stop();
  }

  /**
   * @return the MIDI receiver that sends messages to the Bluetooth output stream
   */
  public MidiReceiver getMidiOut() {
    return toWire;
  }

  /**
   * @return the state of the underlying Bluetooth connection
   */
  public BluetoothSppConnection.State getConnectionState() {
    return btConnection.getConnectionState();
  }
}
