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

import static org.junit.Assert.*;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

public class SystemMessageDecoderTest {

  private SystemMessageDecoder decoder;
  private SystemMessageReceiver receiver;

  @Before
  public void setUp() {
    receiver = EasyMock.createStrictMock(SystemMessageReceiver.class);
    decoder = new SystemMessageDecoder(receiver);
  }

  @Test
  public void testSysex() {
    receiver.onSystemExclusive(EasyMock.aryEq(new byte[0]));
    byte[] msg = new byte[] {0x20, 0x01, 0x02, 0x00, 0x7f, 0x60};
    receiver.onSystemExclusive(EasyMock.aryEq(msg));
    receiver.onSystemExclusive(EasyMock.aryEq(new byte[0]));
    EasyMock.replay(receiver);
    assertTrue(decoder.decodeByte((byte) 0xf0));
    assertTrue(decoder.decodeByte((byte) 0xf7));
    assertTrue(decoder.decodeByte((byte) 0xf0));
    for (byte b : msg) {
      assertTrue(decoder.decodeByte(b));
    }
    assertTrue(decoder.decodeByte((byte) 0xf7));
    assertTrue(decoder.decodeByte((byte) 0xf0));
    assertTrue(decoder.decodeByte((byte) 0xf7));
    EasyMock.verify(receiver);
  }

  @Test
  public void testTimeCode() {
    receiver.onTimeCode(0);
    receiver.onTimeCode(0x7f);
    receiver.onTimeCode(0x40);
    EasyMock.replay(receiver);
    assertTrue(decoder.decodeByte((byte) 0xf1));
    assertTrue(decoder.decodeByte((byte) 0x00));
    assertTrue(decoder.decodeByte((byte) 0xf1));
    assertTrue(decoder.decodeByte((byte) 0x7f));
    assertTrue(decoder.decodeByte((byte) 0xf1));
    assertTrue(decoder.decodeByte((byte) 0x40));
    EasyMock.verify(receiver);
  }

  @Test
  public void testSongPosition() {
    receiver.onSongPosition(0x00);
    receiver.onSongPosition(0x60);
    receiver.onSongPosition(0x80);
    receiver.onSongPosition(0x3fff);
    EasyMock.replay(receiver);
    assertTrue(decoder.decodeByte((byte) 0xf2));
    assertTrue(decoder.decodeByte((byte) 0x00));
    assertTrue(decoder.decodeByte((byte) 0x00));
    assertTrue(decoder.decodeByte((byte) 0xf2));
    assertTrue(decoder.decodeByte((byte) 0x60));
    assertTrue(decoder.decodeByte((byte) 0x00));
    assertTrue(decoder.decodeByte((byte) 0xf2));
    assertTrue(decoder.decodeByte((byte) 0x00));
    assertTrue(decoder.decodeByte((byte) 0x01));
    assertTrue(decoder.decodeByte((byte) 0xf2));
    assertTrue(decoder.decodeByte((byte) 0x7f));
    assertTrue(decoder.decodeByte((byte) 0x7f));
    EasyMock.verify(receiver);
  }

  @Test
  public void testSongSelect() {
    receiver.onSongSelect(0x00);
    receiver.onSongSelect(0x35);
    receiver.onSongSelect(0x7f);
    EasyMock.replay(receiver);
    assertTrue(decoder.decodeByte((byte) 0xf3));
    assertTrue(decoder.decodeByte((byte) 0x00));
    assertTrue(decoder.decodeByte((byte) 0xf3));
    assertTrue(decoder.decodeByte((byte) 0x35));
    assertTrue(decoder.decodeByte((byte) 0xf3));
    assertTrue(decoder.decodeByte((byte) 0x7f));
    EasyMock.verify(receiver);
  }

  @Test
  public void testTuneRequest() {
    receiver.onTuneRequest();
    EasyMock.replay(receiver);
    assertTrue(decoder.decodeByte((byte) 0xf6));
    EasyMock.verify(receiver);
  }

  @Test
  public void testTimingClock() {
    receiver.onTimingClock();
    EasyMock.replay(receiver);
    assertTrue(decoder.decodeByte((byte) 0xf8));
    EasyMock.verify(receiver);
  }

  @Test
  public void testStart() {
    receiver.onStart();
    EasyMock.replay(receiver);
    assertTrue(decoder.decodeByte((byte) 0xfa));
    EasyMock.verify(receiver);
  }

  @Test
  public void testContinue() {
    receiver.onContinue();
    EasyMock.replay(receiver);
    assertTrue(decoder.decodeByte((byte) 0xfb));
    EasyMock.verify(receiver);
  }

  @Test
  public void testStop() {
    receiver.onStop();
    EasyMock.replay(receiver);
    assertTrue(decoder.decodeByte((byte) 0xfc));
    EasyMock.verify(receiver);
  }

  @Test
  public void testActiveSensing() {
    receiver.onActiveSensing();
    EasyMock.replay(receiver);
    assertTrue(decoder.decodeByte((byte) 0xfe));
    EasyMock.verify(receiver);
  }

  @Test
  public void testSystemReset() {
    receiver.onSystemReset();
    EasyMock.replay(receiver);
    assertTrue(decoder.decodeByte((byte) 0xff));
    EasyMock.verify(receiver);
  }

  @Test
  public void testUnhandled() {
    receiver.onTuneRequest();
    EasyMock.replay(receiver);
    assertFalse(decoder.decodeByte((byte) 0x00));
    assertFalse(decoder.decodeByte((byte) 0xf4));
    assertFalse(decoder.decodeByte((byte) 0xf5));
    assertFalse(decoder.decodeByte((byte) 0xf9));
    assertFalse(decoder.decodeByte((byte) 0xfd));
    assertTrue(decoder.decodeByte((byte) 0xf0));
    assertTrue(decoder.decodeByte((byte) 0xf6));
    assertFalse(decoder.decodeByte((byte) 0x00));
    assertTrue(decoder.decodeByte((byte) 0xf7));
    EasyMock.verify(receiver);
  }

  @Test
  public void testInterleaved() {
    receiver.onStart();
    receiver.onSystemExclusive(EasyMock.aryEq(new byte[] {0x01, 0x02, 0x03, 0x04}));
    EasyMock.replay(receiver);
    assertTrue(decoder.decodeByte((byte) 0xf0));
    assertTrue(decoder.decodeByte((byte) 0x01));
    assertTrue(decoder.decodeByte((byte) 0x02));
    assertTrue(decoder.decodeByte((byte) 0xfa)); // Start message in the middle of a sysex message.
    assertTrue(decoder.decodeByte((byte) 0x03));
    assertTrue(decoder.decodeByte((byte) 0x04));
    assertTrue(decoder.decodeByte((byte) 0xf7));
    EasyMock.verify(receiver);
  }
}
