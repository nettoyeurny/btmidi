package com.noisepages.nettoyeur.usbmidi;

public interface PermissionHandler {
	public void onPermissionGranted();
	public void onPermissionDenied();
}
