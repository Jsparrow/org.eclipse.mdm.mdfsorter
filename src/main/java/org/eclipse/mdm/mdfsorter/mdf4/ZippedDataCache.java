/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter.mdf4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.eclipse.mdm.mdfsorter.MDFSorter;

public class ZippedDataCache {
	// private TreeMap<DZBLOCK, byte[]> cache;
	private LinkedList<DZBLOCK> cacheblocks;
	private LinkedList<byte[]> cachedata;
	private FileChannel reader;
	private static final int MAXENTRIES = 3;

	public ZippedDataCache(FileChannel reader) {
		cacheblocks = new LinkedList<DZBLOCK>();
		cachedata = new LinkedList<byte[]>();
		this.reader = reader;
	}

	public boolean isAvailable(DZBLOCK dzblk) {
		return cacheblocks.contains(dzblk);
	}

	public byte[] getData(DZBLOCK dzblk) {
		if (cacheblocks.contains(dzblk)) {
			return cachedata.get(cacheblocks.indexOf(dzblk));
		}
		throw new NoSuchElementException("Element not found in Data Cache.");
	}

	/**
	 * Load an DZBLOCK into Cache
	 *
	 * @param dzblk
	 *            The dzblock to load.
	 * @throws DataFormatException
	 *             If zipped data is in an invalid format.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public void load(DZBLOCK dzblk) throws DataFormatException, IOException {
		byte[] uncompressedData;
		Inflater decompresser = new Inflater();
		uncompressedData = new byte[(int) dzblk.getOrg_data_length()];
		ByteBuffer compressedData = ByteBuffer.allocate((int) dzblk.getData_length());
		// Skip header section of DZ Block
		reader.position(dzblk.getPos() + 48L);
		reader.read(compressedData);
		decompresser.setInput(compressedData.array(), 0, (int) dzblk.getData_length());

		int resultLength = decompresser.inflate(uncompressedData);
		decompresser.end();

		if (dzblk.transposeNeeded()) {
			int columnsize = (int) dzblk.getZip_parameters();
			uncompressedData = MDF4Util.transposeArray(uncompressedData, columnsize, false);
			MDFSorter.log.log(Level.FINER, "Transposing data with columnsize " + columnsize + ".");
		}

		if (resultLength != dzblk.getOrg_data_length()) {
			throw new RuntimeException("Data gain or loss detected while unziping. Expected "
					+ dzblk.getOrg_data_length() + " bytes, got " + resultLength);
		}
		MDFSorter.log.log(Level.FINER, "Unzipped block of size " + resultLength + ".");

		// Store Data in Cache
		if (cacheblocks.size() == MAXENTRIES) {
			cacheblocks.removeFirst();
			cachedata.removeFirst();
			System.out.println("Cache full. Removing");
		}
		cachedata.add(uncompressedData);
		cacheblocks.add(dzblk);
		System.out.println("Cache size:" + cacheblocks.size());
	}

	/**
	 * Reads buf.capacity bytes from the spezified DZBLOCK.
	 *
	 * @param dzblk
	 *            The DZBLOCK we want to read from-
	 * @param offset
	 *            The offset in the unzipped data of the DZBLOCK
	 * @param buf
	 *            The ByteBuffer data will be stored in.
	 * @throws DataFormatException
	 *             If Zipped-Data is in an invalid format
	 * @throws IOException
	 *             If an input error occurs.
	 */
	public void read(DZBLOCK dzblk, long offset, ByteBuffer buf) throws DataFormatException, IOException {
		// Load block if not available
		if (!isAvailable(dzblk)) {
			load(dzblk);
		}

		int readsize = buf.limit();
		byte[] dt = getData(dzblk);
		buf.put(dt, (int) offset, readsize);
	}
}
