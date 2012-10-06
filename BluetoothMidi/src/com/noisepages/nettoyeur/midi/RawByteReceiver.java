/*
 * Copyright (C) 2011 Peter Brinkmann (peter.brinkmann@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.noisepages.nettoyeur.midi;


/**
 * Callback for handling raw byte input.
 * 
 * @author Peter Brinkmann
 */
public interface RawByteReceiver {

	/**
	 * Processes the given buffer.
	 * 
	 * @param nBytes number of bytes to be processed, started at the beginning of the buffer
	 * @param buffer buffer to be processed
	 */
	void onBytesReceived(int nBytes, byte[] buffer);

}
