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
 * Converter from MIDI events to MIDI wire format.
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 */
public class ToWireConverter implements MidiReceiver {

  private final RawByteReceiver rawReceiver;

  public ToWireConverter(RawByteReceiver rawReceiver) {
    this.rawReceiver = rawReceiver;
  }

  @Override
  public void onNoteOff(int ch, int note, int vel) {
    write(0x80, ch, note, vel);
  }

  @Override
  public void onNoteOn(int ch, int note, int vel) {
    write(0x90, ch, note, vel);
  }

  @Override
  public void onPolyAftertouch(int ch, int note, int vel) {
    write(0xa0, ch, note, vel);
  }

  @Override
  public void onControlChange(int ch, int ctl, int val) {
    write(0xb0, ch, ctl, val);
  }

  @Override
  public void onProgramChange(int ch, int pgm) {
    write(0xc0, ch, pgm);
  }

  @Override
  public void onAftertouch(int ch, int vel) {
    write(0xd0, ch, vel);
  }

  @Override
  public void onPitchBend(int ch, int val) {
    val += 8192;
    write(0xe0, ch, (val & 0x7f), (val >> 7));
  }

  @Override
  public void onRawByte(byte value) {
    writeBytes(value);
  }

  private void write(int msg, int ch, int a) {
    writeBytes(firstByte(msg, ch), (byte) a);
  }

  private void write(int msg, int ch, int a, int b) {
    writeBytes(firstByte(msg, ch), (byte) a, (byte) b);
  }

  private byte firstByte(int msg, int ch) {
    return (byte) (msg | (ch & 0x0f));
  }

  private void writeBytes(byte... out) {
    rawReceiver.onBytesReceived(out.length, out);
  }

  @Override
  public boolean beginBlock() {
    return rawReceiver.beginBlock();
  }

  @Override
  public void endBlock() {
    rawReceiver.endBlock();
  }
}
