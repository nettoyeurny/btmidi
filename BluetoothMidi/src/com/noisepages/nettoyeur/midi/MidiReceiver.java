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
 * Callbacks for handling MIDI events and connection changes. Note that this
 * interface chooses sanity over compliance with the MIDI standard, i.e.,
 * channel numbers start at 0, and pitch bend values are centered at 0.
 * 
 * @author Peter Brinkmann
 */
public interface MidiReceiver {

	/**
	 * Handles note off events.
	 * 
	 * @param channel starting at 0
	 * @param key
	 * @param velocity
	 */
	void onNoteOff(int channel, int key, int velocity);

	/**
	 * Handles note on events.
	 * 
	 * @param channel starting at 0
	 * @param key
	 * @param velocity
	 */
	void onNoteOn(int channel, int key, int velocity);

	/**
	 * Handles polyphonic aftertouch events.
	 * 
	 * @param channel starting at 0
	 * @param key
	 * @param velocity
	 */
	void onPolyAftertouch(int channel, int key, int velocity);

	/**
	 * Handles a control change message.
	 * 
	 * @param channel starting at 0
	 * @param controller
	 * @param value
	 */
	void onControlChange(int channel, int controller, int value);

	/**
	 * Handles a program change message.
	 * 
	 * @param channel starting at 0
	 * @param program
	 */
	void onProgramChange(int channel, int program);

	/**
	 * Handles a channel aftertouch event.
	 * 
	 * @param channel starting at 0
	 * @param velocity
	 */
	void onAftertouch(int channel, int velocity);

	/**
	 * Handles a pitch bend event.
	 * 
	 * @param channel starting at 0
	 * @param value centered at 0, ranging from -8192 to 8191
	 */
	void onPitchBend(int channel, int value);

	/**
	 * Handles a raw byte; a raw byte is a byte that is received from the Bluetooth device but
	 * doesn't belong to any of the channel messages that are explicitly handled by this
	 * interface.
	 * 
 	 * The idea is that we don't want to burden this basic interface with support for obscure
	 * system messages, but at the same time we don't want to hide any MIDI events from client
	 * code.  If you need system messages, you can assemble them, one byte at a time, from
	 * successive calls to this method.
	 * 
	 * @param value
	 */
	void onRawByte(int value);
}
