/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter.mdf4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;


/**
 * <p>
 * THE CHANNEL CONVERSION BLOCK <code>CCBLOCK</code>
 * </p>
 * The data records can be used to store raw values (often also denoted as implementation values or internal values).
 * The CCBLOCK serves to specify a conversion formula to convert the raw values to physical values with a physical unit.
 * The result of a conversion always is either a floating-point value (REAL) or a character string (UTF-8).
 *
 * @author Christian Rechner
 */
public class CCBLOCK extends MDF4GenBlock {


	/** Data section */

	// Conversion type (formula identifier)
	// 0 = 1:1 conversion (in this case, the CCBLOCK can be omitted)
	// 1 = linear conversion
	// 2 = rational conversion
	// 3 = algebraic conversion (MCD-2 MC text formula)
	// 4 = value to value tabular look-up with interpolation
	// 5 = value to value tabular look-up without interpolation
	// 6 = value range to value tabular look-up
	// 7 = value to text/scale conversion tabular look-up
	// 8 = value range to text/scale conversion tabular look-up
	// 9 = text to value tabular look-up
	// 10 = text to text tabular look-up (translation)
	// UINT8
	private byte type;

	// Precision for display of floating point values.
	// 0xFF means unrestricted precision (infinite)
	// Any other value specifies the number of decimal places to use for display of floating point values.
	// Note: only valid if "precision valid" flag (bit 0) is set and if cn_precision of the parent CNBLOCK is invalid,
	// otherwise cn_precision must be used.
	// UINT8
	private byte precision;

	// Flags
	// The value contains the following bit flags (Bit 0 = LSB):
	// Bit 0: Precision valid flag
	// Bit 1: Physical value range valid flag
	// Bit 2: Status string flag
	// UINT16
	private int flags;

	// Length M of cc_ref list with additional links.
	// UINT16
	private int refCount;

	// Length N of cc_val list with additional parameters.
	// UINT16
	private int valCount;

	// Minimum physical signal value that occurred for this signal.
	// REAL
	private double phyRangeMin;

	// Maximum physical signal value that occurred for this signal.
	// REAL
	private double phyRangeMax;

	// List of additional conversion parameters.
	// Length of list is given by cc_val_count. The list can be empty.
	// REAL N
	private double[] val;


	/**
	 * Parse a CCBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public CCBLOCK(MDF4GenBlock parent) {
		super(parent.getPos());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	// Link to TXBLOCK with name (identifier) of conversion (can be NIL).
	public MDF4GenBlock getLnkTxName() {
		return links[0];
	}

	// Link to TXBLOCK/MDBLOCK with physical unit of signal data (after conversion). (can be NIL)
	public MDF4GenBlock getLnkMdUnit() {
		return links[1];
	}

	// Link to TXBLOCK/MDBLOCK with comment of conversion and additional information. (can be NIL)
	public MDF4GenBlock getLnkMdComment() {
		return links[2];
	}

	// Link to CCBLOCK for inverse formula (can be NIL, must be NIL for CCBLOCK of the inverse formula (no cyclic
	// reference allowed).
	public MDF4GenBlock getLnkCcInverse() {
		return links[3];
	}

	// List of additional links to TXBLOCKs with strings or to CCBLOCKs with partial conversion rules. Length of list is
	// given by cc_ref_count. The list can be empty. Details are explained in formula-specific block supplement.
	public MDF4GenBlock[] getLnkCcRef() {
		MDF4GenBlock[] ret = new MDF4GenBlock[getLinkCount()-4];
		System.arraycopy(links, 4, ret, 0, ret.length);
		return ret;
	}

	public byte getType() {
		return type;
	}

	public byte getPrecision() {
		return precision;
	}

	public int getFlags() {
		return flags;
	}

	public int getRefCount() {
		return refCount;
	}

	public int getValCount() {
		return valCount;
	}

	public double getPhyRangeMin() {
		return phyRangeMin;
	}

	public double getPhyRangeMax() {
		return phyRangeMax;
	}

	public double[] getVal() {
		return val;
	}

	private void setType(byte type) {
		this.type = type;
	}

	private void setPrecision(byte precision) {
		this.precision = precision;
	}

	private void setFlags(int flags) {
		this.flags = flags;
	}

	private void setRefCount(int refCount) {
		this.refCount = refCount;
	}

	private void setValCount(int valCount) {
		this.valCount = valCount;
	}

	private void setPhyRangeMin(double phyRangeMin) {
		this.phyRangeMin = phyRangeMin;
	}

	private void setPhyRangeMax(double phyRangeMax) {
		this.phyRangeMax = phyRangeMax;
	}

	private void setVal(double[] val) {
		this.val = val;
	}

	@Override
	public String toString() {
		return "CCBLOCK [type=" + type
				+ ", precision=" + precision + ", flags=" + flags + ", refCount=" + refCount + ", valCount=" + valCount
				+ ", phyRangeMin=" + phyRangeMin + ", phyRangeMax=" + phyRangeMax + ", val=" + Arrays.toString(val)
				+ "]";
	}

	/**
	 * Reads a CCBLOCK from its content.
	 *
	 * @param content The data section of this block
	 * @throws IOException If an I/O error occurs.
	 */
	@Override
	public void parse(byte[] content) throws IOException {
		ByteBuffer bb = ByteBuffer.wrap(content);
		// UINT8: Conversion type (formula identifier)
		setType(MDF4Util.readUInt8(bb));

		// UINT8: Precision for display of floating point values.
		setPrecision(MDF4Util.readUInt8(bb));

		// UINT16: Flags.
		setFlags(MDF4Util.readUInt16(bb));

		// UINT16: Length M of cc_ref list with additional links.
		setRefCount(MDF4Util.readUInt16(bb));

		// UINT16: Length N of cc_val list with additional.
		setValCount(MDF4Util.readUInt16(bb));

		// REAL: Minimum physical signal value that occurred for this signal.
		setPhyRangeMin(MDF4Util.readReal(bb));

		// REAL: Maximum physical signal value that occurred for this signal.
		setPhyRangeMax(MDF4Util.readReal(bb));

		// REAL N: List of additional conversion parameters.
		double[] val = new double[getValCount()];
		for (int i = 0; i < val.length; i++) {
			val[i] = MDF4Util.readReal(bb);
		}
		setVal(val);
	}

}
