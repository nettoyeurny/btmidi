package com.noisepages.nettoyeur.midi.util;

import java.io.ByteArrayOutputStream;

import com.noisepages.nettoyeur.midi.MidiReceiver;

public abstract class SysexReceiver implements MidiReceiver {

	private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
	private boolean sysexInProgress = false;
	
	@Override public void onNoteOff(int channel, int key, int velocity) {}
	@Override public void onNoteOn(int channel, int key, int velocity) {}
	@Override public void onPolyAftertouch(int channel, int key, int velocity) {}
	@Override public void onControlChange(int channel, int controller, int value) {}
	@Override public void onProgramChange(int channel, int program) {}
	@Override public void onAftertouch(int channel, int velocity) {}
	@Override public void onPitchBend(int channel, int value) {}

	@Override
	public void onRawByte(byte value) {
		switch (value) {
		case (byte) 0xf0:
			bytes.reset();
			sysexInProgress = true;
			bytes.write(value);
			break;
		case (byte) 0xf7:
			if (sysexInProgress) {
				bytes.write(value);
				onSysex(bytes.toByteArray());
				sysexInProgress = false;
			}
			break;
		default:
			if (sysexInProgress) {
				if (value >= 0) {
					bytes.write(value);
				} else {
					sysexInProgress = false;
				}
			}
			break;
		}
	}
	
	public abstract void onSysex(byte[] sysex);
}
