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

package com.noisepages.nettoyeur.midi;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

public class FromWireConverterTest {

  private FromWireConverter converter;
  private MidiReceiver receiver;

  @Before
  public void setUp() {
    receiver = EasyMock.createStrictMock(MidiReceiver.class);
    converter = new FromWireConverter(receiver);
  }

  @Test
  public void testNoteOff() {
    receiver.onNoteOff(0x00, 0x48, 0x7f);
    receiver.onNoteOff(0x01, 0x60, 0x0f);
    receiver.onNoteOff(0x0f, 0x00, 0x00);
    EasyMock.replay(receiver);
    byte[] msg = new byte[] {(byte) 0x80, 0x48, 0x7f};
    converter.onBytesReceived(msg.length, msg);
    msg = new byte[] {(byte) 0x81, 0x60, 0x0f, (byte) 0x8f, 0x00, 0x00};
    converter.onBytesReceived(msg.length, msg);
    EasyMock.verify(receiver);
  }

  @Test
  public void testNoteOn() {
    receiver.onNoteOn(0x00, 0x48, 0x7f);
    receiver.onNoteOn(0x01, 0x60, 0x0f);
    receiver.onNoteOn(0x0f, 0x00, 0x00);
    EasyMock.replay(receiver);
    byte[] msg = new byte[] {(byte) 0x90, 0x48, 0x7f};
    converter.onBytesReceived(msg.length, msg);
    msg = new byte[] {(byte) 0x91, 0x60, 0x0f, (byte) 0x9f, 0x00, 0x00};
    converter.onBytesReceived(msg.length, msg);
    EasyMock.verify(receiver);
  }

  @Test
  public void testPolyAftertouch() {
    receiver.onPolyAftertouch(0x00, 0x48, 0x7f);
    receiver.onPolyAftertouch(0x01, 0x60, 0x0f);
    receiver.onPolyAftertouch(0x0f, 0x00, 0x00);
    EasyMock.replay(receiver);
    byte[] msg = new byte[] {(byte) 0xa0, 0x48, 0x7f};
    converter.onBytesReceived(msg.length, msg);
    msg = new byte[] {(byte) 0xa1, 0x60, 0x0f, (byte) 0xaf, 0x00, 0x00};
    converter.onBytesReceived(msg.length, msg);
    EasyMock.verify(receiver);
  }

  @Test
  public void testControlChange() {
    receiver.onControlChange(0x00, 0x48, 0x7f);
    receiver.onControlChange(0x01, 0x60, 0x0f);
    receiver.onControlChange(0x0f, 0x00, 0x00);
    EasyMock.replay(receiver);
    byte[] msg = new byte[] {(byte) 0xb0, 0x48, 0x7f};
    converter.onBytesReceived(msg.length, msg);
    msg = new byte[] {(byte) 0xb1, 0x60, 0x0f, (byte) 0xbf, 0x00, 0x00};
    converter.onBytesReceived(msg.length, msg);
    EasyMock.verify(receiver);
  }

  @Test
  public void testProgramChange() {
    receiver.onProgramChange(0x00, 0x48);
    receiver.onProgramChange(0x01, 0x60);
    receiver.onProgramChange(0x0f, 0x00);
    EasyMock.replay(receiver);
    byte[] msg = new byte[] {(byte) 0xc0, 0x48};
    converter.onBytesReceived(msg.length, msg);
    msg = new byte[] {(byte) 0xc1, 0x60, (byte) 0xcf, 0x00};
    converter.onBytesReceived(msg.length, msg);
    EasyMock.verify(receiver);
  }

  @Test
  public void testAftertouch() {
    receiver.onAftertouch(0x00, 0x48);
    receiver.onAftertouch(0x01, 0x60);
    receiver.onAftertouch(0x0f, 0x00);
    EasyMock.replay(receiver);
    byte[] msg = new byte[] {(byte) 0xd0, 0x48};
    converter.onBytesReceived(msg.length, msg);
    msg = new byte[] {(byte) 0xd1, 0x60, (byte) 0xdf, 0x00};
    converter.onBytesReceived(msg.length, msg);
    EasyMock.verify(receiver);
  }

  @Test
  public void testPitchBend() {
    receiver.onPitchBend(0x04, 0);
    receiver.onPitchBend(0x04, 8191);
    receiver.onPitchBend(0x04, -8192);
    receiver.onPitchBend(0x04, -1);
    receiver.onPitchBend(0x00, -2);
    receiver.onPitchBend(0x0f, 5);
    EasyMock.replay(receiver);
    byte[] msg = new byte[] {(byte) 0xe4, 0x00, 0x40};
    converter.onBytesReceived(msg.length, msg);
    msg =
        new byte[] {(byte) 0xe4, 0x7f, 0x7f, (byte) 0xe4, 0x00, 0x00, (byte) 0xe4, 0x7f, 0x3f,
            (byte) 0xe0, 0x7e, 0x3f, (byte) 0xef, 0x05, 0x40};
    converter.onBytesReceived(msg.length, msg);
    EasyMock.verify(receiver);
  }

  @Test
  public void testMidiByte() {
    receiver.onRawByte((byte) 0xf0);
    receiver.onRawByte((byte) 0x01);
    receiver.onRawByte((byte) 0x02);
    receiver.onRawByte((byte) 0x03);
    receiver.onRawByte((byte) 0xf7);
    EasyMock.replay(receiver);
    byte[] msg = new byte[] {(byte) 0xf0};
    converter.onBytesReceived(msg.length, msg);
    msg = new byte[] {0x01, 0x02, 0x03, (byte) 0xf7};
    converter.onBytesReceived(msg.length, msg);
    EasyMock.verify(receiver);
  }

  @Test
  public void testCompoundMessages() {
    receiver.onNoteOff(0x03, 0x48, 0x7f);
    receiver.onNoteOff(0x03, 0x60, 0x23);
    receiver.onPolyAftertouch(0x0b, 0x10, 0x2f);
    receiver.onPolyAftertouch(0x0b, 0x7f, 0x50);
    receiver.onNoteOn(0x08, 0x30, 0x70);
    receiver.onNoteOn(0x08, 0x60, 0x75);
    EasyMock.replay(receiver);
    // Probably not too common, but I've seen sequences like this from my Roland HP-3e digital
    // piano.
    byte[] msg =
        new byte[] {(byte) 0x83, 0x48, 0x7f, 0x60, 0x23, (byte) 0xab, 0x10, 0x2f, 0x7f, 0x50,
            (byte) 0x98, 0x30, 0x70, 0x60, 0x75};
    converter.onBytesReceived(msg.length, msg);
    EasyMock.verify(receiver);
  }

  @Test
  public void testMixedMessages() { // No pun intended.
    receiver.onPitchBend(0x04, -1);
    receiver.onPitchBend(0x00, -2);
    receiver.onPitchBend(0x0f, 5);
    receiver.onProgramChange(0x01, 0x60);
    receiver.onProgramChange(0x0f, 0x00);
    receiver.onRawByte((byte) 0xf0);
    receiver.onRawByte((byte) 0x01);
    receiver.onRawByte((byte) 0x02);
    receiver.onRawByte((byte) 0x03);
    receiver.onRawByte((byte) 0xf7);
    receiver.onPolyAftertouch(0x01, 0x60, 0x0f);
    receiver.onPolyAftertouch(0x0f, 0x00, 0x00);
    EasyMock.replay(receiver);
    byte[] msg =
        new byte[] {(byte) 0xe4, 0x7f, 0x3f, (byte) 0xe0, 0x7e, 0x3f, (byte) 0xef, 0x05, 0x40,
            (byte) 0xc1, 0x60, (byte) 0xcf, 0x00, (byte) 0xf0, 0x01, 0x02, 0x03, (byte) 0xf7,
            (byte) 0xa1, 0x60, 0x0f, (byte) 0xaf, 0x00, 0x00};
    converter.onBytesReceived(msg.length, msg);
    EasyMock.verify(receiver);
  }

  @Test
  public void testIdentity() { // Somewhat redundant, but it's satisfying to see this work.
    ToWireConverter identity = new ToWireConverter(converter);
    receiver.onPitchBend(0x04, -1);
    receiver.onPitchBend(0x00, -2);
    receiver.onPitchBend(0x0f, 5);
    receiver.onProgramChange(0x01, 0x60);
    receiver.onProgramChange(0x0f, 0x00);
    receiver.onRawByte((byte) 0xf0);
    receiver.onRawByte((byte) 0x01);
    receiver.onRawByte((byte) 0x02);
    receiver.onRawByte((byte) 0x03);
    receiver.onRawByte((byte) 0xf7);
    receiver.onPolyAftertouch(0x01, 0x60, 0x0f);
    receiver.onPolyAftertouch(0x0f, 0x00, 0x00);
    EasyMock.replay(receiver);
    identity.onPitchBend(0x04, -1);
    identity.onPitchBend(0x00, -2);
    identity.onPitchBend(0x0f, 5);
    identity.onProgramChange(0x01, 0x60);
    identity.onProgramChange(0x0f, 0x00);
    identity.onRawByte((byte) 0xf0);
    identity.onRawByte((byte) 0x01);
    identity.onRawByte((byte) 0x02);
    identity.onRawByte((byte) 0x03);
    identity.onRawByte((byte) 0xf7);
    identity.onPolyAftertouch(0x01, 0x60, 0x0f);
    identity.onPolyAftertouch(0x0f, 0x00, 0x00);
    EasyMock.verify(receiver);
  }
}
