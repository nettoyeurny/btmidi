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

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;

/**
 * Wrapper for {@link UsbDevice} that also holds a {@link DeviceInfo} object, plus a few convenience
 * methods for handling USB permissions.
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class UsbDeviceWithInfo {

  private static final String ACTION_USB_PERMISSION = "com.noisepages.nettoyeur.usb.USB_PERMISSION";

  private static BroadcastReceiver broadcastReceiver = null;

  protected final UsbDevice device;
  private volatile DeviceInfo info;
  private volatile boolean hasReadableInfo = false;

  /**
   * Convenience method for handling responses to USB permission requests, to be called in the
   * onCreate method of activities that connect to USB devices. Activities that use call this method
   * also have to uninstall the permission handler in their onDestroy method (see
   * uninstallPermissionHandler).
   * 
   * @param context the current context, e.g., the activity invoking this method
   * @param handler
   */
  public static void installBroadcastHandler(Context context, final UsbBroadcastHandler handler) {
    uninstallBroadcastHandler(context);
    broadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device == null) return;
        if (ACTION_USB_PERMISSION.equals(action)) {
          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            handler.onPermissionGranted(device);
          } else {
            handler.onPermissionDenied(device);
          }
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
          handler.onDeviceDetached(device);
        }
      }
    };
    context.registerReceiver(broadcastReceiver, new IntentFilter(ACTION_USB_PERMISSION));
    context.registerReceiver(broadcastReceiver, new IntentFilter(
        UsbManager.ACTION_USB_DEVICE_DETACHED));
  }

  /**
   * Uninstalls the permission handler; must be called in the onDestroy method of activities that
   * install a permission handler.
   * 
   * @param context the current context, e.g., the activity invoking this method
   */
  public static void uninstallBroadcastHandler(Context context) {
    if (broadcastReceiver != null) {
      try {
        context.unregisterReceiver(broadcastReceiver);
      } catch (IllegalArgumentException e) {
        // Do nothing. This exception is entirely benign; it'll happen if we try to remove a
        // receiver that
        // isn't there, which is a reasonable thing to do in some situations.
      }
      broadcastReceiver = null;
    }
  }

  /**
   * Convenience method for requesting permission to use the USB device; may only be called if a
   * permission handler is installed.
   * 
   * @param context the current context, e.g., the activity invoking this method
   */
  public void requestPermission(Context context) {
    if (broadcastReceiver == null) {
      throw new IllegalStateException(
          "installPermissionHandler must be called before requesting permission");
    }
    ((UsbManager) context.getSystemService(Context.USB_SERVICE)).requestPermission(device,
        PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0));
  }

  /**
   * Constructor.
   * 
   * @param device to be wrapped
   */
  public UsbDeviceWithInfo(UsbDevice device) {
    this.device = device;
    info = new DeviceInfo(device);
  }

  /**
   * Checks whether a given USB device equals the device wrapped by this instance; useful for
   * checking whether {@link UsbBroadcastHandler} callbacks are for a particular instance of
   * UsbDeviceWithInfo.
   * 
   * @param otherDevice to compare this device to
   * @return true if otherDevice equals the USB device wrapped by this instance
   */
  public boolean matches(UsbDevice otherDevice) {
    return this.device.equals(otherDevice);
  }

  /**
   * Note: The return value may change over the lifetime of this object. By default, it is populated
   * with numerical information from the underlying UsbDevice object, but if
   * retrieveReadableDeviceInfo is invoked, then it may be replaced with human readable data
   * retrieved from the web.
   * 
   * @return the current device info
   */
  public DeviceInfo getCurrentDeviceInfo() {
    return info;
  }

  /**
   * Attempts to replace the default device info with human readable device info from the web; must
   * not be called on the main thread as it performs an online lookup and may cause the app to
   * become unresponsive.
   * 
   * Requires android.permission.INTERNET.
   * 
   * @return true on success
   */
  public boolean retrieveReadableDeviceInfo() {
    if (hasReadableInfo) return true;
    DeviceInfo readableInfo = DeviceInfo.retrieveDeviceInfo(device);
    if (readableInfo != null) {
      info = readableInfo;
      hasReadableInfo = true;
    }
    return hasReadableInfo;
  }

  @Override
  public String toString() {
    return device.toString();
  }
}
