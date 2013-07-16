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

/**
 * Functionality common to all MIDI devices.
 * 
 * @author Peter Brinkmann (peter.brinkmann)
 */
public interface MidiDevice {

  /**
   * Releases resources in preparation for destruction, e.g., by closing the connection to the MIDI
   * device and stopping all listening threads.
   */
  public void close();
}
