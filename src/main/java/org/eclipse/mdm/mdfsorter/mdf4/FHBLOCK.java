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
 * The File History Block.
 *
 * @author Tobias Leemann
 *
 */
public class FHBLOCK extends MDF4GenBlock {
	/** Data section */

	// Time stamp of modification, in ns since 1/1/1970
	// UINT64
	private long time_ns;

	// Time zone offset in minutes
	// INT16
	private int tz_offset_min;

	// Dayligth saving time zone offset in minutes
	// INT16
	private int dst_offset_min;

	// Time flags Bit 0: Local Time flag / Bit 1: Time offset valid flag
	// UINT8
	private byte time_flags;

	/**
	 * Constructor
	 */
	public FHBLOCK() {
		super(0);
		setLength(56L);
		setLinkCount(2);
		setId("##FH");
	}

	/**
	 * Parse a FHBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public FHBLOCK(MDF4GenBlock parent) {
		super(parent.getPos());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	public long getTime_ns() {
		return time_ns;
	}

	public void setTime_ns(long time_ns) {
		this.time_ns = time_ns;
	}

	public int getTz_offset_min() {
		return tz_offset_min;
	}

	public void setTz_offset_min(int tz_offset_min) {
		this.tz_offset_min = tz_offset_min;
	}

	public int getDst_offset_min() {
		return dst_offset_min;
	}

	public void setDst_offset_min(int dst_offset_min) {
		this.dst_offset_min = dst_offset_min;
	}

	public byte getTime_flags() {
		return time_flags;
	}

	public void setTime_flags(byte time_flags) {
		this.time_flags = time_flags;
	}

	@Override
	public void parse(byte[] content) throws IOException {
		setTime_ns(MDF4Util.readUInt64(MDFParser.getDataBuffer(content, 0, 8)));
		setTz_offset_min(
				MDF4Util.readInt16(MDFParser.getDataBuffer(content, 8, 10)));
		setDst_offset_min(
				MDF4Util.readInt16(MDFParser.getDataBuffer(content, 10, 12)));
		setTime_flags(
				MDF4Util.readUInt8(MDFParser.getDataBuffer(content, 12, 13)));
	}

	@Override
	public byte[] getBodyBytes() throws IOException {
		int arraylen = 16;

		byte[] ret = new byte[arraylen];

		byte[] time_ns = MDF4Util.getBytesUInt64(getTime_ns());
		System.arraycopy(time_ns, 0, ret, 0, 8);

		byte[] tz_offset = MDF4Util.getBytesInt16(getTz_offset_min());
		System.arraycopy(tz_offset, 0, ret, 8, 2);

		byte[] dst_offset = MDF4Util.getBytesInt16(getDst_offset_min());
		System.arraycopy(dst_offset, 0, ret, 10, 2);

		ret[11] = time_flags;

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
