package com.noisepages.nettoyeur.midi.util;

import com.noisepages.nettoyeur.midi.MidiReceiver;

public class SystemMessageEncoder implements SystemMessageReceiver {

	private final MidiReceiver receiver;
	
	public SystemMessageEncoder(MidiReceiver receiver) {
		this.receiver = receiver;
	}
	
	@Override
	public void onSystemExclusive(byte[] sysex) {
		receiver.onRawByte((byte) 0xf0);
		for (byte b : sysex) {
			receiver.onRawByte(b);
		}
		receiver.onRawByte((byte) 0xf7);
	}

	@Override
	public void onTimeCode(int value) {
		receiver.onRawByte((byte) 0xf1);
		receiver.onRawByte((byte) value);
	}

	@Override
	public void onSongPosition(int pointer) {
		if (pointer < 0 || pointer > 0x3fff) {
			throw new IllegalArgumentException("song position pointer out of range: " + pointer);
		}
		receiver.onRawByte((byte) 0xf2);
		receiver.onRawByte((byte) (pointer & 0x7f));
		receiver.onRawByte((byte) (pointer >> 7));
	}

	@Override
	public void onSongSelect(int index) {
		if (index < 0 || index > 0x7f) {
			throw new IllegalArgumentException("song index out of range: " + index);
		}
		receiver.onRawByte((byte) 0xf3);
		receiver.onRawByte((byte) (index & 0x7f));
	}

	@Override
	public void onTuneRequest() {
		receiver.onRawByte((byte) 0xf6);
	}

	@Override
	public void onTimingClock() {
		receiver.onRawByte((byte) 0xf8);
	}

	@Override
	public void onStart() {
		receiver.onRawByte((byte) 0xfa);
	}

	@Override
	public void onContinue() {
		receiver.onRawByte((byte) 0xfb);
	}

	@Override
	public void onStop() {
		receiver.onRawByte((byte) 0xfc);
	}

	@Override
	public void onActiveSensing() {
		receiver.onRawByte((byte) 0xfe);
	}

	@Override
	public void onSystemReset() {
		receiver.onRawByte((byte) 0xff);
	}
}
