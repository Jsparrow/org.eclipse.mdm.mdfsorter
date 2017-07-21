/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter.mdf3;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.mdm.mdfsorter.MDFParser;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4Util;

/**
 * The Data Group Block
 *
 * @author Christian Rechner, Tobias Leemann
 *
 */
public class HDBLOCK extends MDF3GenBlock {

	/** Data section */
	// UINT16
	// Number of Data Groups in linked list.
	private int numberOfDataGroups;

	// CHAR 10 Date at which the recording was started in "DD:MM:YYYY" format
	private String dateStarted;

	// CHAR 8 Time at which the recording was started in "HH:MM:SS" format
	private String timeStarted;

	// CHAR 32 Author’s name
	private String author;

	// CHAR 32 Name of organization or department
	private String department;

	// CHAR 32 Project name
	private String projectName;

	// CHAR 32 Measurement object e. g. the vehicle identification
	private String meaObject;

	// UINT64 1 Time stamp at which recording was started in nanoseconds.
	// Elapsed time since 00:00:00 01.01.1970 (local
	// time) (local time = UTC time + UTC time offset) Note: the local time does
	// not contain a daylight saving time
	// (DST) offset! Valid since version 3.20. Default value: 0 See remark below
	private long timestamp;

	// INT16 1 UTC time offset in hours (= GMT time zone) For example 1 means
	// GMT+1 time zone = Central European Time
	// (CET). The value must be in range [-12, 12], i.e. it can be negative!
	// Valid since version 3.20. Default value: 0
	// (= GMT time)
	private short utcTimeOffsetHours;

	// UINT16 1 Time quality class
	// 0 = local PC reference time (Default)
	// 10 = external time source
	// 16 = external absolute synchronized time
	private int timeQualityClass;

	// CHAR 32 Timer identification (time source), e.g. "Local PC Reference
	// Time" or "GPS Reference Time". Valid since
	// version 3.20. Default value: empty string
	private String timerIdent;

	/**
	 * Parse a HLBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public HDBLOCK(MDF3GenBlock parent) {
		super(parent.getPos(), parent.isBigEndian());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	public MDF3GenBlock getLnkFirstFileGroup() {
		return links[0];
	}

	public MDF3GenBlock getLnkFileCommentTxt() {
		return links[1];
	}

	public MDF3GenBlock getLnkProgramBlock() {
		return links[2];
	}

	public int getNumberOfDataGroups() {
		return numberOfDataGroups;
	}

	public void setNumberOfDataGroups(int numberOfDataGroups) {
		this.numberOfDataGroups = numberOfDataGroups;
	}

	public String getDateStarted() {
		return dateStarted;
	}

	public void setDateStarted(String dateStarted) {
		this.dateStarted = dateStarted;
	}

	public String getTimeStarted() {
		return timeStarted;
	}

	public void setTimeStarted(String timeStarted) {
		this.timeStarted = timeStarted;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getMeaObject() {
		return meaObject;
	}

	public void setMeaObject(String meaObject) {
		this.meaObject = meaObject;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public short getUtcTimeOffsetHours() {
		return utcTimeOffsetHours;
	}

	public void setUtcTimeOffsetHours(short utcTimeOffsetHours) {
		this.utcTimeOffsetHours = utcTimeOffsetHours;
	}

	public int getTimeQualityClass() {
		return timeQualityClass;
	}

	public void setTimeQualityClass(int timeQualityClass) {
		this.timeQualityClass = timeQualityClass;
	}

	public String getTimerIdent() {
		return timerIdent;
	}

	public void setTimerIdent(String timerIdent) {
		this.timerIdent = timerIdent;
	}

	@Override
	public String toString() {
		return "HDBLOCK [dateStarted=" + dateStarted + ", timeStarted=" + timeStarted + ", author=" + author
				+ ", department=" + department + ", projectName=" + projectName + ", meaObject=" + meaObject
				+ ", timestamp=" + timestamp + ", utcTimeOffsetHours=" + utcTimeOffsetHours + ", timeQualityClass="
				+ timeQualityClass + ", timerIdent=" + timerIdent + "]";
	}

	@Override
	public void parse(byte[] content) throws IOException {
		// UNINT16 Number of Data Groups
		setNumberOfDataGroups(MDF3Util.readUInt16(MDFParser.getDataBuffer(content, 0, 2), isBigEndian()));

		// CHAR 10 Date when the recording was started
		setDateStarted(MDF4Util.readCharsUTF8(MDFParser.getDataBuffer(content, 2, 12), 10));

		// CHAR 8 Signal name, i.e. the first 32 characters of the ASAM-MCD
		// unique name
		setTimeStarted(MDF4Util.readCharsUTF8(MDFParser.getDataBuffer(content, 12, 20), 8));

		setAuthor(MDF4Util.readCharsUTF8(MDFParser.getDataBuffer(content, 20, 52), 32));
		setDepartment(MDF4Util.readCharsUTF8(MDFParser.getDataBuffer(content, 52, 84), 32));
		setProjectName(MDF4Util.readCharsUTF8(MDFParser.getDataBuffer(content, 84, 116), 32));
		setMeaObject(MDF4Util.readCharsUTF8(MDFParser.getDataBuffer(content, 116, 148), 32));

		if (content.length > 148) {
			// UNINT 64 Timestamp
			setTimestamp(MDF3Util.readUInt64(MDFParser.getDataBuffer(content, 148, 156), isBigEndian()));
		}
		if (content.length > 156) {
			// INT16, Time zone offset in hours
			setUtcTimeOffsetHours(MDF3Util.readInt16(MDFParser.getDataBuffer(content, 156, 158), isBigEndian()));
		}

		if (content.length > 158) {
			// UINT16, Time Quality
			setTimeQualityClass(MDF3Util.readUInt16(MDFParser.getDataBuffer(content, 158, 160), isBigEndian()));
		}

		if (content.length > 160) {
			setTimerIdent(MDF4Util.readCharsUTF8(MDFParser.getDataBuffer(content, 160, 192), 32));
		}

	}

	@Override
	public byte[] getBodyBytes() throws IOException {
		int arraylength = (int) (getLength() - 16L); // Length of header and 3
		// links
		ByteBuffer b = ByteBuffer.allocate(arraylength);

		b.put(MDF3Util.getBytesUInt16(getNumberOfDataGroups(), isBigEndian()));

		b.put(MDF4Util.getBytesCharsUTF8(getDateStarted()));

		b.put(MDF4Util.getBytesCharsUTF8(getTimeStarted()));

		b.put(MDF4Util.getBytesCharsUTF8(getAuthor()));

		b.put(MDF4Util.getBytesCharsUTF8(getDepartment()));

		b.put(MDF4Util.getBytesCharsUTF8(getProjectName()));

		b.put(MDF4Util.getBytesCharsUTF8(getMeaObject()));

		if (arraylength > 148) {
			b.put(MDF3Util.getBytesUInt64(getTimestamp(), isBigEndian()));
		}

		if (arraylength > 156) {
			b.put(MDF3Util.getBytesInt16(getUtcTimeOffsetHours(), isBigEndian()));
		}

		if (arraylength > 158) {
			b.put(MDF3Util.getBytesUInt16(getTimeQualityClass(), isBigEndian()));
		}

		if (arraylength > 160) {
			b.put(MDF4Util.getBytesCharsUTF8(getTimerIdent()));
		}

		return b.array();
	}

}
