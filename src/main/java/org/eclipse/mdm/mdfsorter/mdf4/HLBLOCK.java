/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter.mdf4;

import java.io.IOException;

import org.eclipse.mdm.mdfsorter.MDFParser;

/**
 * The Header List Block HLBLOCK, list header for a datalist (DLBLOCK),
 * containing compressed DZBLOCKS
 *
 * @author Tobias Leemann
 *
 */
public class HLBLOCK extends MDF4GenBlock {

	/** Data section */

	// flags of this HLBLOCK, BIT0 = equallength flag, must be equal for all
	// DLBLOCKS in the list
	// UINT16
	private int flags;

	// Zip algorithm used to compress the data (0=Deflate; 1=Transpose and
	// Deflate)
	// UINT8
	private byte ziptype;

	public HLBLOCK() {
		super(0);
		setId("##HL");
		setLinkCount(1);
		setLength(40L);
	}

	/**
	 * Parse a HLBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public HLBLOCK(MDF4GenBlock parent) {
		super(parent.getPos());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public byte getZiptype() {
		return ziptype;
	}

	public void setZiptype(byte ziptype) {
		this.ziptype = ziptype;
	}

	public MDF4GenBlock getLnkDlFirst() {
		return links[0];
	}

	public boolean isEquallength() {
		return (flags & (byte) 1) != 0;
	}

	@Override
	public void parse(byte[] content) throws IOException {
		setFlags(MDF4Util.readUInt16(MDFParser.getDataBuffer(content, 0, 2)));
		setZiptype(MDF4Util.readUInt8(MDFParser.getDataBuffer(content, 2, 3)));
	}

	@Override
	public byte[] getBodyBytes() throws IOException {
		int arraylen = 8;

		byte[] ret = new byte[arraylen];

		byte[] hl_flags = MDF4Util.getBytesUInt16(flags);
		System.arraycopy(hl_flags, 0, ret, 0, 2);

		ret[3] = ziptype;

		return ret;
	}

}
