/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter.mdf4;

import java.io.IOException;
import java.math.BigInteger;

import org.eclipse.mdm.mdfsorter.MDFParser;

/**
 * The Channel Block.
 *
 * @author Christian Rechner, Tobias Leemann
 *
 */
public class CNBLOCK extends MDF4GenBlock {
	/** Data section */

	// Channel type
	// 0 = fixed length data channel channel value is contained in record.
	// 1 = variable length data channel also denoted as "variable length signal
	// data" (VLSD) channel
	// 2 = master channel for all signals of this group
	// 3 = virtual master channel
	// 4 = synchronization channel
	// 5 = maximum length data channel
	// 6 = virtual data channel
	// UINT8
	private byte channelType;

	// Sync type:
	// 0 = None (to be used for normal data channels)
	// 1 = Time (physical values must be seconds)
	// 2 = Angle (physical values must be radians)
	// 3 = Distance (physical values must be meters)
	// 4 = Index (physical values must be zero-based index values)
	// UINT8
	private byte syncType;

	// Channel data type of raw signal value
	// 0 = unsigned integer (LE Byte order)
	// 1 = unsigned integer (BE Byte order)
	// 2 = signed integer (two’s complement) (LE Byte order)
	// 3 = signed integer (two’s complement) (BE Byte order)
	// 4 = IEEE 754 floating-point format (LE Byte order)
	// 5 = IEEE 754 floating-point format (BE Byte order)
	// 6 = String (SBC, standard ASCII encoded (ISO-8859-1 Latin), NULL
	// terminated)
	// 7 = String (UTF-8 encoded, NULL terminated)
	// 8 = String (UTF-16 encoded LE Byte order, NULL terminated)
	// 9 = String (UTF-16 encoded BE Byte order, NULL terminated)
	// 10 = Byte Array with unknown content (e.g. structure)
	// 11 = MIME sample (sample is Byte Array with MIME content-type specified
	// in cn_md_unit)
	// 12 = MIME stream (all samples of channel represent a stream with MIME
	// content-type specified in cn_md_unit)
	// 13 = CANopen date (Based on 7 Byte CANopen Date data structure, see Table
	// 36)
	// 14 = CANopen time (Based on 6 Byte CANopen Time data structure, see Table
	// 37)
	// UINT8
	private byte dataType;

	// Bit offset (0-7): first bit (=LSB) of signal value after Byte offset has
	// been applied.
	// If zero, the signal value is 1-Byte aligned. A value different to zero is
	// only allowed for Integer data types
	// (cn_data_type ≤ 3) and if the Integer signal value fits into 8 contiguous
	// Bytes (cn_bit_count + cn_bit_offset ≤
	// 64). For all other cases, cn_bit_offset must be zero.
	// UINT8
	private byte bitOffset;

	// Offset to first Byte in the data record that contains bits of the signal
	// value. The offset is applied to the
	// plain record data, i.e. skipping the record ID.
	// UINT32
	private long byteOffset;

	// Number of bits for signal value in record.
	// UINT32
	private long bitCount;

	// The value contains the following bit flags (Bit 0 = LSB):
	// Bit 0: All values invalid flag
	// Bit 1: Invalidation bit valid flag
	// Bit 2: Precision valid flag
	// Bit 3: Value range valid flag
	// Bit 4: Limit range valid flag
	// Bit 5: Extended limit range valid flag
	// Bit 6: Discrete value flag
	// Bit 7: Calibration flag
	// Bit 8: Calculated flag
	// Bit 10: Bus event flag
	// Bit 11: Monotonous flag
	// Bit 12: Default X axis flag
	// UINT32
	private long flags;

	// Position of invalidation bit.
	// The invalidation bit can be used to specify if the signal value in the
	// current record is valid or not.
	// Note: the invalidation bit is optional and can only be used if the
	// "invalidation bit valid" flag (bit 1) is set.
	// UINT32
	private long invalBitPos;

	// Precision for display of floating point values.
	// 0xFF means unrestricted precision (infinite).
	// Any other value specifies the number of decimal places to use for display
	// of floating point values.
	// Only valid if "precision valid" flag (bit 2) is set
	// UINT8
	private byte precision;

	// Length N of cn_at_reference list, i.e. number of attachments for this
	// channel. Can be zero.
	// UINT16
	private int attachmentCount;

	// Minimum signal value that occurred for this signal (raw value)
	// Only valid if "value range valid" flag (bit 3) is set.
	// REAL
	private double valRangeMin;

	// Maximum signal value that occurred for this signal (raw value)
	// Only valid if "value range valid" flag (bit 3) is set.
	// REAL
	private double valRangeMax;

	// Lower limit for this signal (physical value for numeric conversion rule,
	// otherwise raw value)
	// Only valid if "limit range valid" flag (bit 4) is set.
	// REAL
	private double limitMin;

	// Upper limit for this signal (physical value for numeric conversion rule,
	// otherwise raw value)
	// Only valid if "limit range valid" flag (bit 4) is set.
	// REAL
	private double limitMax;

	// Lower extended limit for this signal (physical value for numeric
	// conversion rule, otherwise raw value)
	// Only valid if "extended limit range valid" flag (bit 5) is set.
	// If cn_limit_min is valid, cn_limit_min must be larger or equal to
	// cn_limit_ext_min.
	// REAL
	private double limitExtMin;

	// Upper extended limit for this signal (physical value for numeric
	// conversion rule, otherwise raw value)
	// Only valid if "extended limit range valid" flag (bit 5) is set.
	// If cn_limit_max is valid, cn_limit_max must be less or equal to
	// cn_limit_ext_max.
	// REAL
	private double limitExtMax;

	/**
	 * Parse a HLBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public CNBLOCK(MDF4GenBlock parent) {
		super(parent.getPos());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	public MDF4GenBlock getLnkCnNext() {
		return links[0];
	}

	public MDF4GenBlock getLnkComposition() {
		return links[1];
	}

	public MDF4GenBlock getLnkTxName() {
		return links[2];
	}

	public MDF4GenBlock getLnkSiSource() {
		return links[3];
	}

	public MDF4GenBlock getLnkCcConversion() {
		return links[4];
	}

	public MDF4GenBlock getLnkData() {
		return links[5];
	}

	public MDF4GenBlock getLnkMdUnit() {
		return links[6];
	}

	public MDF4GenBlock getLnkMdComment() {
		return links[7];
	}

	public MDF4GenBlock[] getLnkAtReference() {
		MDF4GenBlock[] ret = new MDF4GenBlock[getAttachmentCount()];
		System.arraycopy(links, 8, ret, 0, getAttachmentCount());
		return ret;
	}

	public MDF4GenBlock[] getLnkDefaultX() {
		if (getDefaultXFlag()) {
			MDF4GenBlock[] ret = new MDF4GenBlock[3];
			System.arraycopy(links, 8 + getAttachmentCount(), ret, 0, 3);
			return ret;
		} else {
			return null;
		}

	}

	public byte getChannelType() {
		return channelType;
	}

	public byte getSyncType() {
		return syncType;
	}

	public byte getDataType() {
		return dataType;
	}

	public byte getBitOffset() {
		return bitOffset;
	}

	public long getByteOffset() {
		return byteOffset;
	}

	public long getBitCount() {
		return bitCount;
	}

	public long getFlags() {
		return flags;
	}

	public long getInvalBitPos() {
		return invalBitPos;
	}

	public byte getPrecision() {
		return precision;
	}

	public int getAttachmentCount() {
		return attachmentCount;
	}

	public double getValRangeMin() {
		return valRangeMin;
	}

	public double getValRangeMax() {
		return valRangeMax;
	}

	public double getLimitMin() {
		return limitMin;
	}

	public double getLimitMax() {
		return limitMax;
	}

	public double getLimitExtMin() {
		return limitExtMin;
	}

	public double getLimitExtMax() {
		return limitExtMax;
	}

	private void setChannelType(byte channelType) {
		this.channelType = channelType;
	}

	private void setSyncType(byte syncType) {
		this.syncType = syncType;
	}

	private void setDataType(byte dataType) {
		this.dataType = dataType;
	}

	private void setBitOffset(byte bitOffset) {
		this.bitOffset = bitOffset;
	}

	private void setByteOffset(long byteOffset) {
		this.byteOffset = byteOffset;
	}

	private void setBitCount(long bitCount) {
		this.bitCount = bitCount;
	}

	private void setFlags(long flags) {
		this.flags = flags;
	}

	private void setInvalBitPos(long invalBitPos) {
		this.invalBitPos = invalBitPos;
	}

	private void setPrecision(byte precision) {
		this.precision = precision;
	}

	private void setAttachmentCount(int attachmentCount) {
		this.attachmentCount = attachmentCount;
	}

	private void setValRangeMin(double valRangeMin) {
		this.valRangeMin = valRangeMin;
	}

	private void setValRangeMax(double valRangeMax) {
		this.valRangeMax = valRangeMax;
	}

	private void setLimitMin(double limitMin) {
		this.limitMin = limitMin;
	}

	private void setLimitMax(double limitMax) {
		this.limitMax = limitMax;
	}

	private void setLimitExtMin(double limitExtMin) {
		this.limitExtMin = limitExtMin;
	}

	private void setLimitExtMax(double limitExtMax) {
		this.limitExtMax = limitExtMax;
	}

	/**
	 * Returns bit 12 of the flags ("default X"-flag).
	 *
	 * @return The "default X"-flag.
	 */
	public boolean getDefaultXFlag() {
		return BigInteger.valueOf(flags).testBit(12);
	}

	@Override
	public String toString() {
		return "CNBLOCK [lnkCnNext=" + getLnkCnNext().getPos()
				+ ", lnkComposition=" + getLnkComposition().getPos()
				+ ", lnkTxName=" + getLnkTxName().getPos() + ", lnkSiSource="
				+ getLnkSiSource().getPos() + ", lnkCcConversion="
				+ getLnkComposition().getPos() + ", lnkData="
				+ getLnkData().getPos() + ", lnkMdUnit="
				+ getLnkMdUnit().getPos() + ", lnkMdComment="
				+ getLnkMdComment().getPos() + ", lnkAtReference=" + "{"
				+ getLnkAtReference().length + "}" + ", lnkDefaultX=" + "{"
				+ getLnkDefaultX().length + "}" + ", channelType=" + channelType
				+ ", syncType=" + syncType + ", dataType=" + dataType
				+ ", bitOffset=" + bitOffset + ", byteOffset=" + byteOffset
				+ ", bitCount=" + bitCount + ", flags=" + flags
				+ ", invalBitPos=" + invalBitPos + ", precision=" + precision
				+ ", attachmentCount=" + attachmentCount + ", valRangeMin="
				+ valRangeMin + ", valRangeMax=" + valRangeMax + ", limitMin="
				+ limitMin + ", limitMax=" + limitMax + ", limitExtMin="
				+ limitExtMin + ", limitExtMax=" + limitExtMax + "]";
	}

	@Override
	public void parse(byte[] content) throws IOException {
		// UINT8: Channel type
		setChannelType(content[0]);

		// UINT8: Sync type
		setSyncType(content[1]);

		// UINT8: Channel data type of raw signal value
		setDataType(content[2]);

		// UINT8: Bit offset (0-7)
		setBitOffset(content[3]);

		// UINT32: Offset to first Byte in the data record that contains bits of
		// the signal value.
		setByteOffset(
				MDF4Util.readUInt32(MDFParser.getDataBuffer(content, 4, 8)));

		// UINT32: Number of bits for signal value in record.
		setBitCount(
				MDF4Util.readUInt32(MDFParser.getDataBuffer(content, 8, 12)));

		// UINT32: Flags
		setFlags(MDF4Util.readUInt32(MDFParser.getDataBuffer(content, 12, 16)));

		// UINT32: Position of invalidation bit.
		setInvalBitPos(
				MDF4Util.readUInt32(MDFParser.getDataBuffer(content, 16, 20)));

		// UINT8: Precision for display of floating point values.
		setPrecision(content[20]);

		// Skip 1 BYTE: Reserved

		// UINT16: Length N of cn_at_reference list, i.e. number of attachments
		// for this channel. Can be zero.
		setAttachmentCount(
				MDF4Util.readUInt16(MDFParser.getDataBuffer(content, 22, 24)));

		// REAL: Minimum signal value that occurred for this signal (raw value)
		setValRangeMin(
				MDF4Util.readReal(MDFParser.getDataBuffer(content, 24, 32)));

		// REAL: Maximum signal value that occurred for this signal (raw value)
		setValRangeMax(
				MDF4Util.readReal(MDFParser.getDataBuffer(content, 32, 40)));

		// REAL: Lower limit for this signal (physical value for numeric
		// conversion rule, otherwise raw value)
		setLimitMin(
				MDF4Util.readReal(MDFParser.getDataBuffer(content, 40, 48)));

		// REAL: Upper limit for this signal (physical value for numeric
		// conversion rule, otherwise raw value)
		setLimitMax(
				MDF4Util.readReal(MDFParser.getDataBuffer(content, 48, 56)));

		// REAL: Lower extended limit for this signal (physical value for
		// numeric conversion rule, otherwise raw value)
		setLimitExtMin(
				MDF4Util.readReal(MDFParser.getDataBuffer(content, 56, 64)));

		// REAL: Upper extended limit for this signal (physical value for
		// numeric conversion rule, otherwise raw value)
		setLimitExtMax(
				MDF4Util.readReal(MDFParser.getDataBuffer(content, 64, 72)));

	}

}
