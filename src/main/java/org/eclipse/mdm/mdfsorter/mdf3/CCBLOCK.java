/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter.mdf3;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.eclipse.mdm.mdfsorter.MDFParser;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4Util;

/**
 * The Conversion Block
 * 
 * @author Tobias Leemann, Christian Rechner
 */
@SuppressWarnings("unused")
public class CCBLOCK extends MDF3GenBlock {

	// BOOL 1 Value range – known physical value
	private boolean knownPhysValue;

	// REAL 1 Value range – minimum physical value
	private double minPhysValue;

	// REAL 1 Value range – maximum physical value
	private double maxPhysValue;

	// CHAR 20 Physical unit
	private String physUnit;

	// UINT16 1 Conversion formula identifier
	// 0 = parametric, linear
	// 1 = tabular with interpolation
	// 2 = tabular
	// 6 = polynomial function
	// 7 = exponential function
	// 8 = logarithmic function
	// 9 = ASAP2 Rational conversion formula
	// 10 = ASAM-MCD2 Text formula
	// 11 = ASAM-MCD2 Text Table, (COMPU_VTAB)
	// 12 = ASAM-MCD2 Text Range Table (COMPU_VTAB_RANGE)
	// 132 = Date (Based on 7 Byte Date data structure)
	// 133 = time (Based on 6 Byte Time data structure)
	// 65535 = 1:1 conversion formula (Int = Phys)
	private int formulaIdent;

	// UINT16 1 Number of value pairs for conversion formulas 1, 2, 11 and 12 or
	// number of
	// parameters
	private int noOfValuePairsForFormula;

	private byte[] conversionData;

	// ... Parameter (for type 0,6,7,8,9) or table (for type 1, 2, 11, or 12) or
	// text (for type
	// 10), depending on the conversion formula identifier. See formula-specific
	// block
	// supplement.
	private double[] valuePairsForFormula; // formula = 0,6,7,8,9

	private double[] keysForTextTable; // formula = 11
	private String[] valuesForTextTable; // formula = 11

	private String defaultTextForTextRangeTable; // formula = 12
	private double[] lowerRangeKeysForTextRangeTable; // formula = 12
	private double[] upperRangeKeysForTextRangeTable; // formula = 12
	private String[] valuesForTextRangeTable; // formula = 12

	/**
	 * Parse a CCBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public CCBLOCK(MDF3GenBlock parent) {
		super(parent.getPos(), parent.isBigEndian());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	public boolean isKnownPhysValue() {
		return knownPhysValue;
	}

	private void setKnownPhysValue(boolean knownPhysValue) {
		this.knownPhysValue = knownPhysValue;
	}

	public double getMinPhysValue() {
		return minPhysValue;
	}

	private void setMinPhysValue(double minPhysValue) {
		this.minPhysValue = minPhysValue;
	}

	public double getMaxPhysValue() {
		return maxPhysValue;
	}

	private void setMaxPhysValue(double maxPhysValue) {
		this.maxPhysValue = maxPhysValue;
	}

	public String getPhysUnit() {
		return physUnit;
	}

	private void setPhysUnit(String physUnit) {
		this.physUnit = physUnit;
	}

	public int getFormulaIdent() {
		return formulaIdent;
	}

	private void setFormulaIdent(int formulaIdent) {
		this.formulaIdent = formulaIdent;
	}

	public int getNoOfValuePairsForFormula() {
		return noOfValuePairsForFormula;
	}

	private void setNoOfValuePairsForFormula(int noOfValuePairsForFormula) {
		this.noOfValuePairsForFormula = noOfValuePairsForFormula;
	}

	public double[] getValuePairsForFormula() {
		return valuePairsForFormula;
	}

	private void setValuePairsForFormula(double[] valuePairsForFormula) {
		this.valuePairsForFormula = valuePairsForFormula;
	}

	public double[] getKeysForTextTable() {
		return keysForTextTable;
	}

	private void setKeysForTextTable(double[] keysForTextTable) {
		this.keysForTextTable = keysForTextTable;
	}

	public String[] getValuesForTextTable() {
		return valuesForTextTable;
	}

	private void setValuesForTextTable(String[] valuesForTextTable) {
		this.valuesForTextTable = valuesForTextTable;
	}

	public String getDefaultTextForTextRangeTable() {
		return defaultTextForTextRangeTable;
	}

	private void setDefaultTextForTextRangeTable(String defaultTextForTextRangeTable) {
		this.defaultTextForTextRangeTable = defaultTextForTextRangeTable;
	}

	public double[] getLowerRangeKeysForTextRangeTable() {
		return lowerRangeKeysForTextRangeTable;
	}

	private void setLowerRangeKeysForTextRangeTable(double[] lowerRangeKeysForTextRangeTable) {
		this.lowerRangeKeysForTextRangeTable = lowerRangeKeysForTextRangeTable;
	}

	public double[] getUpperRangeKeysForTextRangeTable() {
		return upperRangeKeysForTextRangeTable;
	}

	private void setUpperRangeKeysForTextRangeTable(double[] upperRangeKeysForTextRangeTable) {
		this.upperRangeKeysForTextRangeTable = upperRangeKeysForTextRangeTable;
	}

	public String[] getValuesForTextRangeTable() {
		return valuesForTextRangeTable;
	}

	private void setValuesForTextRangeTable(String[] valuesForTextRangeTable) {
		this.valuesForTextRangeTable = valuesForTextRangeTable;
	}

	@Override
	public String toString() {
		return "CCBLOCK [knownPhysValue=" + knownPhysValue + ", minPhysValue=" + minPhysValue + ", maxPhysValue="
				+ maxPhysValue + ", physUnit=" + physUnit + ", formulaIdent=" + formulaIdent
				+ ", noOfValuePairsForFormula=" + noOfValuePairsForFormula + "]";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.mdm.mdfsorter.mdf3.MDF3GenBlock#parse(byte[])
	 */
	@Override
	public void parse(byte[] content) throws IOException {
		// BOOL 1 Value range – known physical value
		setKnownPhysValue(MDF3Util.readBool(MDFParser.getDataBuffer(content, 0, 2), isBigEndian()));

		// REAL 1 Value range – minimum physical value
		setMinPhysValue(MDF4Util.readReal(MDFParser.getDataBuffer(content, 2, 10)));

		// REAL 1 Value range – maximum physical value
		setMaxPhysValue(MDF4Util.readReal(MDFParser.getDataBuffer(content, 10, 18)));

		// CHAR 20 Physical unit
		setPhysUnit(MDF3Util.readCharsISO8859(MDFParser.getDataBuffer(content, 18, 38), 20));

		// UINT16 1 Conversion formula identifier
		// 0 = parametric, linear
		// 1 = tabular with interpolation
		// 2 = tabular
		// 6 = polynomial function
		// 7 = exponential function
		// 8 = logarithmic function
		// 9 = ASAP2 Rational conversion formula
		// 10 = ASAM-MCD2 Text formula
		// 11 = ASAM-MCD2 Text Table, (COMPU_VTAB)
		// 12 = ASAM-MCD2 Text Range Table (COMPU_VTAB_RANGE)
		// 132 = Date (Based on 7 Byte Date data structure)
		// 133 = time (Based on 6 Byte Time data structure)
		// 65535 = 1:1 conversion formula (Int = Phys)
		setFormulaIdent(MDF3Util.readUInt16(MDFParser.getDataBuffer(content, 38, 40), isBigEndian()));

		if (content.length > 40) {
			// UINT16 1 Number of value pairs for conversion formulas 1, 2, 11
			// and 12 or number of parameters
			setNoOfValuePairsForFormula(MDF3Util.readUInt16(MDFParser.getDataBuffer(content, 40, 42), isBigEndian()));
		}

		if (content.length > 42) {
			conversionData = new byte[content.length - 42];
			System.arraycopy(content, 42, conversionData, 0, conversionData.length);
		}
	}

	@Override
	public void updateLinks(RandomAccessFile r) throws IOException {
		if (getLinkCount() == 0) {
			return;
		}

		if (formulaIdent != 12) {
			throw new RuntimeException("Only a CC block with formula type 12 can have links.");
		}

		MDF3GenBlock linkedblock;
		for (int i = 0; i < getLinkCount(); i++) {
			r.seek(getOutputpos() + 4L + 42L + 20L * i + 16L);
			// position of links, see specification.
			if ((linkedblock = getLink(i)) != null) {
				r.write(MDF3Util.getBytesLink(linkedblock.getOutputpos(), isBigEndian()));
			} else {
				r.write(MDF3Util.getBytesLink(0, isBigEndian()));
			}
		}
	}
}
