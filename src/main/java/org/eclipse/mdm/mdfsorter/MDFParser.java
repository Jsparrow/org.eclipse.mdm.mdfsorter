/*
/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.logging.Level;

import org.eclipse.mdm.mdfsorter.mdf3.MDF3Parser;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4Parser;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4Util;

/**
 * A parser for the MDF file structure. This class provides functions to read a
 * file and transform it into a tree structure.
 *
 * @author EU2IYD9
 *
 */
public abstract class MDFParser {

	@SuppressWarnings("unchecked")
	public static MDFFileContent<? extends MDFGenBlock> serializeFile(FileChannel in) throws IOException {
		// some IDBLOCK Checks.
		char[] versionnum = new char[8];
		byte[] idblock = readBytes(64, in);

		String expected = "MDF     ";
		for (int i = 0; i < 8; i++) {
			if (idblock[i] != expected.charAt(i)) {
				MDFSorter.log.severe("No MDF File detected. Aborting.");
				throw new IllegalArgumentException("Unsupported MDF File.");
			}
		}

		for (int i = 8; i < 15; i++) {
			if (idblock[i] != 0) {
				versionnum[i - 8] = (char) idblock[i];
			}
		}

		int version = MDF4Util.readUInt16(getDataBuffer(idblock, 28, 30));
		MDFSorter.log.log(Level.FINE, "Found MDF Version " + String.valueOf(versionnum) + " (" + version + ")");

		@SuppressWarnings("rawtypes")
		MDFAbstractParser myParser = null;

		if (version < 300 || version > 411) {
			MDFSorter.log.severe("MDF Version " + String.valueOf(versionnum) + "is not supported. Aborting.");
			throw new IllegalArgumentException("Unsupported MDF Version.");
		} else if (version < 400) {
			boolean bigendian = MDF4Util.readUInt16(getDataBuffer(idblock, 24, 26)) != 0;
			myParser = new MDF3Parser(in, bigendian);
		} else {
			myParser = new MDF4Parser(in);
		}

		return myParser.parse();
	}

	/**
	 * Helper Method to read to an array from a FileChannel.
	 *
	 * @param bytes
	 *            The number of bytes to read.
	 * @param in
	 *            The FileChannel to read from.
	 * @return A byte-Array with <code>length=bytes</code> filled with the next
	 *         bytes from the Channel.
	 * @throws IOException
	 *             If an input error occurs.
	 */
	private static byte[] readBytes(int bytes, FileChannel in) throws IOException {
		ByteBuffer chunk = ByteBuffer.allocate(bytes);
		int bytesread = 0;
		if ((bytesread = in.read(chunk)) != bytes) {
			System.err.println("Read only " + bytesread + " Bytes instead of " + bytes);
		}
		return chunk.array();
	}

	/**
	 * Changes a section of a byte-Array to a ByteBuffer, which can be used in
	 * the parsing Methods of MDF4Util, for example to redeem an int-Value.
	 *
	 * @param data
	 *            The byte-Array, containing the data
	 * @param start
	 *            The first index of the section.
	 * @param end
	 *            The first index not included in the section.
	 * @return The section of the array, as a ByteBuffer.
	 */
	public static ByteBuffer getDataBuffer(byte[] data, int start, int end) {
		if (start >= 0 && end <= data.length) {
			return java.nio.ByteBuffer.wrap(Arrays.copyOfRange(data, start, end));
		} else {
			// just for testing
			throw new ArrayIndexOutOfBoundsException(
					"Tried to access bytes " + start + " to " + end + "with array length " + data.length);
		}
	}

}
