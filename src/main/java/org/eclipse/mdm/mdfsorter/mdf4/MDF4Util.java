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


package org.eclipse.mdm.mdfsorter.mdf4;
//package de.rechner.openatfx_mdf.mdf4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utility class having methods to read MDF file contents.
 *
 * @author Christian Rechner
 */
public abstract class MDF4Util {

	// For the strings in the IDBLOCK and for the block identifiers, always
	// single byte character (SBC) encoding is used
	// (standard ASCII extension ISO-8859-1 Latin character set).
	private static final String CHARSET_ISO8859 = "ISO-8859-1";

	// The string encoding used in an MDF file is UTF-8 (1-4 Bytes for each
	// character).
	// This applies to TXBLOCK and MDBLOCK data.
	private static final String CHARSET_UTF8 = "UTF-8";

	// Size of the first block in MDF
	public static int headersize = 64;

	/**
	 * Read an 8-bit signed integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static byte readUInt8(ByteBuffer bb) {
		return bb.order(ByteOrder.LITTLE_ENDIAN).get();
	}

	/**
	 * Read an 8-bit signed integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static int readInt8(ByteBuffer bb) {
		return bb.order(ByteOrder.LITTLE_ENDIAN).get() & 0xff;
	}

	/**
	 * Read an 16-bit unsigned integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static int readUInt16(ByteBuffer bb) {
		// bb.flip();
		return bb.order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff;
	}

	/**
	 * Get a byte array from an 16-bit unsigned integer. (Little Endian)
	 *
	 * @param val
	 *            The number to convert.
	 * @return The bytes.
	 */
	public static byte[] getBytesUInt16(int val) {
		var b = ByteBuffer.allocate(2);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.putShort((short) val);
		return b.array();
	}

	/**
	 * Read an 16-bit signed integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static short readInt16(ByteBuffer bb) {
		return bb.order(ByteOrder.LITTLE_ENDIAN).getShort();
	}

	/**
	 * Get a byte array from an 16-bit signed integer. (Little Endian)
	 *
	 * @param val
	 *            The number to convert.
	 * @return The bytes.
	 */
	public static byte[] getBytesInt16(int val) {
		var b = ByteBuffer.allocate(2);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.putShort((short) val);
		return b.array();
	}

	/**
	 * Read an 32-bit unsigned integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static long readUInt32(ByteBuffer bb) {
		return bb.order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffffL;
	}

	/**
	 * Get a byte array form an 32-bit unsigned integer. (Little Endian)
	 *
	 * @param val
	 *            The number to convert.
	 * @return The bytes.
	 */
	public static byte[] getBytesUInt32(long val) {
		var b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.putInt((int) val);
		return b.array();
	}

	/**
	 * Read an 32-bit signed integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static int readInt32(ByteBuffer bb) {
		return bb.order(ByteOrder.LITTLE_ENDIAN).getInt();
	}

	/**
	 * Read an 64-bit unsigned integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static long readUInt64(ByteBuffer bb) {
		byte[] data = new byte[8];
		bb.get(data);
		long l1 = ((long) data[0] & 0xff) << 0 | ((long) data[1] & 0xff) << 8 | ((long) data[2] & 0xff) << 16
				| ((long) data[3] & 0xff) << 24;
		long l2 = ((long) data[4] & 0xff) << 0 | ((long) data[5] & 0xff) << 8 | ((long) data[6] & 0xff) << 16
				| ((long) data[7] & 0xff) << 24;
		return l1 << 0 | l2 << 32;
	}

	/**
	 * Get a byte array form an 64-bit unsigned integer. (Little Endian)
	 *
	 * @param val
	 *            The number to convert.
	 * @return The bytes.
	 */
	public static byte[] getBytesUInt64(long val) {
		var b = ByteBuffer.allocate(8);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.putLong(val);
		return b.array();
	}

	/**
	 * Read an 64-bit signed integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static long readInt64(ByteBuffer bb) {
		return bb.order(ByteOrder.LITTLE_ENDIAN).getLong();
	}

	/**
	 * Read a floating-point value compliant with IEEE 754, double precision (64
	 * bits) (see [IEEE-FP]) from the byte buffer. An infinite value (e.g. for
	 * tabular ranges in conversion rule) can be expressed using the NaNs
	 * INFINITY resp.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static double readReal(ByteBuffer bb) {
		return bb.order(ByteOrder.LITTLE_ENDIAN).getDouble();
	}

	public static byte[] getBytesReal(double val) {
		var bb = ByteBuffer.allocate(8);
		bb.order(ByteOrder.LITTLE_ENDIAN).putDouble(val);
		return bb.array();
	}

	/**
	 * Read a 64-bit signed integer from the byte buffer, used as byte position
	 * within the file. If a LINK is NIL (corresponds to 0), this means the LINK
	 * cannot be de-referenced. A link must be a multiple of 8.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value as long.
	 */
	public static long readLink(ByteBuffer bb) {
		byte[] data = new byte[8];
		bb.get(data);
		long l1 = ((long) data[0] & 0xff) << 0 | ((long) data[1] & 0xff) << 8 | ((long) data[2] & 0xff) << 16
				| ((long) data[3] & 0xff) << 24;
		long l2 = ((long) data[4] & 0xff) << 0 | ((long) data[5] & 0xff) << 8 | ((long) data[6] & 0xff) << 16
				| ((long) data[7] & 0xff) << 24;
		return l1 << 0 | l2 << 32;
	}

	/**
	 * Get a byte array from a link address. (Unsigned 64 bit int, Little
	 * Endian)
	 *
	 * @param lnk
	 *            The link address to convert.
	 * @return The bytes.
	 */
	public static byte[] getBytesLink(long lnk) {
		var b = ByteBuffer.allocate(8);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.putLong(lnk);
		return b.array();
	}

	/**
	 * Read a String in ISO 8859
	 *
	 * @param bb
	 *            The ByteBuffer to use.
	 * @param length
	 *            Number of chars to read.
	 * @return The result as String.
	 * @throws IOException
	 *             If an reading error occurs.
	 */
	public static String readCharsISO8859(ByteBuffer bb, int length) throws IOException {
		byte[] b = new byte[length];
		bb.get(b);
		return new String(b, 0, length, CHARSET_ISO8859);
	}

	/**
	 * Read a String from UTF-8 encoded bytes
	 *
	 * @param bb
	 *            The ByteBuffer to use.
	 * @param length
	 *            Number of chars to read.
	 * @return The result as String.
	 * @throws IOException
	 *             If an reading error occurs.
	 */
	public static String readCharsUTF8(ByteBuffer bb, int length) throws IOException {
		byte[] b = new byte[length];
		bb.get(b);
		return new String(b, 0, length, CHARSET_UTF8);
	}

	public static byte[] getBytesCharsUTF8(String s) throws IOException {
		return s.getBytes(CHARSET_UTF8);
	}

	public static byte[] getBytesCharsUTF8WithTerminator(String s) throws IOException {
		var term = "\0";
		return (s + term).getBytes(CHARSET_UTF8);
	}

	/**
	 * Shifts an array for compression according to the MDF-Standard
	 *
	 * @param data
	 *            The array to shift.
	 * @param columnsize
	 *            The size of each shift column.
	 * @param forward
	 *            True: The Input is the original array, output is shifted.
	 *            False: vice versa. Input is shifted, and output is original
	 *            Array.
	 * @return An array containing the transposed Data.
	 */
	public static byte[] transposeArray(byte[] data, int columnsize, boolean forward) {
		byte[] out = new byte[data.length];
		int len = data.length;
		int rows = len / columnsize;

		int scolumnsize = columnsize;
		if (!forward) {
			scolumnsize = rows;
			rows = columnsize;
		}
		int counter = 0;
		for (int i = 0; i < scolumnsize; i++) {
			for (int j = 0; j < rows; j++) {
				out[counter++] = data[i + j * scolumnsize];
			}
		}
		System.arraycopy(data, counter, out, counter, len - counter);
		return out;

	}

}
