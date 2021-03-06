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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ArgumentParserTest {

	// Test Exceptions thrown
	// Only on argument
	@Test(expected = IllegalArgumentException.class)
	public void testTooFewArgs() {
		String[] test1 = { "process", "file1" };
		ArgumentStruct.parseArgs(test1);
	}

	// Unknown Flag test
	@Test(expected = IllegalArgumentException.class)
	public void testUnknownFlag() {
		String[] test1 = { "process", "file1", "file2", "-somerandomflag=0", "-unzip" };
		ArgumentStruct.parseArgs(test1);
	}

	// Two different zipflags
	@Test(expected = IllegalArgumentException.class)
	public void testZipFlags() {
		String[] test1 = { "process", "file1", "file2", "-zip", "-maxblocksize=4g", "-unzip" };
		ArgumentStruct.parseArgs(test1);
	}

	// Check maxblocksize without value
	@Test(expected = IllegalArgumentException.class)
	public void testNoValue() {
		String[] test1 = { "process", "file1", "file2", "-zip", "-maxblocksize=" };
		ArgumentStruct.parseArgs(test1);
	}

	@Test // Check values of ArgumentStruct
	public void testParsing1() {
		String[] test1 = { "process", "file1", "file2", "-zip", "-maxblocksize=300" };
		var ar = ArgumentStruct.parseArgs(test1);
		assertEquals(ar.inputname, "file1");
		assertEquals(ar.outputname, "file2");
		assertEquals(ar.unzip, false);
		assertEquals(ar.maxblocksize, 300L);
	}

	@Test // Check values of ArgumentStruct
	public void testParsing2() {
		String[] test1 = { "process", "file1", "file2", "-maxblocksize=3M" };
		var ar = ArgumentStruct.parseArgs(test1);
		assertEquals(ar.maxblocksize, 3L * 1024L * 1024L);

		test1[3] = "-maxblocksize=34k";
		ar = ArgumentStruct.parseArgs(test1);
		assertEquals(ar.maxblocksize, 34L * 1024L);

		test1[3] = "-maxblocksize=3000m";
		ar = ArgumentStruct.parseArgs(test1);
		assertEquals(ar.maxblocksize, 3000L * 1024L * 1024L);

		test1[3] = "-maxblocksize=3G";
		ar = ArgumentStruct.parseArgs(test1);
		assertEquals(ar.maxblocksize, 3 * 1024L * 1024L * 1024L);
	}
}
