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

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.noisepages.nettoyeur.midi.MidiReceiver;

public class SystemMessageEncoderTest {

  MidiReceiver receiver;
  SystemMessageEncoder encoder;

  @Before
  public void setUp() {
    receiver = EasyMock.createStrictMock(MidiReceiver.class);
    encoder = new SystemMessageEncoder(receiver);
  }

  @Test
  public void testSystemExclusive() {
    receiver.onRawByte((byte) 0xf0);
    receiver.onRawByte((byte) 0xf7);
    receiver.onRawByte((byte) 0xf0);
    receiver.onRawByte((byte) 0x00);
    receiver.onRawByte((byte) 0x7f);
    receiver.onRawByte((byte) 0x03);
    receiver.onRawByte((byte) 0xf7);
    EasyMock.replay(receiver);
    encoder.onSystemExclusive(new byte[0]);
    encoder.onSystemExclusive(new byte[] {0x00, 0x7f, 0x03});
    EasyMock.verify(receiver);
  }

  @Test
  public void testTimeCode() {
    receiver.onRawByte((byte) 0xf1);
    receiver.onRawByte((byte) 0x00);
    receiver.onRawByte((byte) 0xf1);
    receiver.onRawByte((byte) 0x7f);
    receiver.onRawByte((byte) 0xf1);
    receiver.onRawByte((byte) 0x61);
    EasyMock.replay(receiver);
    encoder.onTimeCode(0x00);
    encoder.onTimeCode(0x7f);
    encoder.onTimeCode(0x61);
    EasyMock.verify(receiver);
  }

  @Test
  public void testSongPosition() {
    receiver.onRawByte((byte) 0xf2);
    receiver.onRawByte((byte) 0x00);
    receiver.onRawByte((byte) 0x00);
    receiver.onRawByte((byte) 0xf2);
    receiver.onRawByte((byte) 0x7f);
    receiver.onRawByte((byte) 0x7f);
    receiver.onRawByte((byte) 0xf2);
    receiver.onRawByte((byte) 0x03);
    receiver.onRawByte((byte) 0x01);
    EasyMock.replay(receiver);
    encoder.onSongPosition(0);
    encoder.onSongPosition(0x3fff);
    encoder.onSongPosition(0x0083);
    EasyMock.verify(receiver);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadSongPosition1() {
    encoder.onSongPosition(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadSongPosition2() {
    encoder.onSongPosition(0x4000);
  }

  @Test
  public void testSongSelect() {
    receiver.onRawByte((byte) 0xf3);
    receiver.onRawByte((byte) 0x00);
    receiver.onRawByte((byte) 0xf3);
    receiver.onRawByte((byte) 0x7f);
    receiver.onRawByte((byte) 0xf3);
    receiver.onRawByte((byte) 0x45);
    EasyMock.replay(receiver);
    encoder.onSongSelect(0);
    encoder.onSongSelect(0x7f);
    encoder.onSongSelect(0x45);
    EasyMock.verify(receiver);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadSongIndex1() {
    encoder.onSongSelect(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadSongIndex2() {
    encoder.onSongSelect(128);
  }

  @Test
  public void testTuneRequest() {
    receiver.onRawByte((byte) 0xf6);
    EasyMock.replay(receiver);
    encoder.onTuneRequest();
    EasyMock.verify(receiver);

  }

  @Test
  public void testTimingClock() {
    receiver.onRawByte((byte) 0xf8);
    EasyMock.replay(receiver);
    encoder.onTimingClock();
    EasyMock.verify(receiver);
  }

  @Test
  public void testStart() {
    receiver.onRawByte((byte) 0xfa);
    EasyMock.replay(receiver);
    encoder.onStart();
    EasyMock.verify(receiver);
  }

  @Test
  public void testContinue() {
    receiver.onRawByte((byte) 0xfb);
    EasyMock.replay(receiver);
    encoder.onContinue();
    EasyMock.verify(receiver);
  }

  @Test
  public void testStop() {
    receiver.onRawByte((byte) 0xfc);
    EasyMock.replay(receiver);
    encoder.onStop();
    EasyMock.verify(receiver);
  }

  @Test
  public void testActiveSensing() {
    receiver.onRawByte((byte) 0xfe);
    EasyMock.replay(receiver);
    encoder.onActiveSensing();
    EasyMock.verify(receiver);
  }

  @Test
  public void testSystemReset() {
    receiver.onRawByte((byte) 0xff);
    EasyMock.replay(receiver);
    encoder.onSystemReset();
    EasyMock.verify(receiver);
  }
}
