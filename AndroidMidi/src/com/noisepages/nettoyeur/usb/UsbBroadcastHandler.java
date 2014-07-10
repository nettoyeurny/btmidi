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

package com.noisepages.nettoyeur.usb;

import android.hardware.usb.UsbDevice;

/**
 * Interface for handling USB-related broadcasts.
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 */
public interface UsbBroadcastHandler {

  /**
   * Called when a request for USB permission has been granted.
   * 
   * @param device for which permission has been granted
   */
  public void onPermissionGranted(UsbDevice device);

  /**
   * Called when a request for USB permission has been denied.
   * 
   * @param device for which permission has been denied
   */
  public void onPermissionDenied(UsbDevice device);

  /**
   * Called when a USB device has been detached.
   * 
   * @param device that has been detached
   */
  public void onDeviceDetached(UsbDevice device);
}
