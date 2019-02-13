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

import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.mdm.mdfsorter.MDFParser;

/**
 * The Channel Group Block
 *
 * @author Christian Rechner, Tobias Leemann
 *
 */
public class CGBLOCK extends MDF4GenBlock {

	/** Data section */

	// Record ID, value must be less than maximum unsigned integer value allowed
	// by dg_rec_id_size in parent DGBLOCK.
	// UINT64
	private long recordId;

	// Number of cycles, i.e. number of samples for this channel group.
	// This specifies the number of records of this type in the data block.
	// UINT64
	private long cycleCount;

	// Flags
	// The value contains the following bit flags (Bit 0 = LSB):
	// Bit 0: VLSD channel group flag.
	// Bit 1: Bus event channel group flag
	// Bit 2: Plain bus event channel group flag
	// UINT16
	private int flags;

	// Value of character to be used as path separator, 0 if no path separator
	// specified.
	// UINT16
	private int pathSeparator;

	// Normal CGBLOCK:
	// Number of data Bytes (after record ID) used for signal values in record,
	// i.e. size of plain data for each
	// recorded sample of this channel group.
	// VLSD CGBLOCK:
	// Low part of a UINT64 value that specifies the total size in Bytes of all
	// variable length signal values for the
	// recorded samples of this channel group.
	// UINT32
	private long dataBytes;

	// Normal CGBLOCK:
	// Number of additional Bytes for record used for invalidation bits. Can be
	// zero if no invalidation bits are used at
	// all. Invalidation bits may only occur in the specified number of Bytes
	// after the data Bytes, not within the data
	// Bytes that contain the signal values.
	// VLSD CGBLOCK:
	// High part of UINT64 value that specifies the total size in Bytes of all
	// variable length signal values for the
	// recorded samples of this channel group, i.e. the total size in Bytes can
	// be calculated by cg_data_bytes +
	// (cg_inval_bytes << 32)
	// Note: this value does not include the Bytes used to specify the length of
	// each VLSD value!
	private long invalBytes;

	/**
	 * Parse a HLBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public CGBLOCK(MDF4GenBlock parent) {
		super(parent.getPos());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	public MDF4GenBlock getLnkCgNext() {
		return links[0];
	}

	public MDF4GenBlock getLnkCnFirst() {
		return links[1];
	}

	public MDF4GenBlock getLnkTxAcqName() {
		return links[2];
	}

	public MDF4GenBlock getLnkSiAcqSource() {
		return links[3];
	}

	public MDF4GenBlock getLnkSrFirst() {
		return links[4];
	}

	public MDF4GenBlock getLnkMdComment() {
		return links[5];
	}

	public long getRecordId() {
		return recordId;
	}

	public long getCycleCount() {
		return cycleCount;
	}

	public int getFlags() {
		return flags;
	}

	public int getPathSeparator() {
		return pathSeparator;
	}

	public long getDataBytes() {
		return dataBytes;
	}

	public long getInvalBytes() {
		return invalBytes;
	}

	public void setRecordId(long recordId) {
		this.recordId = recordId;
	}

	public void setCycleCount(long cycleCount) {
		this.cycleCount = cycleCount;
	}

	private void setFlags(int flags) {
		this.flags = flags;
	}

	public void setPathSeparator(int pathSeparator) {
		this.pathSeparator = pathSeparator;
	}

	private void setDataBytes(long dataBytes) {
		this.dataBytes = dataBytes;
	}

	private void setInvalBytes(long invalBytes) {
		this.invalBytes = invalBytes;
	}

	public boolean isVLSDChannel() {
		return BigInteger.valueOf(flags).testBit(0);
	}

	/**
	 * Get the number of Bytes that are used in Variable Length Signal Data
	 * (VLSD) values This value is calculated from dataBytes and invalBytes.
	 * (see Specification)
	 *
	 * @return The value.
	 */
	public long getVLSDlength() {
		return dataBytes + (invalBytes << 32);
	}

	public void setIsVlSD(boolean isVLSD) {
		if (isVLSD) {
			flags = flags | 0x1;
		} else {
			flags = flags & -2;
		}
	}

	/**
	 * Returns a list with all VLSDChannels in this group
	 *
	 * @return The list of CNBLOCKs.
	 */
	public List<CNBLOCK> getVLSDChannels() {
		LinkedList<CNBLOCK> ret = new LinkedList<>();
		CNBLOCK next = (CNBLOCK) getLnkCnFirst();
		while (next != null) {
			// check if vlsd channel
			if (next.getChannelType() == 1) {
				ret.add(next);
			}
			next = (CNBLOCK) next.getLnkCnNext();
		}
		return ret;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("CGBLOCK [recordId=").append(recordId).append(", cycleCount=").append(cycleCount).append(", flags=").append(flags).append(", pathSeparator=")
				.append(pathSeparator).append(", dataBytes=").append(dataBytes).append(", invalBytes=").append(invalBytes).append("]").toString();
	}

	/**
	 * @see MDF4GenBlock#parse(byte[])
	 */
	@Override
	public void parse(byte[] content) throws IOException {

		// UINT64: Record ID
		setRecordId(MDF4Util.readUInt64(MDFParser.getDataBuffer(content, 0, 8)));

		// UINT64: Number of cycles
		setCycleCount(MDF4Util.readUInt64(MDFParser.getDataBuffer(content, 8, 16)));

		// UINT16: Flags
		setFlags(MDF4Util.readUInt16(MDFParser.getDataBuffer(content, 16, 18)));

		// UINT16: Value of character to be used as path separator, 0 if no path
		// separator specified.
		setPathSeparator(MDF4Util.readUInt16(MDFParser.getDataBuffer(content, 18, 20)));

		// UINT32: Number of data Bytes (after record ID) used for signal values
		// in record.
		setDataBytes(MDF4Util.readUInt32(MDFParser.getDataBuffer(content, 24, 28)));

		// UINT32: Number of additional Bytes for record used for invalidation
		// bits.
		setInvalBytes(MDF4Util.readUInt32(MDFParser.getDataBuffer(content, 28, 32)));
	}

	@Override
	public byte[] getBodyBytes() {
		int arraylen = 32;
		byte[] ret = new byte[arraylen];

		byte[] recID = MDF4Util.getBytesUInt64(getRecordId());
		System.arraycopy(recID, 0, ret, 0, 8);

		byte[] cyccount = MDF4Util.getBytesUInt64(getCycleCount());
		System.arraycopy(cyccount, 0, ret, 8, 8);

		byte[] flags = MDF4Util.getBytesUInt16(getFlags());
		System.arraycopy(flags, 0, ret, 16, 2);

		byte[] pathsep = MDF4Util.getBytesUInt16(getPathSeparator());
		System.arraycopy(pathsep, 0, ret, 18, 2);

		// 4 Spacer bytes

		byte[] databytes = MDF4Util.getBytesUInt32(getDataBytes());
		System.arraycopy(databytes, 0, ret, 24, 4);

		byte[] invalbytes = MDF4Util.getBytesUInt32(getInvalBytes());
		System.arraycopy(invalbytes, 0, ret, 28, 4);

		return ret;
	}

}
