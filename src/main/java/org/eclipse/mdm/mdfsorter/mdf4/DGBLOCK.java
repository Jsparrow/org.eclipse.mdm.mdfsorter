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

import org.eclipse.mdm.mdfsorter.MDFParser;

/**
 * The Data Group Block
 *
 * @author Christian Rechner, Tobias Leemann
 *
 */
public class DGBLOCK extends MDF4GenBlock {

	/** Data section */

	// Number of Bytes used for record IDs in the data block.
	// 0 = data records without record ID (only possible for sorted data group)
	// 1 = record ID (UINT8) before each data record
	// 2 = record ID (UINT16, LE Byte order) before each data record
	// 4 = record ID (UINT32, LE Byte order) before each data record
	// 8 = record ID (UINT64, LE Byte order) before each data record
	// UINT8
	private byte recIdSize;

	/**
	 * Parse a HLBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public DGBLOCK(MDF4GenBlock parent) {
		super(parent.getPos());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	public DGBLOCK() {
		setId("##DG");
		setLength(64L);
		setLinkCount(4);
	}

	public MDF4GenBlock getLnkDgNext() {
		// same in MDF3 and MDF4
		return links[0];
	}

	public MDF4GenBlock getLnkCgFirst() {
		// same in MDF3 and MDF4
		return links[1];
	}

	public MDF4GenBlock getLnkData() {
		return links[2];
	}

	public MDF4GenBlock getLnkMdComment() {
		return links[3];
	}

	public byte getRecIdSize() {
		return recIdSize;
	}

	public void setRecIdSize(byte recIdSize) {
		this.recIdSize = recIdSize;
	}

	/**
	 * {@inheritDoc}
	 *
	 */
	@Override
	public String toString() {
		return new StringBuilder().append("DGBLOCK [lnkDgNext=").append(getLnkDgNext().getPos()).append(", lnkCgFirst=").append(getLnkCgFirst().getPos()).append(", lnkData=").append(getLnkData().getPos())
				.append(", lnkMdComment=").append(getLnkMdComment().getPos()).append(", recIdSize=").append(recIdSize).append("]").toString();
	}

	/**
	 * @see MDF4GenBlock#parse(byte[])
	 */
	@Override
	public void parse(byte[] content) throws IOException {

		// UINT8: Number of Bytes used for record IDs in the data block.
		setRecIdSize(MDF4Util.readUInt8(MDFParser.getDataBuffer(content, 0, 1)));

	}

	@Override
	public byte[] getBodyBytes() {
		// UINT8: Number of Bytes used for record IDs in the data block.
		byte[] ret = new byte[8];
		ret[0] = getRecIdSize();
		return ret;
	}

}
