package com.noisepages.nettoyeur.usb;

import java.util.Scanner;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.hardware.usb.UsbDevice;

public class DeviceInfo {

	private final String vendor;
	private final String product;

	private DeviceInfo(String vendor, String product) {
		this.vendor = vendor;
		this.product = product;
	}

	@Override
	public String toString() {
		return vendor + ":" + product;
	}

	public String getVendor() {
		return vendor;
	}

	public String getProduct() {
		return product;
	}

	public static DeviceInfo getDeviceInfo(UsbDevice device) {
		return getDeviceInfo(device.getVendorId(), device.getProductId());
	}

	public static DeviceInfo getDeviceInfo(int vendorId, int productId) {
		String vendorHex = Integer.toHexString(0x10000 | vendorId).substring(1);
		String productHex = Integer.toHexString(0x10000 | productId).substring(1);
		String url = "http://usb-ids.gowdy.us/read/UD/" + vendorHex;
		String vendorName = null;
		String productName = null;
		try {
			vendorName = getName(url);
			productName = getName(url + "/" + productHex);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (vendorName != null && productName != null) ? new DeviceInfo(vendorName, productName) : null;
	}

	private static String getName(String url) throws Exception {
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(url);
		HttpResponse response = client.execute(request);
		Scanner scanner = new Scanner(response.getEntity().getContent());
		while (scanner.hasNext()) {
			String line = scanner.nextLine();
			int start = line.indexOf("Name:") + 6;
			if (start > 5) {
				int end = line.indexOf("<", start);
				return line.substring(start, end);
			}
		}
		return null;
	}
}
