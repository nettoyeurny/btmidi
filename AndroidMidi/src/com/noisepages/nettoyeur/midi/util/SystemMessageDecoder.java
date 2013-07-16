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

package com.noisepages.nettoyeur.midi.util;

import java.io.ByteArrayOutputStream;

/**
 * Utility class for decoding MIDI system messages. Instances of this class parse an incoming stream
 * of MIDI bytes, e.g., from the onRawByte method of MidiReceiver, and extract system messages.
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 */
public class SystemMessageDecoder {

  private enum State {
    SYSTEM_EXCLUSIVE, TIME_CODE, SONG_POSITION, SONG_SELECT, NONE
  };

  private final SystemMessageReceiver receiver;
  private State state = State.NONE;
  private int firstByte = -1;
  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

  public SystemMessageDecoder(SystemMessageReceiver receiver) {
    this.receiver = receiver;
  }

  /**
   * Handles an incoming MIDI byte.
   * 
   * @param value to be decoded
   * @return true if the byte was handled as part of a system message
   */
  public boolean decodeByte(byte value) {
    boolean byteHandled = true;
    switch (value) {
    // Handle system common messages.
      case (byte) 0xf0:
        buffer.reset();
        state = State.SYSTEM_EXCLUSIVE;
        break;
      case (byte) 0xf1:
        state = State.TIME_CODE;
        break;
      case (byte) 0xf2:
        firstByte = -1;
        state = State.SONG_POSITION;
        break;
      case (byte) 0xf3:
        state = State.SONG_SELECT;
        break;
      case (byte) 0xf6:
        receiver.onTuneRequest();
        state = State.NONE;
        break;
      case (byte) 0xf7:
        if (state == State.SYSTEM_EXCLUSIVE) {
          receiver.onSystemExclusive(buffer.toByteArray());
        }
        state = State.NONE;
        break;
      // Handel system real time messages. Note that those don't reset the state since they may be
      // interleaved with sysex messages.
      case (byte) 0xf8:
        receiver.onTimingClock();
        break;
      case (byte) 0xfa:
        receiver.onStart();
        break;
      case (byte) 0xfb:
        receiver.onContinue();
        break;
      case (byte) 0xfc:
        receiver.onStop();
        break;
      case (byte) 0xfe:
        receiver.onActiveSensing();
        break;
      case (byte) 0xff:
        receiver.onSystemReset();
        break;
      default:
        if (value >= 0) {
          switch (state) {
            case SYSTEM_EXCLUSIVE:
              buffer.write(value);
              break;
            case TIME_CODE:
              receiver.onTimeCode(value);
              state = State.NONE;
              break;
            case SONG_POSITION:
              if (firstByte < 0) {
                firstByte = value;
              } else {
                receiver.onSongPosition((value << 7) | firstByte);
                state = State.NONE;
              }
              break;
            case SONG_SELECT:
              receiver.onSongSelect(value);
              state = State.NONE;
              break;
            default:
              byteHandled = false;
              break;
          }
        } else {
          state = State.NONE;
          byteHandled = false;
        }
        break;
    }
    return byteHandled;
  }
}
