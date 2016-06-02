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
import java.util.zip.DataFormatException;

import org.eclipse.mdm.mdfsorter.MDFAbstractProcessWriter;

public class MDF3BlocksSplittMerger {

	/**
	 * Parent Object.
	 */
	private MDF3ProcessWriter ps;

	/**
	 * Pointer to block in which the data is written.
	 */
	private DTBLOCK curr;

	/**
	 * Pointer to block that is currently written. If curr is an DZBlock, write
	 * out data is buffered here, and zipped and written out, once all data for
	 * this block is read.
	 */
	private final MDF3GenBlock parentnode;

	/**
	 * Total length of the new Data section. Needs to be calculated in advance
	 * before processing blocks.
	 */
	private final long totdatalength;

	/**
	 * Amount of bytes of data written behind the output block
	 */
	private long datawritten = 0;

	/**
	 * An offset in the global data section, only used, if the block that is
	 * read from is not known (towrite == null)
	 */
	private long globalReadPtr = 0;

	/**
	 * Data Provider for reading blocks. This Object manages unzipping of
	 * blocks.
	 */
	private MDF3DataProvider prov;

	/**
	 * Create a new Datasection with the given options, to write data without
	 * concern about the underlying block structure. Totaldatalength and prov
	 * can be passed manually. Therfore oldsection is not needed.
	 *
	 * @param ps
	 *            The parent ProcessWriter
	 * @param parentnode
	 *            Node that is parent of this datasection.
	 * @param totdatalength
	 *            The length of data that will be written.
	 * @param prov
	 *            The DataProvider to read from.
	 */
	public MDF3BlocksSplittMerger(MDF3ProcessWriter ps, MDF3GenBlock parentnode,
			long totdatalength, MDF3DataProvider prov) {
		this.ps = ps;
		this.parentnode = parentnode;

		this.totdatalength = totdatalength;
		this.prov = prov;

	}

	public DTBLOCK getStructuralRoot() {
		return curr;
	}

	/**
	 * Append the datasection beginning at <code>startaddress</code> until
	 * length to the output.
	 *
	 * @param startaddress
	 *            Beginning of the section.
	 * @param length
	 *            Length of the section.
	 * @throws IOException
	 *             If an input error occurs.
	 * @throws DataFormatException
	 *             If zipped data is in an invalid format.
	 */
	public void splitmerge(long startaddress, long length)
			throws IOException, DataFormatException {
		globalReadPtr = startaddress;
		appendDataFromPos(length);
	}

	/**
	 * Reads leftbytes from the current position and appends them to the output
	 * section.
	 *
	 * @param leftbytes
	 *            The number of bytes to append.
	 * @throws IOException
	 *             If an I/O-Error occurs.
	 * @throws DataFormatException
	 *             If zipped data is in an invalid format.
	 */
	public void appendDataFromPos(long leftbytes)
			throws IOException, DataFormatException {
		// check if space in curr-Block is available, and fill with first data,
		// or attach all data if it fits
		if (curr == null) {
			curr = abstractcreate(totdatalength);
		}

		if (datawritten + leftbytes <= totdatalength) { // Space available
			abstractcopy(leftbytes);
			datawritten += leftbytes;
		} else {
			throw new RuntimeException(
					"MDF3Merger got more data than space was reserved.");
		}
	}

	/**
	 * Read data was read from the stream or Cache.
	 *
	 * @param length
	 *            Number of Bytes to get.
	 * @return A Bytebuffer where <code>length</code> Bytes can be read from.
	 * @throws IOException
	 *             If an I/O-Error occurs.
	 * @throws DataFormatException
	 *             If zipped data is in an invalid format.
	 */
	public ByteBuffer abstractread(int length)
			throws IOException, DataFormatException {
		return prov.cachedRead(globalReadPtr, length);
	}

	public void abstractput(ByteBuffer buf, int length) {
		ps.performPut(buf, length, true);
	}

	/**
	 * This Method creats a new data block.
	 *
	 * @param newblocklength
	 *            Datalength of the new block
	 * @return The newly created block.
	 * @throws IOException
	 *             If an I/O-Error occurs.
	 */
	public DTBLOCK abstractcreate(long newblocklength) throws IOException {
		DTBLOCK ret;
		ret = ps.create(newblocklength);
		return ret;
	}

	/**
	 * Reads length bytes with <code>reader</code> and writes them out. This
	 * happens in smaller blocks.
	 *
	 * @param length
	 *            The number of bytes to copy.
	 * @throws IOException
	 *             If an I/O-Error occurs.
	 * @throws DataFormatException
	 *             If zipped data is in an invalid format.
	 */
	public void abstractcopy(long length)
			throws IOException, DataFormatException {
		long written = 0L;
		do {
			int bytesread = 0;
			if (written
					+ MDFAbstractProcessWriter.MAX_OUTPUTBLOCKSIZE > length) {
				bytesread = (int) (length - written);
				ByteBuffer custombuffer = abstractread(bytesread);
				abstractput(custombuffer, bytesread);
			} else {
				ByteBuffer buffer = abstractread(
						MDFAbstractProcessWriter.MAX_OUTPUTBLOCKSIZE);
				bytesread = MDFAbstractProcessWriter.MAX_OUTPUTBLOCKSIZE;
				abstractput(buffer, bytesread);
			}
			written += bytesread;
		} while (written < length);
		if (length != written) {
			throw new IOException("written length not equal to blocklength: "
					+ length + "/" + written);
		}
	}

	public void setLinks() {
		if (parentnode instanceof DGBLOCK) {
			parentnode.setLink(3, curr);
		} else {
			System.err.println(
					"Unable to set link to data block. Parent block not recognized.");
		}
	}

	/**
	 * ONLY FOR TESTING! Used in MDFUnsorter, replaces this splitmerger's
	 * provider.
	 *
	 * @param prov
	 *            The new data provider.
	 */
	public void setProv(MDF3DataProvider prov) {
		this.prov = prov;
	}

}
