/*
 * Copyright (C) 2013 Peter Brinkmann (peter.brinkmann@gmail.com)
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

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.noisepages.nettoyeur.common.RawByteReceiver;

public class ToWireConverterTest {

	private ToWireConverter converter;
	private RawByteReceiver receiver;
	
	@Before
	public void setUp() {
		receiver = EasyMock.createStrictMock(RawByteReceiver.class);
		converter = new ToWireConverter(receiver);
	}

	@Test
	public void testNoteOff() {
		byte[] msg = new byte[] { (byte) 0x80, 0x48, 0x7f };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0x81, 0x60, 0x0f };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0x8f, 0x00, 0x00 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		EasyMock.replay(receiver);
		converter.onNoteOff(0x00, 0x48, 0x7f);
		converter.onNoteOff(0x01, 0x60, 0x0f);
		converter.onNoteOff(0x0f, 0x00, 0x00);
		EasyMock.verify(receiver);
	}

	@Test
	public void testNoteOn() {
		byte[] msg = new byte[] { (byte) 0x90, 0x48, 0x7f };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0x91, 0x60, 0x0f };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0x9f, 0x00, 0x00 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		EasyMock.replay(receiver);
		converter.onNoteOn(0x00, 0x48, 0x7f);
		converter.onNoteOn(0x01, 0x60, 0x0f);
		converter.onNoteOn(0x0f, 0x00, 0x00);
		EasyMock.verify(receiver);
	}

	@Test
	public void testPolyAftertouch() {
		byte[] msg = new byte[] { (byte) 0xa0, 0x48, 0x7f };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0xa1, 0x60, 0x0f };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0xaf, 0x00, 0x00 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		EasyMock.replay(receiver);
		converter.onPolyAftertouch(0x00, 0x48, 0x7f);
		converter.onPolyAftertouch(0x01, 0x60, 0x0f);
		converter.onPolyAftertouch(0x0f, 0x00, 0x00);
		EasyMock.verify(receiver);
	}

	@Test
	public void testControlChange() {
		byte[] msg = new byte[] { (byte) 0xb0, 0x48, 0x7f };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0xb1, 0x60, 0x0f };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0xbf, 0x00, 0x00 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		EasyMock.replay(receiver);
		converter.onControlChange(0x00, 0x48, 0x7f);
		converter.onControlChange(0x01, 0x60, 0x0f);
		converter.onControlChange(0x0f, 0x00, 0x00);
		EasyMock.verify(receiver);
	}

	@Test
	public void testProgramChange() {
		byte[] msg = new byte[] { (byte) 0xc0, 0x48 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0xc1, 0x60 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0xcf, 0x00 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		EasyMock.replay(receiver);
		converter.onProgramChange(0x00, 0x48);
		converter.onProgramChange(0x01, 0x60);
		converter.onProgramChange(0x0f, 0x00);
		EasyMock.verify(receiver);
	}

	@Test
	public void testAftertouch() {
		byte[] msg = new byte[] { (byte) 0xd0, 0x48 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0xd1, 0x60 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0xdf, 0x00 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		EasyMock.replay(receiver);
		converter.onAftertouch(0x00, 0x48);
		converter.onAftertouch(0x01, 0x60);
		converter.onAftertouch(0x0f, 0x00);
		EasyMock.verify(receiver);
	}

	@Test
	public void testPitchBend() {
		byte[] msg = new byte[] { (byte) 0xe4, 0x00, 0x40 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0xe4, 0x7f, 0x7f };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0xe4, 0x00, 0x00 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0xe4, 0x7f, 0x3f };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0xe0, 0x7e, 0x3f };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0xef, 0x05, 0x40 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		EasyMock.replay(receiver);
		converter.onPitchBend(0x04, 0);
		converter.onPitchBend(0x04, 8191);
		converter.onPitchBend(0x04, -8192);
		converter.onPitchBend(0x04, -1);
		converter.onPitchBend(0x00, -2);
		converter.onPitchBend(0x0f, 5);
		EasyMock.verify(receiver);
	}

	@Test
	public void testMidiByte() {
		byte[] msg = new byte[] { (byte) 0xf0 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0x01 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0x02 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0x03 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		msg = new byte[] { (byte) 0xf7 };
		receiver.onBytesReceived(EasyMock.eq(msg.length), EasyMock.aryEq(msg));
		EasyMock.replay(receiver);
		converter.onRawByte((byte) 0xf0);
		converter.onRawByte((byte) 0x01);
		converter.onRawByte((byte) 0x02);
		converter.onRawByte((byte) 0x03);
		converter.onRawByte((byte) 0xf7);
		EasyMock.verify(receiver);
	}
	
	@Test
	public void testIdentity() {  // Somewhat redundant, but it's satisfying to see this work.
		FromWireConverter identity = new FromWireConverter(converter);
		byte[] msg1 = new byte[] { (byte) 0x81, 0x60, 0x0f };
		receiver.onBytesReceived(EasyMock.eq(msg1.length), EasyMock.aryEq(msg1));
		byte[] msg2 = new byte[] { (byte) 0x8f, 0x00, 0x00 };
		receiver.onBytesReceived(EasyMock.eq(msg2.length), EasyMock.aryEq(msg2));
		byte[] msg3 = new byte[] { (byte) 0xe4, 0x00, 0x00 };
		receiver.onBytesReceived(EasyMock.eq(msg3.length), EasyMock.aryEq(msg3));
		byte[] msg4 = new byte[] { (byte) 0xe4, 0x7f, 0x3f };
		receiver.onBytesReceived(EasyMock.eq(msg4.length), EasyMock.aryEq(msg4));
		byte[] msg5 = new byte[] { (byte) 0xe0, 0x7e, 0x3f };
		receiver.onBytesReceived(EasyMock.eq(msg5.length), EasyMock.aryEq(msg5));
		byte[] msg6 = new byte[] { (byte) 0xf0, 0x01, 0x02, 0x03, (byte) 0xf7 };
		for (byte b : msg6) {
			receiver.onBytesReceived(EasyMock.eq(1), EasyMock.aryEq(new byte[] {b}));
		}
		byte[] msg7 = new byte[] { (byte) 0xc1, 0x60 };
		receiver.onBytesReceived(EasyMock.eq(msg7.length), EasyMock.aryEq(msg7));
		byte[] msg8 = new byte[] { (byte) 0xcf, 0x00 };
		receiver.onBytesReceived(EasyMock.eq(msg8.length), EasyMock.aryEq(msg8));
		EasyMock.replay(receiver);
		identity.onBytesReceived(msg1.length, msg1);
		identity.onBytesReceived(msg2.length, msg2);
		identity.onBytesReceived(msg3.length, msg3);
		identity.onBytesReceived(msg4.length, msg4);
		identity.onBytesReceived(msg5.length, msg5);
		identity.onBytesReceived(msg6.length, msg6);
		identity.onBytesReceived(msg7.length, msg7);
		identity.onBytesReceived(msg8.length, msg8);
		EasyMock.verify(receiver);
	}
}
