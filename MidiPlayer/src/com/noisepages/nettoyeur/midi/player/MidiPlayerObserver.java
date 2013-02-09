package com.noisepages.nettoyeur.midi.player;

import com.noisepages.nettoyeur.bluetooth.BluetoothSppObserver;

public interface MidiPlayerObserver extends BluetoothSppObserver {

	void onPlaybackFinished();

}
