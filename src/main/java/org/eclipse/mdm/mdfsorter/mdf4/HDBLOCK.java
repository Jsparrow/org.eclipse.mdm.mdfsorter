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

import org.eclipse.mdm.mdfsorter.MDFParser;

/**
 * The Header Block The HDBLOCK always begins at file position 64. It contains
 * general information about the contents of the measured data file and is the
 * root for the block hierarchy.
 *
 * @author Christian Rechner
 */
public class HDBLOCK extends MDF4GenBlock {

	/** Data section */

	// Time stamp at start of measurement in nanoseconds elapsed since 00:00:00
	// 01.01.1970 (UTC time or local time,
	// depending on "local time" flag, see [UTC]). All time stamps for time
	// synchronized master channels or events are
	// always relative to this start time stamp.
	// UINT64
	private long startTimeNs;

	// Time zone offset in minutes.
	// The value is not necessarily a multiple of 60 and can be negative! For
	// the current time zone definitions, it is
	// expected to be in the range [-840,840] min.
	// For example a value of 60 (min) means UTC+1 time zone = Central European
	// Time
	// Only valid if "time offsets valid" flag is set in time flags.
	// INT16
	private short tzOffsetMin;

	// Daylight saving time (DST) offset in minutes for start time stamp. During
	// the summer months, most regions observe
	// a DST offset of 60 min (1 hour).
	// Only valid if "time offsets valid" flag is set in time flags.
	// INT16
	private short dstOffsetMin;

	// Time flags
	// The value contains the following bit flags (Bit 0 = LSB):
	// Bit 0: Local time flag
	// If set, the start time stamp in nanoseconds represents the local time
	// instead of the UTC time, In this case, time
	// zone and DST offset must not be considered (time offsets flag must not be
	// set). Should only be used if UTC time
	// is unknown.
	// If the bit is not set (default), the start time stamp represents the UTC
	// time.
	// Bit 1: Time offsets valid flag
	// If set, the time zone and DST offsets are valid. Must not be set together
	// with "local time" flag (mutually
	// exclusive).
	// If the offsets are valid, the locally displayed time at start of
	// recording can be determined
	// (after conversion of offsets to ns) by
	// Local time = UTC time + time zone offset + DST offset.
	// UINT8
	private byte timeFlags;

	// Time quality class
	// 0 = local PC reference time (Default)
	// 10 = external time source
	// 16 = external absolute synchronized time
	// UINT8
	private byte timeClass;

	// Flags
	// The value contains the following bit flags (Bit 0 = LSB):
	// Bit 0: Start angle valid flag. If set, the start angle value below is
	// valid.
	// Bit 1: Start distance valid flag. If set, the start distance value below
	// is valid.
	// UINT8
	private byte flags;

	// Start angle in radians at start of measurement (only for angle
	// synchronous measurements)
	// Only valid if "start angle valid" flag is set.
	// REAL
	private double startAngleRad;

	// Start distance in meters at start of measurement
	// (only for distance synchronous measurements)
	// Only valid if "start distance valid" flag is set.
	// All distance values for distance synchronized master channels
	// REAL
	private double startDistanceM;

	/**
	 * Parse a HLBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public HDBLOCK(MDF4GenBlock parent) {
		super(parent.getPos());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	public MDF4GenBlock getLnkDgFirst() {
		return links[0];
	}

	public MDF4GenBlock getLnkFhFirst() {
		return links[1];
	}

	public MDF4GenBlock getLnkChFirst() {
		return links[2];
	}

	public MDF4GenBlock getLnkAtFirst() {
		return links[3];
	}

	public MDF4GenBlock getLnkEvFirst() {
		return links[4];
	}

	public MDF4GenBlock getLnkMdComment() {
		return links[5];
	}

	public long getStartTimeNs() {
		return startTimeNs;
	}

	private void setStartTimeNs(long startTimeNs) {
		this.startTimeNs = startTimeNs;
	}

	public short getTzOffsetMin() {
		return tzOffsetMin;
	}

	private void setTzOffsetMin(short tzOffsetMin) {
		this.tzOffsetMin = tzOffsetMin;
	}

	public short getDstOffsetMin() {
		return dstOffsetMin;
	}

	private void setDstOffsetMin(short dstOffsetMin) {
		this.dstOffsetMin = dstOffsetMin;
	}

	public byte getTimeFlags() {
		return timeFlags;
	}

	private void setTimeFlags(byte timeFlags) {
		this.timeFlags = timeFlags;
	}

	public byte getTimeClass() {
		return timeClass;
	}

	private void setTimeClass(byte timeClass) {
		this.timeClass = timeClass;
	}

	public byte getFlags() {
		return flags;
	}

	private void setFlags(byte flags) {
		this.flags = flags;
	}

	public double getStartAngleRad() {
		return startAngleRad;
	}

	private void setStartAngleRad(double startAngleRad) {
		this.startAngleRad = startAngleRad;
	}

	public double getStartDistanceM() {
		return startDistanceM;
	}

	private void setStartDistanceM(double startDistanceM) {
		this.startDistanceM = startDistanceM;
	}

	public boolean isLocalTime() {
		return BigInteger.valueOf(timeFlags).testBit(0);
	}

	public boolean isTimeFlagsValid() {
		return BigInteger.valueOf(timeFlags).testBit(1);
	}

	public boolean isStartAngleValid() {
		return BigInteger.valueOf(flags).testBit(0);
	}

	public boolean isStartDistanceValid() {
		return BigInteger.valueOf(flags).testBit(1);
	}

	@Override
	public String toString() {
		return "HDBLOCK [ startTimeNs=" + startTimeNs + ", tzOffsetMin=" + tzOffsetMin + ", dstOffsetMin="
				+ dstOffsetMin + ", timeFlags=" + timeFlags + ", timeClass=" + timeClass + ", flags=" + flags
				+ ", startAngleRad=" + startAngleRad + ", startDistanceM=" + startDistanceM + "]";
	}

	@Override
	public void parse(byte[] content) throws IOException {

		// UINT64: Time stamp at start of measurement in nanoseconds elapsed
		// since 00:00:00 01.01.1970
		setStartTimeNs(MDF4Util.readUInt64(MDFParser.getDataBuffer(content, 0, 8)));

		// INT16: Time zone offset in minutes.
		setTzOffsetMin(MDF4Util.readInt16(MDFParser.getDataBuffer(content, 8, 10)));

		// INT16: Daylight saving time (DST) offset in minutes for start time
		setDstOffsetMin(MDF4Util.readInt16(MDFParser.getDataBuffer(content, 10, 12)));

		// UINT8: Time flags block.setTimeFlags(MDF4Util.readUInt8(bb));
		setTimeFlags(MDF4Util.readUInt8(MDFParser.getDataBuffer(content, 12, 13)));

		// UINT8: Time quality class
		setTimeClass(MDF4Util.readUInt8(MDFParser.getDataBuffer(content, 13, 14)));

		// UINT8: Flags block.setFlags(MDF4Util.readUInt8(bb)); if
		setFlags(MDF4Util.readUInt8(MDFParser.getDataBuffer(content, 14, 15)));

		// 1 Byte reserved (15)

		// REAL: Start angle in radians at start of measurement (only for
		// angle synchronous measurements)
		setStartAngleRad(MDF4Util.readReal(MDFParser.getDataBuffer(content, 16, 24)));

		// REAL: Start distance in meters at start of measurement
		setStartDistanceM(MDF4Util.readReal(MDFParser.getDataBuffer(content, 24, 32)));

	}

}
