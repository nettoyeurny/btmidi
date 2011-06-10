/* Copyright (C) 2011 Peter Brinkmann
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.noisepages.nettoyeur.midi.player;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.noisepages.nettoyeur.midi.file.InvalidMidiDataException;
import com.noisepages.nettoyeur.midi.file.MetaMessage;
import com.noisepages.nettoyeur.midi.file.MidiEvent;
import com.noisepages.nettoyeur.midi.file.MidiMessage;
import com.noisepages.nettoyeur.midi.file.MidiUtils;
import com.noisepages.nettoyeur.midi.file.MidiUtils.TempoCache;
import com.noisepages.nettoyeur.midi.file.Sequence;
import com.noisepages.nettoyeur.midi.file.StandardMidiFileReader;
import com.noisepages.nettoyeur.midi.file.Track;
import com.noisepages.nettoyeur.midi.file.spi.MidiFileReader;


/**
 * Instances of this class read MIDI files and convert them in to sequence of MIDI events
 * represented by byte arrays with time stamps.
 * 
 * @author Peter Brinkmann
 */
public class MidiFileSequencer implements Iterable<MidiFileSequencer.CompoundMidiEvent> {

	/**
	 * A container for time-stamped compound MIDI events, consisting of a time (measured in
	 * milliseconds) and a byte array containing one or more MIDI events in the MIDI wire
	 * format.
	 * 
	 * Note: Handing out raw byte arrays is very naughty, but I want this class to operate
	 * without the overhead of defensive copies.  Client code is expected not to overwrite
	 * the content of the byte array.
	 */
	public static class CompoundMidiEvent implements Comparable<CompoundMidiEvent>{
		public final long timeInMillis;
		public final byte[] midiBytes;
		
		private CompoundMidiEvent(long timeInMillies, byte[] midiBytes) {
			this.timeInMillis = timeInMillies;
			this.midiBytes = midiBytes;
		}

		@Override
		public int compareTo(CompoundMidiEvent another) {
			return (timeInMillis < another.timeInMillis) ? -1 :
				  ((timeInMillis > another.timeInMillis) ?  1 : 0);
		}
	}
	
	private final List<CompoundMidiEvent> events = new ArrayList<CompoundMidiEvent>();
	
	/**
	 * Duration of the entire song in milliseconds.
	 */
	public final long duration;

	/**
	 * Creates a new sequencer object for a MIDI file.
	 * 
	 * @param is input stream to read MIDI content from
	 * @throws InvalidMidiDataException thrown if the file is invalid
	 * @throws IOException thrown if the file can't be read
	 */
	public MidiFileSequencer(InputStream is) throws InvalidMidiDataException, IOException {
		MidiFileReader reader = new StandardMidiFileReader();
		Sequence seq = reader.getSequence(is);
		TempoCache tempoCache = new TempoCache(seq);
		Map<Long, ByteArrayOutputStream> eventsBuilder = new HashMap<Long, ByteArrayOutputStream>();
		long maxTime = 0;
		for (Track track : seq.getTracks()) {
			for (int i = 0; i < track.size(); i++) {
				MidiEvent event = track.get(i);
				MidiMessage message = event.getMessage();
				if (!(message instanceof MetaMessage)) {
					long time = (MidiUtils.tick2microsecond(seq, event.getTick(), tempoCache) + 500) / 1000;
					if (time > maxTime) {
						maxTime = time;
					}
					ByteArrayOutputStream baos = eventsBuilder.get(time);
					if (baos == null) {
						eventsBuilder.put(time, baos = new ByteArrayOutputStream());
					}
					baos.write(message.getMessage());
				}
			}
		}
		duration = maxTime;
		for (Entry<Long, ByteArrayOutputStream> entry : eventsBuilder.entrySet()) {
			events.add(new MidiFileSequencer.CompoundMidiEvent(entry.getKey(), entry.getValue().toByteArray()));
		}
		Collections.sort(events);
	}

	@Override
	public Iterator<MidiFileSequencer.CompoundMidiEvent> iterator() {
		return events.iterator();
	}
}