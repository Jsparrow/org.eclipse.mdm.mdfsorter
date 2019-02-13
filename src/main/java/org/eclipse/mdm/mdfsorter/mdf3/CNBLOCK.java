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
import java.nio.ByteBuffer;

import org.eclipse.mdm.mdfsorter.MDFParser;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4Util;

/**
 * <p>
 * Channel block: Description of a channel
 * </p>
 * This block describes a measurement channel.
 *
 * @author Christian Rechner
 */
public class CNBLOCK extends MDF3GenBlock {

	// UINT16 1 Channel type
	// 0 = data channel
	// 1 = time channel for all signals of this group (in each channel group,
	// exactly one
	// channel must be defined as time channel)
	private int channelType;

	// CHAR 32 Signal name, i.e. the first 32 characters of the ASAM-MCD unique
	// name
	private String signalName;

	// CHAR 128 Signal description
	private String signalDescription;

	// UINT16 1 Number of the first bits [0..n] (bit position within a byte: bit
	// 0 is the least significant
	// bit, bit 7 is the most significant bit)
	private int numberOfFirstBits;

	// UINT16 1 Number of bits
	private int numberOfBits;

	// UINT16 1 Signal data type
	// 0 = unsigned integer
	// 1 = signed integer (two’s complement)
	// 2,3 = IEEE 754 floating-point format
	// 7 = String (NULL terminated)
	// 8 = Byte Array
	private int signalDataType;

	// BOOL 1 Value Range Valid.
	private boolean knownImplValue;

	// REAL 1 Value range – minimum implementation value
	private double minImplValue;

	// REAL 1 Value range – maximum implementation value
	private double maxImplValue;

	// REAL 1 Rate in which the variable was sampled. Unit [s]
	private double sampleRate;

	// UINT16 1 Byte offset of the signal in the data record in addition to bit
	// offset (default value: 0)
	// note: this fields shall only be used if the CGBLOCK record size and the
	// actual offset
	// is larger than 8192 Bytes to ensure compatibility; it enables to write
	// data blocks
	// larger than 8kBytes
	private int byteOffset;

	public CNBLOCK(MDF3GenBlock parent) {
		super(parent.getPos(), parent.isBigEndian());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	public MDF3GenBlock getLnkNextCnBlock() {
		return links[0];
	}

	public MDF3GenBlock getLnkCcBlock() {
		return links[1];
	}

	public MDF3GenBlock getLnkCeBlock() {
		return links[2];
	}

	public MDF3GenBlock getLnkCdBlock() {
		return links[3];
	}

	public MDF3GenBlock getLnkChannelComment() {
		return links[4];
	}

	public MDF3GenBlock getLnkMcdUniqueName() {
		return getLinkCount() > 5 ? links[5] : null;
	}

	public MDF3GenBlock getLnkSignalDisplayIdentifier() {
		return getLinkCount() > 6 ? links[6] : null;
	}

	public int getChannelType() {
		return channelType;
	}

	private void setChannelType(int channelType) {
		this.channelType = channelType;
	}

	public String getSignalName() {
		return signalName;
	}

	public void setSignalName(String signalName) {
		this.signalName = signalName;
	}

	public String getSignalDescription() {
		return signalDescription;
	}

	public void setSignalDescription(String signalDescription) {
		this.signalDescription = signalDescription;
	}

	public int getNumberOfFirstBits() {
		return numberOfFirstBits;
	}

	private void setNumberOfFirstBits(int numberOfFirstBits) {
		this.numberOfFirstBits = numberOfFirstBits;
	}

	public int getNumberOfBits() {
		return numberOfBits;
	}

	private void setNumberOfBits(int numberOfBits) {
		this.numberOfBits = numberOfBits;
	}

	public int getSignalDataType() {
		return signalDataType;
	}

	private void setSignalDataType(int signalDataType) {
		this.signalDataType = signalDataType;
	}

	public boolean isKnownImplValue() {
		return knownImplValue;
	}

	private void setKnownImplValue(boolean knownImplValue) {
		this.knownImplValue = knownImplValue;
	}

	public double getMinImplValue() {
		return minImplValue;
	}

	private void setMinImplValue(double minImplValue) {
		this.minImplValue = minImplValue;
	}

	public double getMaxImplValue() {
		return maxImplValue;
	}

	private void setMaxImplValue(double maxImplValue) {
		this.maxImplValue = maxImplValue;
	}

	public double getSampleRate() {
		return sampleRate;
	}

	private void setSampleRate(double sampleRate) {
		this.sampleRate = sampleRate;
	}

	public int getByteOffset() {
		return byteOffset;
	}

	private void setByteOffset(int byteOffset) {
		this.byteOffset = byteOffset;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("CNBLOCK [ channelType=").append(channelType).append(", signalName=").append(signalName).append(", signalDescription=").append(signalDescription).append(", numberOfFirstBits=")
				.append(numberOfFirstBits).append(", numberOfBits=").append(numberOfBits).append(", signalDataType=").append(signalDataType).append(", knownImplValue=").append(knownImplValue)
				.append(", minImplValue=").append(minImplValue).append(", maxImplValue=").append(maxImplValue).append(", sampleRate=").append(sampleRate).append(" byteOffset=")
				.append(byteOffset).append("]").toString();
	}

	@Override
	public byte[] getHeaderBytes() {
		byte[] ret = super.getHeaderBytes();
		if (getLinkCount() > 5) {
			byte[] newret = new byte[24];
			System.arraycopy(ret, 0, newret, 0, newret.length);
			ret = newret;
		}
		return ret;

	}

	@Override
	public void updateLinks(RandomAccessFile r) throws IOException {
		// set position to start of Block link section
		r.seek(getOutputpos() + 4L);
		MDF3GenBlock linkedblock;
		for (int i = 0; i < 5; i++) {
			if ((linkedblock = getLink(i)) != null) {
				r.write(MDF3Util.getBytesLink((int) linkedblock.getOutputpos(), isBigEndian()));
			} else {
				r.write(MDF3Util.getBytesLink(0, isBigEndian()));
			}
		}

		// update last two links manually
		if (getLinkCount() != 7) {
			return;
		}
		r.seek(getOutputpos() + 4L + 20L + 194L);
		r.write(MDF3Util.getBytesLink(getLink(5) != null ? getLink(5).getOutputpos() : 0, isBigEndian()));
		r.write(MDF3Util.getBytesLink(getLink(6) != null ? getLink(6).getOutputpos() : 0, isBigEndian()));

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.mdm.mdfsorter.mdf3.MDF3GenBlock#parse(byte[])
	 */
	@Override
	public void parse(byte[] content) throws IOException {
		// UINT16 1 Channel type
		// 0 = data channel
		// 1 = time channel for all signals of this group (in each channel
		// group, exactly one
		// channel must be defined as time channel)
		setChannelType(MDF3Util.readUInt16(MDFParser.getDataBuffer(content, 0, 2), isBigEndian()));

		// CHAR 32 Signal name, i.e. the first 32 characters of the ASAM-MCD
		// unique name
		setSignalName(MDF3Util.readCharsISO8859(MDFParser.getDataBuffer(content, 2, 34), 32));

		// CHAR 128 Signal description
		setSignalDescription(MDF3Util.readCharsISO8859(MDFParser.getDataBuffer(content, 34, 162), 128));

		// UINT16 1 Number of the first bits [0..n] (bit position within a byte:
		// bit 0 is the least significant
		// bit, bit 7 is the most significant bit)
		setNumberOfFirstBits(MDF3Util.readUInt16(MDFParser.getDataBuffer(content, 162, 164), isBigEndian()));

		// UINT16 1 Number of bits
		setNumberOfBits(MDF3Util.readUInt16(MDFParser.getDataBuffer(content, 164, 166), isBigEndian()));

		// UINT16 1 Signal data type
		// 0 = unsigned integer
		// 1 = signed integer (two’s complement)
		// 2,3 = IEEE 754 floating-point format
		// 7 = String (NULL terminated)
		// 8 = Byte Array
		setSignalDataType(MDF3Util.readUInt16(MDFParser.getDataBuffer(content, 166, 168), isBigEndian()));

		// BOOL 1 Value range – known implementation value
		setKnownImplValue(MDF3Util.readBool(MDFParser.getDataBuffer(content, 168, 170), isBigEndian()));

		// REAL 1 Value range – minimum implementation value
		setMinImplValue(MDF4Util.readReal(MDFParser.getDataBuffer(content, 170, 178)));

		// REAL 1 Value range – maximum implementation value
		setMaxImplValue(MDF4Util.readReal(MDFParser.getDataBuffer(content, 178, 186)));

		// REAL 1 Rate in which the variable was sampled. Unit [s]
		setSampleRate(MDF4Util.readReal(MDFParser.getDataBuffer(content, 186, 194)));

		// skip two links (2* 4Bytes, they are already read.

		if (content.length > 202) {
			// UINT16 1 Byte offset of the signal in the data record in addition
			// to bit offset (default value: 0)
			// note: this fields shall only be used if the CGBLOCK record size
			// and the actual offset
			// is larger than 8192 Bytes to ensure compatibility; it enables to
			// write data blocks
			// larger than 8kBytes
			setByteOffset(MDF3Util.readUInt16(MDFParser.getDataBuffer(content, 202, 204), isBigEndian()));
		}
	}

	@Override
	public byte[] getBodyBytes() throws IOException {
		int arraylength = (int) (getLength() - 24L); // Length of header and 5
		// links
		var b = ByteBuffer.allocate(arraylength);

		b.put(MDF3Util.getBytesUInt16(getChannelType(), isBigEndian()));

		b.put(MDF3Util.getBytesCharsISO8859(getSignalName()));

		b.put(MDF3Util.getBytesCharsISO8859(getSignalDescription()));

		b.put(MDF3Util.getBytesUInt16(getNumberOfFirstBits(), isBigEndian()));

		b.put(MDF3Util.getBytesUInt16(getNumberOfBits(), isBigEndian()));

		b.put(MDF3Util.getBytesUInt16(getSignalDataType(), isBigEndian()));

		b.put(MDF3Util.getBytesBool(isKnownImplValue(), isBigEndian()));

		b.put(MDF4Util.getBytesReal(getMinImplValue()));

		b.put(MDF4Util.getBytesReal(getMaxImplValue()));

		b.put(MDF4Util.getBytesReal(getSampleRate()));

		if (arraylength > 202) {
			b.put(new byte[8]);
			// Skip space for two links
			b.put(MDF4Util.getBytesInt16(getByteOffset()));
		}

		return b.array();

	}
}
