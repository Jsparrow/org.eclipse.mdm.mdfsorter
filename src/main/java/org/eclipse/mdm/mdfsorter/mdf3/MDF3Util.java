/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter.mdf3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author EU2IYD9
 *
 */
public abstract class MDF3Util {

	/**
	 * Returns the expected number of links for a specific MDF3 Blocktype.
	 *
	 * @param blockID
	 *            The ID-String of the block, e.g. "HD", "DT"...
	 * @return The number of Links, return[0] = number of links in the link
	 *         section, return[1] = total number of links
	 */
	public static int getLinkcount(String blockID) {
		if (blockID.length() != 2) {
			throw new IllegalArgumentException("ID-Length = " + blockID.length());
		}

		switch (blockID) {
		case "HD":
			return 3;
		case "TX":
			return 0;
		case "PR":
			return 0;
		case "TR":
			return 1;
		case "SR":
			return 2;
		case "DG":
			return 4; // special case: pointer to data
		case "CG":
			return 3; // includes link to SR-BLOCK
		case "CN":
			return 5;
		case "CD":
			return 0;
		case "CE":
			return 0;
		case "CC":
			return 0;
		default:
			System.err.println("Unknown blocktype: " + blockID);
			return 0;
		}
	}

	/**
	 * Read an 8-bit signed integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static int readUInt8(ByteBuffer bb) {
		return bb.get() & 0xff;
	}

	/**
	 * Read an 16-bit unsigned integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @param bigendian
	 *            True, if this number is in BigEndian order, false if
	 *            LittleEndian.
	 * @return The value.
	 */
	public static int readUInt16(ByteBuffer bb, boolean bigendian) {
		setByteOrder(bb, bigendian);
		return bb.getShort() & 0xffff;
	}

	/**
	 * Get a byte array from an 16-bit unsigned integer. (Little Endian)
	 *
	 * @param val
	 *            The number to convert.
	 * @param bigendian
	 *            True, if this number is in BigEndian order, false if
	 *            LittleEndian.
	 * @return The bytes.
	 */
	public static byte[] getBytesUInt16(int val, boolean bigendian) {
		ByteBuffer b = ByteBuffer.allocate(2);
		setByteOrder(b, bigendian);
		b.putShort((short) val);
		return b.array();
	}

	/**
	 * Read an 16-bit signed integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @param bigendian
	 *            True, if this number is in BigEndian order, false if
	 *            LittleEndian.
	 * @return The value.
	 */
	public static short readInt16(ByteBuffer bb, boolean bigendian) {
		setByteOrder(bb, bigendian);
		return bb.getShort();
	}

	/**
	 * Get a byte array from an 16-bit signed integer. (Little Endian)
	 *
	 * @param val
	 *            The number to convert.
	 * @param bigendian
	 *            True, if this number is in BigEndian order, false if
	 *            LittleEndian.
	 * @return The bytes.
	 */
	public static byte[] getBytesInt16(short val, boolean bigendian) {
		ByteBuffer b = ByteBuffer.allocate(2);
		setByteOrder(b, bigendian);
		b.putShort(val);
		return b.array();
	}

	/**
	 * Read an 32-bit unsigned integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @param bigendian
	 *            True, if this number is in BigEndian order, false if
	 *            LittleEndian.
	 * @return The value.
	 */
	public static long readUInt32(ByteBuffer bb, boolean bigendian) {
		setByteOrder(bb, bigendian);
		return bb.getInt() & 0xffffffffL;
	}

	/**
	 * Get a byte array form an 32-bit unsigned integer. (Little Endian)
	 *
	 * @param val
	 *            The number to convert.
	 * @param bigendian
	 *            True, if this number is in BigEndian order, false if
	 *            LittleEndian.
	 * @return The bytes.
	 */
	public static byte[] getBytesUInt32(long val, boolean bigendian) {
		ByteBuffer b = ByteBuffer.allocate(4);
		setByteOrder(b, bigendian);
		b.putInt((int) val);
		return b.array();
	}

	public static long readUInt64(ByteBuffer bb, boolean bigendian) {
		setByteOrder(bb, bigendian);
		return bb.getLong();
	}

	public static byte[] getBytesUInt64(long val, boolean bigendian) {
		ByteBuffer b = ByteBuffer.allocate(8);
		setByteOrder(b, bigendian);
		b.putLong(val);
		return b.array();
	}

	public static boolean readBool(ByteBuffer bb, boolean bigendian) {
		return readUInt16(bb, bigendian) != 0;
	}

	public static byte[] getBytesBool(boolean val, boolean bigendian) {
		if (val) {
			return getBytesInt16((short) 1, bigendian);
		} else {
			return getBytesInt16((short) 0, bigendian);
		}
	}

	private static void setByteOrder(ByteBuffer buf, boolean bigendian) {
		if (bigendian) {
			buf.order(ByteOrder.BIG_ENDIAN);
		} else {
			buf.order(ByteOrder.LITTLE_ENDIAN);
		}
	}

	/**
	 * Read a LINK.
	 *
	 * @param bb
	 *            The ByteBuffer to read from.
	 * @param bigendian
	 *            True, if this number is in BigEndian order, false if
	 *            LittleEndian.
	 * @return The value of this link.
	 */
	public static long readLink(ByteBuffer bb, boolean bigendian) {
		return readUInt32(bb, bigendian);
	}

	/**
	 * Get bytes from a link address.
	 *
	 * @param val
	 *            The value.
	 * @param bigendian
	 *            True, if this number is in BigEndian order, false if
	 *            LittleEndian.
	 * @return The link address as byte array.
	 */
	public static byte[] getBytesLink(long val, boolean bigendian) {
		return getBytesUInt32(val, bigendian);
	}
}
