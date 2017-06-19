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
 * The Channel Array Block
 *
 * @author Tobias Leemann
 *
 */
/**
 * @author EU2IYD9
 *
 */
public class CABLOCK extends MDF4GenBlock {

	/**
	 * Defines what kind of array is described.
	 */
	private byte arrayType;

	/**
	 * Defines how elements are stored - 0 = CN Template 1 = CG Template 2 = DG
	 * Template
	 */
	private byte storageType;

	/**
	 * Number of dimensions of this array.
	 */
	private int channelDimensions;

	/**
	 * Flags of this block.
	 */
	private long flags;

	/**
	 * Base factor for calculation of Byte offsets (CN template)
	 */
	private int byteOffsetBase;

	/**
	 * Base factor for calculation of the inval bit position (CN template only)
	 */
	private long invalBitPosBase;

	/**
	 * Sizes of D Dimensions of this array.
	 */
	private long[] dimSize;

	/**
	 * List of raw values fro axis points on each axis. Only present if
	 * FixedAxis flag is set.
	 */
	private double[] axisValues;

	/**
	 * Cycle Counts for all elements. (Only in CG DG template storage)
	 */
	private long[] cycCounts;

	// Helper variables

	/**
	 * The product of all sizes of dimensions.
	 * (dimSize[0]*dimSize[1]*dimSize[2]*...)
	 */
	private long volume;

	/**
	 * The sum of all sizes of dimensions.
	 * (dimSize[0]+dimSize[1]+dimSize[2]+...)
	 */
	private int sumd;

	/**
	 * Parse a CABLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public CABLOCK(MDF4GenBlock parent) {
		super(parent.getPos());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	// Links

	public MDF4GenBlock getLnkComposition() {
		return links[0];
	}

	public MDF4GenBlock[] getLnkData() {
		if (getStorageType() == 2) { // DG Template
			MDF4GenBlock[] data = new MDF4GenBlock[(int) volume];
			System.arraycopy(links, 1, getStartPosition(1), 0, (int) volume);
			return data;
		} else {
			return null;
		}
	}

	public MDF4GenBlock[] getLnkDynamicSize() {
		if (isDynamicSize()) {
			MDF4GenBlock[] data = new MDF4GenBlock[getChannelDimensions() * 3];
			System.arraycopy(links, getStartPosition(2), data, 0, getChannelDimensions() * 3);
			return data;
		} else {
			return null;
		}
	}

	public MDF4GenBlock[] getLnkInputQuantity() {
		if (isInputQuantityFlag()) {
			MDF4GenBlock[] data = new MDF4GenBlock[getChannelDimensions() * 3];
			System.arraycopy(links, getStartPosition(3), data, 0, getChannelDimensions() * 3);
			return data;
		} else {
			return null;
		}
	}

	public MDF4GenBlock[] getLnkOutputQuantity() {
		if (isOutputQuantityFlag()) {
			MDF4GenBlock[] data = new MDF4GenBlock[3];
			System.arraycopy(links, getStartPosition(4), data, 0, 3);
			return data;
		} else {
			return null;
		}
	}

	public MDF4GenBlock[] getLnkComparisonQuantity() {
		if (isComparisonQuantityFlag()) {
			MDF4GenBlock[] data = new MDF4GenBlock[3];
			System.arraycopy(links, getStartPosition(5), data, 0, 3);
			return data;
		} else {
			return null;
		}
	}

	public MDF4GenBlock[] getLnkAxisConversion() {
		if (isAxis()) {
			MDF4GenBlock[] data = new MDF4GenBlock[channelDimensions];
			System.arraycopy(links, getStartPosition(6), data, 0, channelDimensions);
			return data;
		} else {
			return null;
		}
	}

	public MDF4GenBlock[] getLnkAxis() {
		if (isAxis() && !isFixedAxis()) {
			MDF4GenBlock[] data = new MDF4GenBlock[channelDimensions * 3];
			System.arraycopy(links, getStartPosition(7), data, 0, 3 * channelDimensions);
			return data;
		} else {
			return null;
		}
	}

	/**
	 * This function calculates the start offset for the given link section. 1 =
	 * LinkData 2 = LinkDynamicSize 3 = LinkInputQuantity 4 = LinkOutputQuantity
	 * 5 = LinkComparisionQuantity 6 = LinkAxisConversion 7 = LinkAxis
	 * 
	 * @param whichLinkSection
	 */
	private int getStartPosition(int whichLinkSection) {
		int startposition = 1;
		for (int i = 1; i < 10; i++) {
			if (whichLinkSection == i) {
				return startposition;
			}
			switch (i) {
			case 1:
				if (getStorageType() == 2) {
					startposition += volume;
				}
				break;
			case 2:
				if (isDynamicSize()) {
					startposition += 3 * channelDimensions;
				}
				break;
			case 3:
				if (isInputQuantityFlag()) {
					startposition += 3 * channelDimensions;
				}
				break;
			case 4:
				if (isOutputQuantityFlag()) {
					startposition += 3;
				}
				break;
			case 5:
				if (isComparisonQuantityFlag()) {
					startposition += 3;
				}
				break;
			case 6:
				if (isAxis()) {
					startposition += channelDimensions;
				}
				break;
			}
		}
		return -1;

	}

	public byte getArrayType() {
		return arrayType;
	}

	public void setArrayType(byte arrayType) {
		this.arrayType = arrayType;
	}

	public byte getStorageType() {
		return storageType;
	}

	public void setStorageType(byte storageType) {
		this.storageType = storageType;
	}

	public int getChannelDimensions() {
		return channelDimensions;
	}

	public void setChannelDimensions(int channelDimensions) {
		this.channelDimensions = channelDimensions;
	}

	public long getFlags() {
		return flags;
	}

	public void setFlags(long flags) {
		this.flags = flags;
	}

	public boolean isDynamicSize() {
		return BigInteger.valueOf(getFlags()).testBit(0);
	}

	public boolean isInputQuantityFlag() {
		return BigInteger.valueOf(getFlags()).testBit(1);
	}

	public boolean isOutputQuantityFlag() {
		return BigInteger.valueOf(getFlags()).testBit(2);
	}

	public boolean isComparisonQuantityFlag() {
		return BigInteger.valueOf(getFlags()).testBit(3);
	}

	public boolean isAxis() {
		return BigInteger.valueOf(getFlags()).testBit(4);
	}

	public boolean isFixedAxis() {
		return BigInteger.valueOf(getFlags()).testBit(5);
	}

	public int getByteOffsetBase() {
		return byteOffsetBase;
	}

	public void setByteOffsetBase(int byteOffsetBase) {
		this.byteOffsetBase = byteOffsetBase;
	}

	public long getInvalBitPosBase() {
		return invalBitPosBase;
	}

	public void setInvalBitPosBase(long invalBitPosBase) {
		this.invalBitPosBase = invalBitPosBase;
	}

	public long[] getDimSize() {
		return dimSize;
	}

	public void setDimSize(long[] dimSize) {
		this.dimSize = dimSize;
	}

	public double[] getAxisValues() {
		return axisValues;
	}

	public void setAxisValues(double[] axisValues) {
		this.axisValues = axisValues;
	}

	public long[] getCycCounts() {
		return cycCounts;
	}

	public void setCycCounts(long[] cycCounts) {
		this.cycCounts = cycCounts;
	}

	/**
	 * @see MDF4GenBlock#parse(byte[])
	 */
	@Override
	public void parse(byte[] content) throws IOException {
		// UINT8: StorageType
		setArrayType(content[0]);

		// UINT8: StorageType
		setStorageType(content[1]);

		// UINT16: Number of dimensions
		setChannelDimensions(MDF4Util.readUInt16(MDFParser.getDataBuffer(content, 2, 4)));

		// UINT32: Flags
		setFlags(MDF4Util.readUInt32(MDFParser.getDataBuffer(content, 4, 8)));

		// INT32 ByteOffsetBase
		setByteOffsetBase(MDF4Util.readInt32(MDFParser.getDataBuffer(content, 8, 12)));

		// UINT32 InvalBitPosBase
		setInvalBitPosBase(MDF4Util.readUInt32(MDFParser.getDataBuffer(content, 12, 16)));

		// UINT64 * D: Size of each dimension
		long[] dimSizes = new long[getChannelDimensions()];
		volume = 1;
		sumd = 0;
		for (int d = 0; d < getChannelDimensions(); d++) {
			dimSizes[d] = MDF4Util.readUInt64(MDFParser.getDataBuffer(content, 16 + 8 * d, 16 + 8 * d + 8));
			volume *= dimSizes[d];
			sumd += dimSizes[d];
		}
		setDimSize(dimSizes);

		int currpos = 16 + getChannelDimensions() * 8;

		if (isFixedAxis()) {
			// REAL * SUM(D)
			double[] axesval = new double[sumd];
			for (int d = 0; d < sumd; d++) {
				axesval[d] = MDF4Util.readReal(MDFParser.getDataBuffer(content, currpos, currpos + 8));
				currpos += 8;
			}

		}

		if (getStorageType() == 1 || getStorageType() == 2) {
			// UINT64* PROD(D)
			long[] cycCounters = new long[(int) volume];
			for (int d = 0; d < sumd; d++) {
				cycCounters[d] = MDF4Util.readUInt64(MDFParser.getDataBuffer(content, currpos, currpos + 8));
				currpos += 8;
			}

		}
	}
}
