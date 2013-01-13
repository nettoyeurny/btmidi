/*
 * Copyright (C) 2013 Peter Brinkmann (peter.brinkmann@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.noisepages.nettoyeur.usb;

import android.hardware.usb.UsbDevice;

/**
 * Callback for asynchronous retrieval of device information.
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 */
public interface DeviceInfoCallback {

	/**
	 * Callback invoked on conclusion of retrieval of device information.
	 * 
	 * @param device for which to retrieve information
	 * @param info for the given device
	 */
	public void onDeviceInfo(UsbDevice device, DeviceInfo info);

	/**
	 * Callback invoked when no device info was found.
	 * 
	 * @param device for which to retrieve information
	 */
	public void onFailure(UsbDevice device);
}
