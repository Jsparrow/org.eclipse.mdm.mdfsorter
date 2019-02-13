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


package org.eclipse.mdm.mdfsorter.mdf3;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.eclipse.mdm.mdfsorter.MDFParser;

/**
 * The Channel Group Block
 *
 * @author Christian Rechner, Tobias Leemann
 *
 */
public class CGBLOCK extends MDF3GenBlock {

	/** Data section */

	// Record ID, value must be less than maximum unsigned integer value allowed
	// by dg_rec_id_size in parent DGBLOCK.
	// UINT16
	private int recordId;

	// Number of Channels in the Channel Group
	// UINT16
	private int numChannels;

	// Number of data Bytes (after record ID) used for signal values in record,
	// i.e. size of plain data for each
	// recorded sample of this channel group.
	// UINT16
	private int dataBytes;

	// Number of cycles, i.e. number of samples for this channel group.
	// This specifies the number of records of this type in the data block.
	// UINT32
	private long cycleCount;

	/**
	 * Parse a CGBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public CGBLOCK(MDF3GenBlock parent) {
		super(parent.getPos(), parent.isBigEndian());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	public MDF3GenBlock getLnkCgNext() {
		return links[0];
	}

	public MDF3GenBlock getLnkCnFirst() {
		return links[1];
	}

	public MDF3GenBlock getLnkTxComment() {
		return links[2];
	}

	public MDF3GenBlock getLnkSrFirst() {
		if (links.length < 3) {
			return null;
		}
		return links[3];
	}

	public int getRecordId() {
		return recordId;
	}

	public long getCycleCount() {
		return cycleCount;
	}

	public int getDataBytes() {
		return dataBytes;
	}

	public void setCycleCount(long cycleCount) {
		this.cycleCount = cycleCount;
	}

	public int getNumChannels() {
		return numChannels;
	}

	public void setNumChannels(int numChannels) {
		this.numChannels = numChannels;
	}

	public void setRecordId(int recordId) {
		this.recordId = recordId;
	}

	public void setDataBytes(int dataBytes) {
		this.dataBytes = dataBytes;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("CGBLOCK [recordId=").append(recordId).append(", cycleCount=").append(cycleCount).append(", dataBytes=").append(dataBytes).append(", numChannels=")
				.append(numChannels).append("]").toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.mdm.mdfsorter.mdf3.MDF3GenBlock#parse(byte[])
	 */
	@Override
	public void parse(byte[] content) throws IOException {
		// UINT16: Record ID
		setRecordId(MDF3Util.readUInt16(MDFParser.getDataBuffer(content, 0, 2), isBigEndian()));

		// UINT16: Number of Channel
		setNumChannels(MDF3Util.readUInt16(MDFParser.getDataBuffer(content, 2, 4), isBigEndian()));

		// UINT16: Number of data Bytes (after record ID) used for signal values
		// in record.
		setDataBytes(MDF3Util.readUInt16(MDFParser.getDataBuffer(content, 4, 6), isBigEndian()));

		// UINT32: Number of cycles
		setCycleCount(MDF3Util.readUInt32(MDFParser.getDataBuffer(content, 6, 10), isBigEndian()));
	}

	@Override
	public byte[] getHeaderBytes() {
		byte[] ret = super.getHeaderBytes();
		if (getLinkCount() == 4) {
			// forth link is not in the header. its space will be reserved in
			// body.
			byte[] newret = new byte[ret.length - 4];
			System.arraycopy(ret, 0, newret, 0, newret.length);
			ret = newret;
		}
		return ret;
	}

	@Override
	public byte[] getBodyBytes() {
		int arraylen = 10;
		if (getLinkCount() == 4) {
			arraylen = 14;
		}

		byte[] ret = new byte[arraylen];

		// UINT16 RecordID
		byte[] recID = MDF3Util.getBytesUInt16(getRecordId(), isBigEndian());
		System.arraycopy(recID, 0, ret, 0, 2);

		// UINT16 Number of Channels
		byte[] channelCount = MDF3Util.getBytesUInt16(getNumChannels(), isBigEndian());
		System.arraycopy(channelCount, 0, ret, 2, 2);

		// UINT16 Size of a record in bytes
		byte[] databytes = MDF3Util.getBytesUInt16(getDataBytes(), isBigEndian());
		System.arraycopy(databytes, 0, ret, 4, 2);

		// UINT32
		byte[] cyccount = MDF3Util.getBytesUInt32(getCycleCount(), isBigEndian());
		System.arraycopy(cyccount, 0, ret, 6, 4);

		// Last link address!
		if (getLinkCount() == 4) {
			byte[] lnk = MDF3Util.getBytesLink(links[3].getOutputpos(), isBigEndian());
			System.arraycopy(lnk, 0, ret, 10, 2);
		}
		// LINK
		return ret;
	}

	@Override
	public void updateLinks(RandomAccessFile r) throws IOException {
		// set position to start of Block link section
		r.seek(getOutputpos() + 4L);
		MDF3GenBlock linkedblock;
		// Update first three blocks normally
		for (int i = 0; i < 3; i++) {
			if ((linkedblock = getLink(i)) != null) {
				r.write(MDF3Util.getBytesLink((int) linkedblock.getOutputpos(), isBigEndian()));
			} else {
				r.write(MDF3Util.getBytesLink(0, isBigEndian()));
			}
		}
		// update fourth link manually
		if (getLinkCount() != 4) {
			return;
		}
		r.seek(getOutputpos() + 4L + 3L * 4L + 10L);
		r.write(MDF3Util.getBytesLink(getLink(4).getOutputpos(), isBigEndian()));
	}

}
