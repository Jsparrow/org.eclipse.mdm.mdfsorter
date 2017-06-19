/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter.mdf3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.DataFormatException;

import org.eclipse.mdm.mdfsorter.AbstractDataProvider;
import org.eclipse.mdm.mdfsorter.ReadDataCache;

/**
 * This Class is used to read Data from a data section, with an given offset. If
 * Data is stored in a linked list or in a zipped block, it is this classes job
 * to provide the data needed.
 *
 * @author Tobias Leemann
 *
 */
/**
 * @author EU2IYD9
 *
 */
public class MDF3DataProvider implements AbstractDataProvider {

	private FileChannel reader;
	private MDF3GenBlock datasectionhead;

	/**
	 * Data Cache.
	 */
	private ReadDataCache readCache;

	/**
	 * Length of this data section.
	 */
	private long sectionlength;

	private byte[] dataarr;

	/**
	 * This constructer creates a new DataProvider with the given head.
	 *
	 * @param datasectionhead
	 *            The header of the data section.
	 * @param reader
	 *            FileChannel to the input file.
	 */
	public MDF3DataProvider(MDF3GenBlock datasectionhead, FileChannel reader) {
		this.datasectionhead = datasectionhead;
		this.reader = reader;
		sectionlength = calculateLength();

		readCache = new ReadDataCache(this);

	}

	public MDF3DataProvider(byte[] data) {
		dataarr = data;
		sectionlength = data.length;
	}

	public void setDataArray(byte[] dataArray) {
		dataarr = dataArray;
		sectionlength = dataArray.length;
	}

	/**
	 * Read length bytes from this datasection, starting at globaloffset (The
	 * offset in the datasection) This method physically reads data, and does
	 * not check the read data cache.
	 *
	 * @param globaloffset
	 *            The offset from the start of this section, e.g. DTBLOCK
	 * @param data
	 *            The ByteBuffer to read in. data.limit bytes are read.
	 * @throws IOException
	 *             If an I/O-Error occurs.
	 */
	@Override
	public void read(long globaloffset, ByteBuffer data) throws IOException {

		if (globaloffset + data.capacity() > sectionlength) {
			throw new IllegalArgumentException(
					"Invalid read access on Data Provider. Section is only " + sectionlength + " bytes long.");
		}

		if (dataarr != null) {
			data.put(dataarr, (int) globaloffset, data.capacity());
			data.rewind();
			return;
		}
		reader.position(datasectionhead.getPos() + globaloffset);
		reader.read(data);
		data.rewind();
	}

	/**
	 * Returns a ByteBuffer with position at globaloffset, where length bytes
	 * can be read from. If the data is available in the cache, it is taken from
	 * there.
	 *
	 * @param globaloffset
	 *            The place in the data section, where the read should start.
	 * @param length
	 *            The length to read.
	 * @return A ByteBuffer with the position at global offset, from which
	 *         length bytes can be read.
	 * @throws IOException
	 *             If an I/O-Error occurs.
	 * @throws DataFormatException
	 *             If zipped data is in an invalid Format (this cannot happen
	 *             here)
	 */
	@Override
	public ByteBuffer cachedRead(long globaloffset, int length) throws IOException, DataFormatException {
		// argument check
		if (globaloffset + length > sectionlength) {
			throw new IllegalArgumentException(
					"Invalid read access on Data Provider. Section is only " + sectionlength + " bytes long.");
		}

		if (dataarr != null) {
			ByteBuffer data = ByteBuffer.allocate(length);
			data.put(dataarr, (int) globaloffset, data.capacity());
			data.rewind();
			return data;
		} else {
			return readCache.read(globaloffset, length);
		}

	}

	@Override
	public long getLength() {
		return sectionlength;
	}

	/**
	 * Calculate the length of this data section.
	 *
	 * @return The length.
	 */
	private long calculateLength() {
		if (datasectionhead != null) {
			return datasectionhead.getLength();
		}
		return 0;

	}
}
