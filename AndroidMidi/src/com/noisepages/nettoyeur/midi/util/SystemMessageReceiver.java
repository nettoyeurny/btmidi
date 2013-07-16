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

/**
 * Interface for handling MIDI system messages.
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 */
public interface SystemMessageReceiver {

  /**
   * Handles system exclusive messages. Note that the byte array will hold just the payload, but not
   * the opening 0xf0 byte or the closing 0xf7.
   * 
   * @param sysex byte array holding the payload of the sysex message, without 0xf0 or 0xf7
   */
  void onSystemExclusive(byte[] sysex);

  // System common messages.
  void onTimeCode(int value);

  void onSongPosition(int pointer);

  void onSongSelect(int index);

  void onTuneRequest();

  // System real time messages.
  void onTimingClock();

  void onStart();

  void onContinue();

  void onStop();

  void onActiveSensing();

  void onSystemReset();

  public static class DummyReceiver implements SystemMessageReceiver {
    @Override
    public void onSystemExclusive(byte[] sysex) {}

    @Override
    public void onTimeCode(int value) {}

    @Override
    public void onSongPosition(int pointer) {}

    @Override
    public void onSongSelect(int index) {}

    @Override
    public void onTuneRequest() {}

    @Override
    public void onTimingClock() {}

    @Override
    public void onStart() {}

    @Override
    public void onContinue() {}

    @Override
    public void onStop() {}

    @Override
    public void onActiveSensing() {}

    @Override
    public void onSystemReset() {}
  }
}
