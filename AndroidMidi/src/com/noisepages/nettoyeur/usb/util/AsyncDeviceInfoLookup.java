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

package com.noisepages.nettoyeur.usb.util;

import android.os.AsyncTask;

import com.noisepages.nettoyeur.usb.UsbDeviceWithInfo;

/**
 * Utility class for looking up human-readable USB devices names asynchronously.
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 */
public abstract class AsyncDeviceInfoLookup extends AsyncTask<UsbDeviceWithInfo, Void, Void> {

  @Override
  protected Void doInBackground(UsbDeviceWithInfo... params) {
    for (UsbDeviceWithInfo device : params) {
      device.retrieveReadableDeviceInfo();
    }
    return null;
  }

  @Override
  protected void onPostExecute(Void result) {
    onLookupComplete();
  }

  /**
   * Invoked when device info for all given devices has been looked up (regardless of whether the
   * lookup was successful).
   */
  protected abstract void onLookupComplete();
}
