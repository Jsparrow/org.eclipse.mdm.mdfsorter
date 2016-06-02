/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter;

import static org.junit.Assert.assertArrayEquals;

import org.eclipse.mdm.mdfsorter.mdf4.MDF4Util;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MDF4UtilTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testGetBytesUInt32() {
		long l = Integer.MAX_VALUE * 2L + 1L;
		assertArrayEquals(MDF4Util.getBytesUInt32(l),
				new byte[] { -1, -1, -1, -1 });
	}

	@Test
	public void testGetBytesUInt16() {
		int val = Short.MAX_VALUE + 1;
		assertArrayEquals(MDF4Util.getBytesUInt16(val), new byte[] { 0, -128 });
	}

	@Test
	public void testArrayTranspose() {

		// See MDF Base Specification page 147 for this example
		byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		byte[] out = MDF4Util.transposeArray(data, 3, true);
		byte[] expected = new byte[] { 1, 4, 2, 5, 3, 6, 7, 8 };
		assertArrayEquals(expected, out);

		// and back
		byte[] result = MDF4Util.transposeArray(out, 3, false);
		assertArrayEquals(result, data);

	}

	@Test // Test a larger array
	public void testArrayTranspose2() {

		byte[] data = new byte[1000];
		for (int i = 0; i < 1000; i++) {
			data[i] = (byte) (i % 256);
		}
		byte[] out = MDF4Util.transposeArray(data, 256, true);
		byte[] expected = new byte[1000];
		for (int i = 0; i < 256; i++) {
			expected[3 * i] = (byte) i;
			expected[3 * i + 1] = (byte) i;
			expected[3 * i + 2] = (byte) i;
		}
		for (int j = 768; j < 1000; j++) {
			expected[j] = (byte) (j - 768);
		}
		assertArrayEquals(expected, out);

		// and back
		byte[] result = MDF4Util.transposeArray(out, 256, false);
		assertArrayEquals(result, data);
	}
}
