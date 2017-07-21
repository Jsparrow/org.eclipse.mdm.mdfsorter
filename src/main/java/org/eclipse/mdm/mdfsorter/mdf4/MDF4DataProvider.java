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
public class MDF4DataProvider implements AbstractDataProvider {

	private FileChannel reader;
	private MDF4GenBlock datasectionhead;

	/**
	 * What type of Data section is given? 'l' = linked list, not zipped. 'h' =
	 * linked list, zipped. 'd' = normal data block;. 'z' = zipped data block
	 */
	private final char sectype;

	private ZippedDataCache cache;

	private ReadDataCache ReadCache;
	/**
	 * LastProcessed block.
	 */
	private MDF4GenBlock lastprocessed;

	/**
	 * LastProcessed block's start in this data section
	 */
	private long lastprocessedstart;

	/**
	 * LastProcessed block's start in this data section
	 */
	private long lastprocessedend;

	/**
	 * Length of this data section.
	 */
	private long sectionlength;

	private byte[] dataarr;

	/**
	 * This constructer creates a new DataProvider with the given head. Possible
	 * blocktypes are: ##HL, ##DL, ##DZ, ##DT, ##SD, ##RD
	 *
	 * @param datasectionhead
	 *            The header of the data section
	 * @param reader
	 *            FileChannel to the input file.
	 */
	public MDF4DataProvider(MDF4GenBlock datasectionhead, FileChannel reader) {
		// empty data section
		if (datasectionhead == null) {
			sectype = '0';
			return;
		}
		this.datasectionhead = datasectionhead;
		this.reader = reader;
		cache = new ZippedDataCache(reader);

		switch (datasectionhead.getId()) {
		case "##DT":
		case "##SD":
		case "##RD":
			sectype = 'd';
			break;
		case "##DL":
			sectype = 'l';
			break;
		case "##HL":
			sectype = 'h';
			// skip HL-Block
			this.datasectionhead = this.datasectionhead.getLink(0);
			break;
		case "##DZ":
			sectype = 'z';
			break;
		default:
			sectype = '0';
			System.err.println("Unknown blocktype in DataProvider.");
		}

		sectionlength = calculateLength();

		cache = new ZippedDataCache(reader);
		ReadCache = new ReadDataCache(this);

	}

	public MDF4DataProvider(byte[] data) {
		dataarr = data;
		sectype = '0';
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
	 *             If an I/O error occurs.
	 * @throws DataFormatException
	 *             If zipped data is in an invalid format.
	 */
	@Override
	public void read(long globaloffset, ByteBuffer data) throws IOException, DataFormatException {

		if (globaloffset + data.capacity() > sectionlength) {
			throw new IllegalArgumentException(
					"Invalid read access on Data Provider. Section is only " + sectionlength + " bytes long.");
		}
		if (dataarr != null) {
			data.put(dataarr, (int) globaloffset, data.capacity());
			data.rewind();
			return;
		}

		MDF4GenBlock blk = null;
		long blkoffset = 0;

		// check if block was last processed block (performance optimization)
		if (lastprocessed != null) {
			if (lastprocessedstart <= globaloffset && lastprocessedend > globaloffset) {
				blk = lastprocessed;
				blkoffset = globaloffset - lastprocessedstart;
			}
		}

		// we need to search for the block to read from
		if (blk == null) {
			Object[] bo = getBlockWithOffset(globaloffset);
			blk = (MDF4GenBlock) bo[0];
			blkoffset = (long) bo[1];
			lastprocessedstart = (long) bo[2];
			lastprocessed = blk;
			long lpdatasize = lastprocessed instanceof DZBLOCK ? ((DZBLOCK) lastprocessed).getOrg_data_length()
					: lastprocessed.getLength() - 24L;
			lastprocessedend = lastprocessedstart + lpdatasize;
		}

		// length check
		long datasize = blk instanceof DZBLOCK ? ((DZBLOCK) blk).getOrg_data_length() : blk.getLength() - 24L;
		if (blkoffset + data.capacity() > datasize) {
			int readablesize = (int) (datasize - blkoffset);
			ByteBuffer readable = ByteBuffer.allocate(readablesize);

			// divide and conquer: Read available section first
			read(globaloffset, readable);

			// read unavailable section
			ByteBuffer unreadable = ByteBuffer.allocate(data.capacity() - readablesize);
			read(globaloffset + readablesize, unreadable);

			// merge sections
			data.put(readable);
			data.put(unreadable);
			data.rewind();
			return;
		}

		if (blk.getId().equals("##DZ")) {
			cache.read((DZBLOCK) blk, blkoffset, data);
		} else {
			reader.position(blk.getPos() + 24L + blkoffset);
			reader.read(data);
		}
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
	 *             If an I/O error occurs.
	 * @throws DataFormatException
	 *             If zipped data is in an invalid format.
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
			return ReadCache.read(globaloffset, length);
		}

	}

	/**
	 * Reads data.limit() bytes from Block blk, starting at blockoffset, from
	 * the start of blk's data. Using this Method is more efficent if the block,
	 * that the section to read from is contained in, is already known.
	 * (Blockwise read) This method does not use the read data cache.
	 *
	 * @param blockoffset
	 *            The offset to start reading in the block.
	 * @param data
	 *            The Bytebuffer that the data should be read in.
	 * @param blk
	 *            The block to read from.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws DataFormatException
	 *             If zipped data is in an invalid format.
	 */
	public void read(long blockoffset, ByteBuffer data, MDF4GenBlock blk) throws IOException, DataFormatException {
		// argument check
		long datalength = blk instanceof DZBLOCK ? ((DZBLOCK) blk).getOrg_data_length() : blk.getLength() - 24L;
		if (blockoffset + data.capacity() > datalength) {
			throw new IllegalArgumentException(
					"Invalid read access on Data Provider. Block is only " + datalength + " bytes long.");
		}

		if (blk.getId().equals("##DZ")) {
			cache.read((DZBLOCK) blk, blockoffset, data);
		} else {
			reader.position(blk.getPos() + 24L + blockoffset);
			reader.read(data);
			data.rewind();
		}
	}

	/**
	 * Returns the block which contains the data starting at globaloffset and
	 * the internal offset, where to start reading in the block.
	 *
	 * @param globaloffset
	 *            The offset that is looked for in this data section
	 * @return An Array with length 2. The Block (Type MDF4GenBlock) is stored
	 *         at index 0, the offset (long) at index 1.
	 */
	private Object[] getBlockWithOffset(long globaloffset) {
		Object[] ret = new Object[3];
		// No list walking needed, only a single block
		if (sectype == 'd' || sectype == 'z') {
			ret[0] = datasectionhead;
			ret[1] = globaloffset;
			ret[2] = 0L;
			return ret;
		} else {
			// list... Datasection head has to be a DL, because HLs are skipped
			// in the constructor
			DLBLOCK curr = (DLBLOCK) datasectionhead;
			if (curr.isEqualLengthFlag()) {
				long blknum = globaloffset / curr.getEqualLength(); // block
				// we
				// need
				long pastblocks = 0;
				DLBLOCK drag = null;

				// skip to next list if needed.
				while (pastblocks <= blknum) {
					pastblocks += curr.getCount();
					drag = curr;
					curr = (DLBLOCK) curr.getLink(0);
				}
				ret[0] = drag.getLink((int) (blknum - (pastblocks - drag.getCount()) + 1));
				ret[1] = globaloffset % drag.getEqualLength();
				ret[2] = drag.getEqualLength() * blknum;
			} else {
				long curroff = -1;
				int listblknum = 0; // Block number in active list
				DLBLOCK actlist = curr; // active list.
				MDF4GenBlock drag = null;
				long draglength = 0; // length of drag block.
				while (curroff <= globaloffset) {
					drag = actlist.getLink(listblknum + 1);
					draglength = drag.getId().equals("##DZ") ? ((DZBLOCK) drag).getOrg_data_length()
							: drag.getLength() - 24L;

					curroff = actlist.getOffset()[listblknum] + draglength;

					listblknum++;
					// switch to next list, if end is reached.
					if (listblknum == actlist.getCount()) {
						actlist = (DLBLOCK) actlist.getLnkDlNext();
						listblknum = 0;
					}
				}
				ret[0] = drag;
				ret[1] = globaloffset - (curroff - draglength);
				ret[2] = curroff - draglength;
			}
		}
		return ret;
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
		if (datasectionhead == null) {
			return 0;
		}
		long newlength = 0;
		if (sectype == 'd') {
			return datasectionhead.getLength() - 24L;
		} else if (sectype == 'z') {
			return ((DZBLOCK) datasectionhead).getOrg_data_length();
		} else {
			DLBLOCK drag = null;
			DLBLOCK blk = (DLBLOCK) datasectionhead;
			MDF4GenBlock lastchld;
			do {
				lastchld = blk.getLink((int) blk.getCount());
				newlength += (blk.getCount() - 1) * blk.getEqualLength();
				if (lastchld.getId().equals("##DZ")) {
					newlength += ((DZBLOCK) lastchld).getOrg_data_length();
				} else {
					newlength += lastchld.getLength() - 24L;
				}
				drag = blk;
			} while ((blk = (DLBLOCK) blk.links[0]) != null);

			if (!drag.isEqualLengthFlag()) {
				// We were wrong, calculate newlength otherwise (last offset +
				// length of last datablock)
				newlength = drag.getOffset()[(int) drag.getCount() - 1];
				if (lastchld.getId().equals("##DZ")) {
					newlength += ((DZBLOCK) lastchld).getOrg_data_length();
				} else {
					newlength += lastchld.getLength() - 24L;
				}
			}
		}
		return newlength;
	}

}
