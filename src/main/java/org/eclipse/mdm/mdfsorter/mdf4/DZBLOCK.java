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
 * The Data Zipped Block. A DZBLOCK stores zipped Data of a DT, SD or DR block
 * (see DT Block).
 *
 * @author Tobias Leemann
 *
 */
public class DZBLOCK extends MDF4GenBlock {

	/** Data section */

	// Block Type, Contains type of compressed Block ("DT|"SD"|"RD")
	// CHAR (2x)
	private String block_type;

	// Zip algorithm used to compress the data (0=Deflate; 1=Transpose and
	// Deflate)
	// UINT8
	private byte zip_type;

	// Parameters for zip algorithm
	// UINT32
	private long zip_parameters;

	// Length of uncompressed Data (should not exeed 4MB)
	// UINT64
	private long org_data_length;

	// Length of compressed Data
	// UINT64
	private long data_length;

	/**
	 * Constructor.
	 */
	public DZBLOCK() {
		super(0);
		setId("##DZ");
	}

	/**
	 * Parse a HLBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public DZBLOCK(MDF4GenBlock parent) {
		super(parent.getPos());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	public String getBlock_type() {
		return block_type;
	}

	public void setBlock_type(String block_type) {
		this.block_type = block_type;
	}

	public byte getZip_type() {
		return zip_type;
	}

	public void setZip_type(byte zip_type) {
		this.zip_type = zip_type;
	}

	public long getZip_parameters() {
		return zip_parameters;
	}

	public void setZip_parameters(long zip_parameters) {
		this.zip_parameters = zip_parameters;
	}

	public long getOrg_data_length() {
		return org_data_length;
	}

	public void setOrg_data_length(long org_data_length) {
		this.org_data_length = org_data_length;
	}

	public long getData_length() {
		return data_length;
	}

	public void setData_length(long data_length) {
		this.data_length = data_length;
	}

	public boolean transposeNeeded() {
		return zip_type == 1;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.mdm.mdf4sorter.MDFGenBlock#parse(byte[])
	 */
	@Override
	public void parse(byte[] content) throws IOException {
		setBlock_type(MDF4Util.readCharsUTF8(MDFParser.getDataBuffer(content, 0, 2), 2));

		setZip_type(MDF4Util.readUInt8(MDFParser.getDataBuffer(content, 2, 3)));

		setZip_parameters(MDF4Util.readUInt32(MDFParser.getDataBuffer(content, 4, 8)));

		setOrg_data_length(MDF4Util.readUInt64(MDFParser.getDataBuffer(content, 8, 16)));

		setData_length(MDF4Util.readUInt64(MDFParser.getDataBuffer(content, 16, 24)));

	}

	@Override
	public byte[] getBodyBytes() throws IOException {
		int arraylen = 24;

		byte[] ret = new byte[arraylen];

		byte[] type = MDF4Util.getBytesCharsUTF8(getBlock_type());
		System.arraycopy(type, 0, ret, 0, 2);

		ret[3] = zip_type;

		byte[] zipparam = MDF4Util.getBytesUInt32(zip_parameters);
		System.arraycopy(zipparam, 0, ret, 4, 4);

		byte[] orgdlen = MDF4Util.getBytesUInt64(org_data_length);
		System.arraycopy(orgdlen, 0, ret, 8, 8);

		byte[] dlen = MDF4Util.getBytesUInt64(data_length);
		System.arraycopy(dlen, 0, ret, 16, 8);

		return ret;
	}

}
