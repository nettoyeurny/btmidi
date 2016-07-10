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

package com.noisepages.nettoyeur.midi;

import com.noisepages.nettoyeur.midi.util.SystemMessageDecoder;


/**
 * Callbacks for handling MIDI events and connection changes. Note that this interface chooses
 * sanity over compliance with the MIDI standard, i.e., channel numbers start at 0, and pitch bend
 * values are centered at 0.
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 */
public interface MidiReceiver {

  /**
   * Handles note off events.
   * 
   * @param channel starting at 0
   * @param key
   * @param velocity
   */
  void onNoteOff(int channel, int key, int velocity);

  /**
   * Handles note on events.
   * 
   * @param channel starting at 0
   * @param key
   * @param velocity
   */
  void onNoteOn(int channel, int key, int velocity);

  /**
   * Handles polyphonic aftertouch events.
   * 
   * @param channel starting at 0
   * @param key
   * @param velocity
   */
  void onPolyAftertouch(int channel, int key, int velocity);

  /**
   * Handles a control change message.
   * 
   * @param channel starting at 0
   * @param controller
   * @param value
   */
  void onControlChange(int channel, int controller, int value);

  /**
   * Handles a program change message.
   * 
   * @param channel starting at 0
   * @param program
   */
  void onProgramChange(int channel, int program);

  /**
   * Handles a channel aftertouch event.
   * 
   * @param channel starting at 0
   * @param velocity
   */
  void onAftertouch(int channel, int velocity);

  /**
   * Handles a pitch bend event.
   * 
   * @param channel starting at 0
   * @param value centered at 0, ranging from -8192 to 8191
   */
  void onPitchBend(int channel, int value);

  /**
   * Handles a raw MIDI byte; a raw byte is a byte that is received from the Bluetooth device but
   * doesn't belong to any of the channel messages that are explicitly handled by this interface.
   * 
   * The idea is that we don't want to burden this basic interface with support for obscure system
   * messages, but at the same time we don't want to hide any MIDI events from client code. If you
   * need system messages, you can handle them by calling the decodeByte method of
   * {@link SystemMessageDecoder} from here.
   * 
   * @param value raw MIDI byte
   */
  void onRawByte(byte value);

  /**
   * Begin assembling subsequent MIDI messages into one buffer. This is an optional optimization
   * that allows wire format converters to reduce the number of buffers they need to send, and it
   * provides a hint that several messages are supposed to occur at the same time.
   * 
   * @return true if block mode is supported
   */
  boolean beginBlock();

  /**
   * Optionally concludes a block of buffers. If block mode is supported, this call will cause the
   * messages received since the beginBlock() call to be handled, e.g., by writing them to a USB or
   * other device.
   */
  void endBlock();

  public static class DummyReceiver implements MidiReceiver {
    @Override
    public void onNoteOff(int channel, int key, int velocity) {}

    @Override
    public void onNoteOn(int channel, int key, int velocity) {}

    @Override
    public void onPolyAftertouch(int channel, int key, int velocity) {}

    @Override
    public void onControlChange(int channel, int controller, int value) {}

    @Override
    public void onProgramChange(int channel, int program) {}

    @Override
    public void onAftertouch(int channel, int velocity) {}

    @Override
    public void onPitchBend(int channel, int value) {}

    @Override
    public void onRawByte(byte value) {}

    @Override
    public boolean beginBlock() {
      return false;
    }

    @Override
    public void endBlock() {}
  }
}
