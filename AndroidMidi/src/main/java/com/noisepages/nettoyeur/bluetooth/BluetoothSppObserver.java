/*
 * Copyright (C) 2011 Peter Brinkmann (peter.brinkmann@gmail.com)
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

import android.bluetooth.BluetoothDevice;

/**
 * Callbacks for monitoring the state of a Bluetooth SPP connection.
 * 
 * @author Peter Brinkmann
 */
public interface BluetoothSppObserver {

  /**
   * Called when a connection succeeds.
   * 
   * @param device the connected device
   */
  void onDeviceConnected(BluetoothDevice device);

  /**
   * Called when a connection fails.
   */
  void onConnectionFailed();

  /**
   * Called when a connection is lost or closed.
   */
  void onConnectionLost();

}
