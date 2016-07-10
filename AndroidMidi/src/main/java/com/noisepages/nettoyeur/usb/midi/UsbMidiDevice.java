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

package com.noisepages.nettoyeur.usb.midi;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import com.noisepages.nettoyeur.common.RawByteReceiver;
import com.noisepages.nettoyeur.midi.FromWireConverter;
import com.noisepages.nettoyeur.midi.MidiDevice;
import com.noisepages.nettoyeur.midi.MidiReceiver;
import com.noisepages.nettoyeur.midi.ToWireConverter;
import com.noisepages.nettoyeur.usb.ConnectionFailedException;
import com.noisepages.nettoyeur.usb.DeviceNotConnectedException;
import com.noisepages.nettoyeur.usb.InterfaceNotAvailableException;
import com.noisepages.nettoyeur.usb.UsbDeviceWithInfo;

/**
 * MIDI-specific wrapper for USB devices. Instances of this class will implicitly manage interfaces
 * and USB endpoints. MIDI connections (both input and output) go through the {@link MidiReceiver}
 * interface. MIDI inputs take receiver instances that they'll invoke on incoming events; MIDI
 * outputs provide receivers through which client code can send out MIDI events.
 * 
 * Note: Activities that use this class need to specify android:launchMode="singleTask" in their
 * manifest to make sure that the USB connection won't be lost on restart.
 * 
 * Acknowledgment: The implementation of the USB driver was done from scratch, but the idea of
 * writing a soft driver in Java came from Kaoru Shoji's USB-MIDI-Driver
 * (https://github.com/kshoji/USB-MIDI-Driver).
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 */
@TargetApi(12)
public class UsbMidiDevice extends UsbDeviceWithInfo implements MidiDevice {

  // USB payload size by Code Index Number.
  private static final int[] midiPayloadSize = new int[] {
  /* 0x00 */-1, /* 0x01 */-1, // Reserved for future extensions; currently unused
      /* 0x02 */2, /* 0x03 */3, // System common
      /* 0x04 */3, /* 0x05 */1, /* 0x06 */2, /* 0x07 */3, // System exclusive
      /* 0x08 */3, /* 0x09 */3, /* 0x0a */3, /* 0x0b */3, /* 0x0c */2, /* 0x0d */2, /* 0x0e */3, // Channel
                                                                                                 // messages
      /* 0x0f */1 // MIDI byte
      };

  private final List<UsbMidiInterface> interfaces = new ArrayList<UsbMidiDevice.UsbMidiInterface>();
  private UsbDeviceConnection connection = null;

  /**
   * MIDI-specific wrapper for USB interfaces within a USB devices. This class doesn't do much and
   * mostly serves to organize inputs and outputs.
   */
  public class UsbMidiInterface {
    private final UsbInterface iface;
    private final List<UsbMidiInput> inputs;
    private final List<UsbMidiOutput> outputs;

    private UsbMidiInterface(UsbInterface iface, List<UsbMidiInput> inputs,
        List<UsbMidiOutput> outputs) {
      this.iface = iface;
      this.inputs = inputs;
      this.outputs = outputs;
    }

    private UsbInterface getInterface() {
      return iface;
    }

    public String toString() {
      return iface.toString();
    }

    /**
     * @return an unmodifiable list of MIDI inputs belonging to this interface
     */
    public List<UsbMidiInput> getInputs() {
      return Collections.unmodifiableList(inputs);
    }

    /**
     * @return an unmodifiable list of MIDI outputs belonging to this interface
     */
    public List<UsbMidiOutput> getOutputs() {
      return Collections.unmodifiableList(outputs);
    }

    /**
     * Stops listening threads on all MIDI inputs belonging to this interface.
     */
    public void stop() {
      for (UsbMidiInput input : inputs) {
        input.stop();
      }
    }
  }

  /**
   * Wrapper for USB MIDI input endpoints.
   */
  public class UsbMidiInput {
    private final UsbInterface iface;
    private final UsbEndpoint inputEndpoint;
    private final ConcurrentMap<Integer, FromWireConverter> converters =
        new ConcurrentHashMap<Integer, FromWireConverter>();
    private volatile Thread inputThread = null;

    private UsbMidiInput(UsbInterface iface, UsbEndpoint endpoint) {
      this.iface = iface;
      inputEndpoint = endpoint;
    }

    @Override
    public String toString() {
      return "in:" + inputEndpoint;
    }

    /**
     * Sets the receiver for incoming MIDI events on all virtual cables.
     * 
     * @param receiver MIDI receiver for all cables; may be null
     */
    public void setReceiver(MidiReceiver receiver) {
      setReceiverInternal(-1, receiver);
    }

    /**
     * Sets the receiver for a given virtual cable.
     * 
     * @param cable ranging from 0x00 to 0x0f
     * @param receiver MIDI receiver for the given cable; may be null
     */
    public void setReceiver(int cable, MidiReceiver receiver) {
      if (cable < 0x00 || cable > 0x0f) {
        throw new IllegalArgumentException("Cable number out of range");
      }
      setReceiverInternal(cable, receiver);
    }

    private void setReceiverInternal(int cable, MidiReceiver receiver) {
      if (receiver != null) {
        converters.put(cable, new FromWireConverter(receiver));
      } else {
        converters.remove(cable);
      }
    }

    /**
     * Starts listening to this MIDI input.
     * 
     * @throws DeviceNotConnectedException if the MIDI device is not connected
     * @throws InterfaceNotAvailableException if the corresponding interface is not available
     */
    public void start() throws DeviceNotConnectedException, InterfaceNotAvailableException {
      if (connection == null) {
        throw new DeviceNotConnectedException();
      }
      stop();
      if (!connection.claimInterface(iface, true)) {
        throw new InterfaceNotAvailableException();
      }
      inputThread = new Thread() {
        private final byte[] inputBuffer = new byte[inputEndpoint.getMaxPacketSize()];
        private final byte[] tmpBuffer = new byte[3];

        @Override
        public void run() {
          while (!interrupted()) {
            int nRead = connection.bulkTransfer(inputEndpoint, inputBuffer, inputBuffer.length, 50);
            for (int i = 0; i < nRead; i += 4) {
              int b = inputBuffer[i];
              int cable = (b >> 4) & 0x0f;
              int n = midiPayloadSize[b & 0x0f];
              if (n < 0) continue;
              for (int j = 0; j < n; ++j) {
                tmpBuffer[j] = inputBuffer[i + j + 1];
              }
              convertBytes(converters.get(-1), n); // Call converter for all cables, if any.
              convertBytes(converters.get(cable), n);
            }
          }
        }

        private void convertBytes(FromWireConverter converter, int n) {
          if (converter != null) {
            converter.onBytesReceived(n, tmpBuffer);
          }
        }
      };
      inputThread.start();
    }

    /**
     * Stops listening to this input.
     */
    public void stop() {
      if (inputThread != null) {
        inputThread.interrupt();
        try {
          inputThread.join();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt(); // Preserve interrupt flag in case the caller needs
                                              // it.
        }
        inputThread = null;
      }
    }
  }

  /**
   * Wrapper for USB MIDI output endpoints.
   */
  public class UsbMidiOutput {
    private final UsbInterface iface;
    private final UsbEndpoint outputEndpoint;
    private final byte[] outBuffer;
    private volatile int cable;

    private final ToWireConverter toWire = new ToWireConverter(new RawByteReceiver() {
      private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      private boolean inBlock = false;
      int writeIndex = 0;

      @Override
      public synchronized void onBytesReceived(int nBytes, byte[] buffer) {
        if (connection == null) return;
        for (int start = 0, end; start < nBytes; start = end) {
          for (end = start + 1; end < nBytes && (buffer[end] == (byte) 0xf7 || buffer[end] >= 0); ++end);
          processChunk(buffer, start, end);
        }
        transfer();
      }

      private void processChunk(byte[] buffer, int start, int end) {
        int cin = (buffer[start] >> 4) & 0x0f;
        if (cin >= 0x08 && cin < 0x0f && end - start == midiPayloadSize[cin]) {
          // The most common case: Correctly formed MIDI channel message.
          outBuffer[writeIndex++] = (byte) (cable | cin);
          while (start < end) {
            outBuffer[writeIndex++] = buffer[start++];
          }
          while ((writeIndex & 0x03) != 0) {
            outBuffer[writeIndex++] = 0;
          }
          transferIfFull();
        } else {
          // No channel message? Just dump single bytes.
          while (start < end) {
            outBuffer[writeIndex++] = (byte) (cable | 0x0f);
            outBuffer[writeIndex++] = buffer[start++];
            outBuffer[writeIndex++] = 0;
            outBuffer[writeIndex++] = 0;
            transferIfFull();
          }
        }
      }

      private void transferIfFull() {
        if (writeIndex >= outBuffer.length) {
          transfer();
        }
      }

      private void transfer() {
        if (inBlock) {
          outputStream.write(outBuffer, 0, writeIndex);
        } else {
          connection.bulkTransfer(outputEndpoint, outBuffer, writeIndex, 0);
        }
        writeIndex = 0;
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
          connection.bulkTransfer(outputEndpoint, outputStream.toByteArray(), outputStream.size(),
              0);
        } else {
          throw new IllegalStateException("Not in block mode");
        }
      }
    });

    private UsbMidiOutput(UsbInterface iface, UsbEndpoint ep) {
      this.iface = iface;
      outputEndpoint = ep;
      outBuffer = new byte[ep.getMaxPacketSize()];
      setVirtualCable(0);
    }

    @Override
    public String toString() {
      return "out:" + outputEndpoint;
    }

    /**
     * Sets the virtual cable to write to; the default is 0.
     * 
     * @param c virtual cable number
     */
    public void setVirtualCable(int c) {
      cable = (c << 4) & 0xf0;
    }

    /**
     * Returns a MidiReceiver instance associated with this endpoint. Requires that the enclosing
     * USB MIDI device be connected.
     * 
     * @return MidiReceiver instance to write MIDI events to
     * @throws DeviceNotConnectedException if the USB MIDI device is not connected
     * @throws InterfaceNotAvailableException
     */
    public MidiReceiver getMidiOut() throws DeviceNotConnectedException,
        InterfaceNotAvailableException {
      if (connection == null) {
        throw new DeviceNotConnectedException();
      }
      if (!connection.claimInterface(iface, true)) {
        throw new InterfaceNotAvailableException();
      }
      return toWire;
    }
  }

  /**
   * Scans the currently attached USB devices and returns those the look like MIDI devices. Note
   * that there may be false positives since this method will list all devices with endpoints that
   * look like MIDI endpoints. While this behavior is potentially awkward, it is preferable to the
   * wholesale suppression of devices that are MIDI devices but fail to properly identify themselves
   * as such (sadly, this is a common problem).
   * 
   * @param context the current context, e.g., the activity invoking this method
   * @return list of (probable) MIDI devices
   */
  public static List<UsbMidiDevice> getMidiDevices(Context context) {
    List<UsbMidiDevice> midiDevices = new ArrayList<UsbMidiDevice>();
    UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    for (UsbDevice device : manager.getDeviceList().values()) {
      UsbMidiDevice midiDevice = new UsbMidiDevice(device);
      if (!midiDevice.getInterfaces().isEmpty()) {
        midiDevices.add(midiDevice);
      }
    }
    return midiDevices;
  }

  private UsbMidiInterface asMidiInterface(UsbInterface iface) {
    // We really ought to check interface class and subclass, but we don't since a lot of MIDI
    // devices don't fully comply with the standard.
    // if (iface.getInterfaceClass() != 1 || iface.getInterfaceSubclass() != 3) return null;
    List<UsbMidiInput> inputs = new ArrayList<UsbMidiInput>();
    List<UsbMidiOutput> outputs = new ArrayList<UsbMidiOutput>();
    int epCount = iface.getEndpointCount();
    for (int j = 0; j < epCount; ++j) {
      UsbEndpoint ep = iface.getEndpoint(j);
      // If the endpoint looks like a MIDI endpoint, assume that it is one. My reading of the USB
      // MIDI specification is that the max
      // packet size ought to be 0x40, but I've seen at least one interface (Focusrite Scarlett 2i4)
      // that reports a max packet size
      // of 0x200. So, we'll just check the minimum requirement for the rest of this class to work,
      // i.e., the max packet size must be
      // a positive multiple of 4.
      if ((ep.getType() & UsbConstants.USB_ENDPOINT_XFERTYPE_MASK) == UsbConstants.USB_ENDPOINT_XFER_BULK
          && (ep.getMaxPacketSize() & 0x03) == 0 && ep.getMaxPacketSize() > 0) {
        if ((ep.getDirection() & UsbConstants.USB_ENDPOINT_DIR_MASK) == UsbConstants.USB_DIR_IN) {
          inputs.add(new UsbMidiInput(iface, ep));
        } else {
          outputs.add(new UsbMidiOutput(iface, ep));
        }
      }
    }
    return (inputs.isEmpty() && outputs.isEmpty()) ? null : new UsbMidiInterface(iface, inputs,
        outputs);
  }

  /**
   * Wraps a given USB device as a MIDI device if possible. If the device has no USB endpoints that
   * look like MIDI endpoints, the result will be null. Otherwise, we'll just assume that the device
   * is a MIDI device.
   * 
   * @param device to be wrapped
   * @return MIDI device if possible, null otherwise
   */
  public static UsbMidiDevice asMidiDevice(UsbDevice device) {
    UsbMidiDevice midiDevice = new UsbMidiDevice(device);
    return (!midiDevice.getInterfaces().isEmpty()) ? midiDevice : null;
  }

  private UsbMidiDevice(UsbDevice device) {
    super(device);
    int ifaceCount = device.getInterfaceCount();
    for (int i = 0; i < ifaceCount; ++i) {
      UsbMidiInterface iface = asMidiInterface(device.getInterface(i));
      if (iface != null) {
        interfaces.add(iface);
      }
    }
  }

  /**
   * @return an unmodifiable list of MIDI interfaces belonging to this device.
   */
  public List<UsbMidiInterface> getInterfaces() {
    return Collections.unmodifiableList(interfaces);
  }

  /**
   * Opens a USB connection to this device.
   * 
   * @param context the current context, e.g., the activity invoking this method
   * @throws ConnectionFailedException
   */
  public synchronized void open(Context context) throws ConnectionFailedException {
    close();
    UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    connection = manager.openDevice(device);
    if (connection == null) {
      throw new ConnectionFailedException();
    }
  }

  /**
   * Stops listening on all inputs and closes the current USB connection, if any.
   */
  @Override
  public synchronized void close() {
    if (connection == null) return;
    for (UsbMidiInterface iface : interfaces) {
      iface.stop();
      connection.releaseInterface(iface.getInterface());
    }
    connection.close();
    connection = null;
  }

  /**
   * @return true if and only if the device currently has a USB connection
   */
  public synchronized boolean isConnected() {
    return connection != null;
  }
}
