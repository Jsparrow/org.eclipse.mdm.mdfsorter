/********************************************************************************
 * Copyright (c) 2015-2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 ********************************************************************************/


package org.eclipse.mdm.mdfsorter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.eclipse.mdm.mdfsorter.mdf3.MDF3Util;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MDF3UtilTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testGetBytesUInt32() {
		long l = Integer.MAX_VALUE * 2L + 1L;
		assertArrayEquals(MDF3Util.getBytesUInt32(l, false), new byte[] { -1, -1, -1, -1 });

		l = Integer.MAX_VALUE * 2L + 1L;
		assertArrayEquals(MDF3Util.getBytesUInt32(l, true), new byte[] { -1, -1, -1, -1 });
	}

	@Test
	public void testGetBytesUInt16() {
		int val = Short.MAX_VALUE + 1;
		assertArrayEquals(MDF3Util.getBytesUInt16(val, false), new byte[] { 0, -128 });
	}

	@Test
	public void ReadUINT32() {
		long val = Integer.MAX_VALUE + 1L;
		ByteBuffer buf1 = ByteBuffer.wrap(new byte[] { -128, 0, 0, 0 });
		assertEquals(val, MDF3Util.readUInt32(buf1, true));

		ByteBuffer buf2 = ByteBuffer.wrap(new byte[] { 0, 0, 0, -128 });
		assertEquals(val, MDF3Util.readUInt32(buf2, false));
	}

	@Test
	public void testGetBool() {
		assertArrayEquals(new byte[] { 1, 0 }, MDF3Util.getBytesBool(true, false));
	}

	@Test
	public void testParseBool() {
		ByteBuffer buf1 = ByteBuffer.wrap(new byte[] { 0, -1 });
		assertTrue(MDF3Util.readBool(buf1, false));
		buf1.rewind();
		assertTrue(MDF3Util.readBool(buf1, true));
		ByteBuffer buf2 = ByteBuffer.wrap(new byte[] { 0, 0 });
		assertFalse(MDF3Util.readBool(buf2, false));
	}

}
