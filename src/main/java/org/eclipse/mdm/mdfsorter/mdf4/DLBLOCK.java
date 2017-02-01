/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter.mdf4;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import org.eclipse.mdm.mdfsorter.ArgumentStruct;
import org.eclipse.mdm.mdfsorter.MDFParser;

/**
 * The Data List Block The DLBLOCK references a list of data blocks (DTBLOCK) or
 * a list of signal data blocks (SDBLOCK) or a list of reduction data blocks
 * (RDBLOCK). This list of blocks is equivalent to using a single
 * (signal/reduction) data block and can be used to avoid a huge data block by
 * splitting it into smaller parts.
 *
 * @author Christian Rechner, Tobias Leemann
 */
public class DLBLOCK extends MDF4GenBlock {

	// public static String BLOCK_ID = "##DL";

	/** Data section */

	// Flags
	// Bit 0: Equal length flag
	// UINT8
	private byte flags;

	// Number of referenced blocks N
	// UINT32
	private long count;

	// Only present if "equal length" flag (bit 0 in dl_flags) is set.
	// Equal data section length.
	// UINT64
	private long equalLength;

	// Only present if "equal length" flag (bit 0 in dl_flags) is not set.
	// Start offset (in Bytes) for the data section of each referenced block.
	// UINT64
	private long[] offset;

	public DLBLOCK() {
		super(0);
		setId("##DL");
	}

	/**
	 * Parse a HLBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public DLBLOCK(MDF4GenBlock parent) {
		super(parent.getPos());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	public MDF4GenBlock getLnkDlNext() {
		return links[0];
	}

	public MDF4GenBlock[] getLnkDlData() {
		return Arrays.copyOfRange(links, 1, links.length);
	}

	public byte getFlags() {
		return flags;
	}

	public long getCount() {
		return count;
	}

	public long getEqualLength() {
		return equalLength;
	}

	public long[] getOffset() {
		return offset;
	}

	public void setFlags(byte flags) {
		this.flags = flags;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public void setEqualLength(long equalLength) {
		this.equalLength = equalLength;
	}

	public void setOffset(long[] offset) {
		this.offset = offset;
	}

	public boolean isEqualLengthFlag() {
		return BigInteger.valueOf(flags).testBit(0);
	}

	@Override
	public void setLinkCount(long linkCount) {
		offset = new long[(int) linkCount - 1];
		super.setLinkCount(linkCount);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "DLBLOCK [lnkDlNext=" + getLnkDlNext() + ", lnkDlData="
				+ Arrays.toString(getLnkDlData()) + ", flags=" + flags
				+ ", count=" + count + ", equalLength=" + equalLength
				+ ", offset=" + Arrays.toString(offset) + "]";
	}

	@Override
	public void parse(byte[] content) throws IOException {
		setFlags(MDF4Util.readUInt8(MDFParser.getDataBuffer(content, 0, 1)));
		setCount(MDF4Util.readUInt32(MDFParser.getDataBuffer(content, 4, 8)));
		if (isEqualLengthFlag()) {
			setEqualLength(MDF4Util
					.readUInt64(MDFParser.getDataBuffer(content, 8, 16)));
		} else {
			long[] offset = new long[(int) getCount()];
			for (int i = 0; i < offset.length; i++) {
				offset[i] = MDF4Util.readUInt64(MDFParser.getDataBuffer(content,
						8 + 8 * i, 16 + 8 * i));
			}
			setOffset(offset);
		}
	}

	@Override
	public byte[] getBodyBytes() {
		int arraylen = 0;
		if (isEqualLengthFlag()) {
			arraylen = 4 + 4 + 8;
		} else {
			arraylen = (int) (4 + 4 + 8 * getCount());
		}
		byte[] ret = new byte[arraylen];

		// flags UINT8
		ret[0] = flags;

		byte[] count = MDF4Util.getBytesUInt32(this.count);
		System.arraycopy(count, 0, ret, 4, 4);

		if (isEqualLengthFlag()) {
			byte[] eqlen = MDF4Util.getBytesUInt64(equalLength);
			System.arraycopy(eqlen, 0, ret, 8, 8);
		} else {
			int offset = 8;
			for (int i = 0; i < this.count; i++) {
				byte[] dtoffset = MDF4Util.getBytesUInt64(this.offset[i]);
				System.arraycopy(dtoffset, 0, ret, offset, 8);
				offset += 8;
			}
		}

		return ret;
	}

	/**
	 * Checks if this list can be improved by merging blocks.
	 *
	 * @param args
	 *            The programm arguments for this call
	 * @return True, if fewer blocks or an improvement is possible. False if
	 *         not.
	 */
	public boolean isImproveable(ArgumentStruct args) {
		if(getCount() == 0){
			return true;
		}
		// A data list that only contains one block is useless.
		if (getCount() == 1
				&& getLink(1).getLength() < args.maxblocksize) {
			return true;
		}
		// Split blocks if necessary
		if (args.overrideOldSize) {
			return true;
		}
		// check if list contains unzipped data but zipping is needed.
		if (!args.unzip) {
			DLBLOCK curr = this;
			do {
				for (int i = 0; i < curr.count; i++) {
					String chldid = curr.getLink(i + 1).getId();
					if (chldid.equals("##DT") || chldid.equals("##RD")
							|| chldid.equals("##SD")) {
						return true;
					}
				}
			} while ((curr = (DLBLOCK) curr.getLink(0)) != null);
		}

		// check if fewer block are possible
		if (isEqualLengthFlag()) {
			if (getEqualLength() >= args.maxblocksize) {
				return false;
			} else {
				// list consists of smaller blocks than possible
				return true;
			}
		} else {
			if (getLnkDlNext() != null) {
				// list consists of another list (bad!)
				return true;
			} else {
				long datasectionlength = getOffset()[(int) (getCount() - 1)]; // Calculate
				// last
				// offset
				datasectionlength += getLink((int) getCount()).getLength()
						- 24L; // ... and add length of last block
				if (datasectionlength / args.maxblocksize + 1 < getCount()) {
					// fewer blocks are possible
					return true;
				} else {
					return false;
				}
			}
		}
	}
}
