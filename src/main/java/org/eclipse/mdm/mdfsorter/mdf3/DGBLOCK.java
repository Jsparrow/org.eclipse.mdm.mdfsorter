/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter.mdf3;

import java.io.IOException;

import org.eclipse.mdm.mdfsorter.MDFParser;

/**
 * The Data Group Block
 *
 * @author Christian Rechner, Tobias Leemann
 *
 */
public class DGBLOCK extends MDF3GenBlock {

	/** Data section */

	// Number of Bytes used for record IDs in the data block.
	// 0 = data records without record ID (only possible for sorted data group)
	// 1 = record ID (UINT8) before each data record
	// 2 = record ID (UINT8) before and after each data record
	// UINT16
	private int numberOfRecId;

	/**
	 * Number of Channel Groups UINT16
	 */
	private int channelGroups;

	/**
	 * Parse a HLBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public DGBLOCK(MDF3GenBlock parent) {
		super(parent.getPos(), parent.isBigEndian());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	public DGBLOCK(boolean bigendian) {
		super(bigendian);
		setId("DG");
		setLength(28);
		setLinkCount(4);
	}

	public MDF3GenBlock getLnkDgNext() {
		// same in MDF3 and MDF4
		return links[0];
	}

	public MDF3GenBlock getLnkCgFirst() {
		// same in MDF3 and MDF4
		return links[1];
	}

	public MDF3GenBlock getLnkTrTrigger() {
		return links[2];
	}

	public MDF3GenBlock getLnkData() {
		return links[3];
	}

	public int getNumOfRecId() {
		return numberOfRecId;
	}

	public void setNumOfRecId(int numOfrecId) {
		numberOfRecId = numOfrecId;
	}

	public int getChannelGroups() {
		return channelGroups;
	}

	public void setChannelGroups(int channelGroups) {
		this.channelGroups = channelGroups;
	}

	@Override
	public String toString() {
		return "DGBLOCK [numberOfRecId=" + numberOfRecId + ", channelGroups="
				+ channelGroups + "]";
	}

	@Override
	public void parse(byte[] content) throws IOException {
		// UNINT16 Number of Channel Groups
		setChannelGroups(MDF3Util.readUInt16(
				MDFParser.getDataBuffer(content, 0, 2), isBigEndian()));

		// UINT16 RecordIDLayout
		setNumOfRecId(MDF3Util.readUInt16(
				MDFParser.getDataBuffer(content, 2, 4), isBigEndian()));

	}

	@Override
	public byte[] getBodyBytes() {
		int arraylen = 8;

		byte[] ret = new byte[arraylen];

		byte[] cgs = MDF3Util.getBytesUInt16(channelGroups, isBigEndian());
		System.arraycopy(cgs, 0, ret, 0, 2);

		byte[] numrecid = MDF3Util.getBytesUInt16(numberOfRecId, isBigEndian());
		System.arraycopy(numrecid, 0, ret, 2, 2);

		return ret;
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}