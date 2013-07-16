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

package com.noisepages.nettoyeur.common;


/**
 * Callback for handling raw byte input.
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 */
public interface RawByteReceiver {

  /**
   * Processes the given buffer.
   * 
   * @param nBytes number of bytes to be processed, started at the beginning of the buffer
   * @param buffer buffer to be processed
   */
  void onBytesReceived(int nBytes, byte[] buffer);

  /**
   * Begin assembling subsequent calls to onBytesReceived into one buffer. This is an optional
   * optimization.
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
}
