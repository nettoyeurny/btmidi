/* Copyright (C) 2013 Peter Brinkmann
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

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SystemClock;

import com.noisepages.nettoyeur.common.RawByteReceiver;
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


public abstract class MidiSequence {

	private static class CompoundMidiEvent implements Comparable<CompoundMidiEvent>{
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
	
	/**
	 * Duration of the entire song in milliseconds.
	 */
	public final long duration;

	private final List<CompoundMidiEvent> events = new ArrayList<CompoundMidiEvent>();
	private Iterator<CompoundMidiEvent> eventIterator = null;
	private HandlerThread handlerThread = null;
	private RawByteReceiver receiver = null;
 
	/**
	 * Creates a new sequencer object for a MIDI file.
	 * 
	 * @param is input stream to read MIDI content from
	 * @throws InvalidMidiDataException thrown if the file is invalid
	 * @throws IOException thrown if the file can't be read
	 */
	public MidiSequence(InputStream is) throws InvalidMidiDataException, IOException {
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
			events.add(new MidiSequence.CompoundMidiEvent(entry.getKey(), entry.getValue().toByteArray()));
		}
		Collections.sort(events);
	}

	protected abstract void onPlaybackFinished();
	
	private class MidiRunnable implements Runnable {
		private byte[] buffer;
		private CompoundMidiEvent currentEvent;
		private final long t0;
		private final Handler handler;
		
		private MidiRunnable() {
			handler = new Handler(handlerThread.getLooper());
			currentEvent = eventIterator.next();
			t0 = SystemClock.uptimeMillis() - currentEvent.timeInMillis + 250;
		}
		
		private void scheduleNext() {
			buffer = currentEvent.midiBytes;
			handler.postAtTime(this, t0 + currentEvent.timeInMillis);
		}

		@Override
		public void run() {
			receiver.onBytesReceived(buffer.length, buffer);
			if (eventIterator.hasNext()) {
				currentEvent = eventIterator.next();
				scheduleNext();
			} else {
				pause();
				onPlaybackFinished();
			}
		}
	}
	
	/**
	 * Starts playback.
	 * 
	 * @param receiver to which MIDI bytes will be written
	 */
	public void start(RawByteReceiver receiver) {
		if (events.isEmpty()) {
			onPlaybackFinished();
			return;
		}
		pause();
		this.receiver = receiver;
		if (eventIterator == null || !eventIterator.hasNext()) {
			allNotesOff();
			resetAllControllers();
			eventIterator = events.iterator();
		}
		handlerThread = new HandlerThread("MidiSequencer", Process.THREAD_PRIORITY_AUDIO);
		handlerThread.start();
		MidiRunnable midiRunnable = new MidiRunnable();
		midiRunnable.scheduleNext();
	}

	/**
	 * Pauses playback.
	 */
	public void pause() {
		if (handlerThread == null) return;
		handlerThread.quit();
		try {
			handlerThread.join();
		} catch (InterruptedException e) {
			// Do nothing.
		}
		handlerThread = null;
		allNotesOff();
	}
	
	/**
	 * Rewinds to the beginning of the song.
	 */
	public void rewind() {
		pause();
		eventIterator = null;
	}
	
	public boolean isPlaying() {
		return handlerThread != null;
	}
	
	private void allNotesOff() {
		allChannels((byte) 0x7b, (byte) 0);
	}
	
	private void resetAllControllers() {
		allChannels((byte) 0x79, (byte) 0);
	}

	private void allChannels(byte controller, byte v) {
		byte[] buffer = new byte[] {0, controller, v};
		for (int c = 0x00; c < 0x10; ++c) {
			buffer[0] = (byte) (0xb0 | c);
			receiver.onBytesReceived(buffer.length, buffer);
		}
	}
}