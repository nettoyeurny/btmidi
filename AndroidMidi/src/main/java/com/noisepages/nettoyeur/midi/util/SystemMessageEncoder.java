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

import com.noisepages.nettoyeur.midi.MidiReceiver;

public class SystemMessageEncoder implements SystemMessageReceiver {

  private final MidiReceiver receiver;

  public SystemMessageEncoder(MidiReceiver receiver) {
    this.receiver = receiver;
  }

  @Override
  public void onSystemExclusive(byte[] sysex) {
    receiver.beginBlock();
    receiver.onRawByte((byte) 0xf0);
    for (byte b : sysex) {
      receiver.onRawByte(b);
    }
    receiver.onRawByte((byte) 0xf7);
    receiver.endBlock();
  }

  @Override
  public void onTimeCode(int value) {
    receiver.beginBlock();
    receiver.onRawByte((byte) 0xf1);
    receiver.onRawByte((byte) value);
    receiver.endBlock();
  }

  @Override
  public void onSongPosition(int pointer) {
    if (pointer < 0 || pointer > 0x3fff) {
      throw new IllegalArgumentException("song position pointer out of range: " + pointer);
    }
    receiver.beginBlock();
    receiver.onRawByte((byte) 0xf2);
    receiver.onRawByte((byte) (pointer & 0x7f));
    receiver.onRawByte((byte) (pointer >> 7));
    receiver.endBlock();
  }

  @Override
  public void onSongSelect(int index) {
    if (index < 0 || index > 0x7f) {
      throw new IllegalArgumentException("song index out of range: " + index);
    }
    receiver.beginBlock();
    receiver.onRawByte((byte) 0xf3);
    receiver.onRawByte((byte) (index & 0x7f));
    receiver.endBlock();
  }

  @Override
  public void onTuneRequest() {
    receiver.onRawByte((byte) 0xf6);
  }

  @Override
  public void onTimingClock() {
    receiver.onRawByte((byte) 0xf8);
  }

  @Override
  public void onStart() {
    receiver.onRawByte((byte) 0xfa);
  }

  @Override
  public void onContinue() {
    receiver.onRawByte((byte) 0xfb);
  }

  @Override
  public void onStop() {
    receiver.onRawByte((byte) 0xfc);
  }

  @Override
  public void onActiveSensing() {
    receiver.onRawByte((byte) 0xfe);
  }

  @Override
  public void onSystemReset() {
    receiver.onRawByte((byte) 0xff);
  }
}
