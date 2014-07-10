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

import com.noisepages.nettoyeur.common.RawByteReceiver;


/**
 * Converter from MIDI wire format to MIDI events.
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 */
public class FromWireConverter implements RawByteReceiver {

  private static enum State {
    NOTE_OFF, NOTE_ON, POLY_TOUCH, CONTROL_CHANGE, PROGRAM_CHANGE, AFTERTOUCH, PITCH_BEND, NONE
  }

  private final MidiReceiver midiReceiver;
  private State midiState = State.NONE;
  private int channel;
  private int firstByte;

  public FromWireConverter(MidiReceiver midiReceiver) {
    this.midiReceiver = midiReceiver;
  }

  @Override
  public void onBytesReceived(int nBytes, byte[] buffer) {
    for (int i = 0; i < nBytes; i++) {
      processByte(buffer[i]);
    }
  }

  private void processByte(int b) {
    if (b < 0) {
      midiState = State.values()[(b >> 4) & 0x07];
      if (midiState != State.NONE) {
        channel = b & 0x0f;
        firstByte = -1;
      } else {
        midiReceiver.onRawByte((byte) b);
      }
    } else {
      switch (midiState) {
        case NOTE_OFF:
          if (firstByte < 0) {
            firstByte = b;
          } else {
            midiReceiver.onNoteOff(channel, firstByte, b);
            firstByte = -1;
          }
          break;
        case NOTE_ON:
          if (firstByte < 0) {
            firstByte = b;
          } else {
            midiReceiver.onNoteOn(channel, firstByte, b);
            firstByte = -1;
          }
          break;
        case POLY_TOUCH:
          if (firstByte < 0) {
            firstByte = b;
          } else {
            midiReceiver.onPolyAftertouch(channel, firstByte, b);
            firstByte = -1;
          }
          break;
        case CONTROL_CHANGE:
          if (firstByte < 0) {
            firstByte = b;
          } else {
            midiReceiver.onControlChange(channel, firstByte, b);
            firstByte = -1;
          }
          break;
        case PROGRAM_CHANGE:
          midiReceiver.onProgramChange(channel, b);
          break;
        case AFTERTOUCH:
          midiReceiver.onAftertouch(channel, b);
          break;
        case PITCH_BEND:
          if (firstByte < 0) {
            firstByte = b;
          } else {
            midiReceiver.onPitchBend(channel, ((b << 7) | firstByte) - 8192);
            firstByte = -1;
          }
          break;
        default /* State.NONE */:
          midiReceiver.onRawByte((byte) b);
          break;
      }
    }
  }

  @Override
  public boolean beginBlock() {
    return midiReceiver.beginBlock();
  }

  @Override
  public void endBlock() {
    midiReceiver.endBlock();
  }
}
