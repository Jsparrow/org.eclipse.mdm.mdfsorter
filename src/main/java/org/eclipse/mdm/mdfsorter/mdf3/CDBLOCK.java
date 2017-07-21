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

/**
 * @author Tobias Leemann The Channel Dependency Block
 */
public class CDBLOCK extends MDF3GenBlock {

	/**
	 * Dependency Type
	 */
	private int dependancyType;

	/**
	 * UINT16: The number of dependencies.
	 */
	private int noDependencies;

	/**
	 * INT16[]: Size of each dimension, only valid for N-dimensional dependency.
	 */
	private int dimensionsSize[];

	/**
	 * Parse a CDBLOCK from an existing MDF3GenBlock
	 *
	 * @param parent
	 *            The already existing MDF3 Generic Block.
	 */
	public CDBLOCK(MDF3GenBlock parent) {
		super(parent.getPos(), parent.isBigEndian());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	public int getDependancyType() {
		return dependancyType;
	}

	public void setDependancyType(int dependancyType) {
		this.dependancyType = dependancyType;
	}

	public int getNoDependencies() {
		return noDependencies;
	}

	public void setNoDependencies(int noDependencies) {
		this.noDependencies = noDependencies;
	}

	public int[] getDimensionsSize() {
		return dimensionsSize;
	}

	public void setDimensionsSize(int[] dimensionsSize) {
		this.dimensionsSize = dimensionsSize;
	}

	@Override
	public void parse(byte[] content) throws IOException {
		// UINT16 Dependency type
		// 0 = none
		// 1 = linear
		// 2 = matrix dependency
		// 256 + N: N-dimensional
		setDependancyType(MDF3Util.readUInt16(MDFParser.getDataBuffer(content, 0, 2), isBigEndian()));

		// UINT16 number of dependencies
		setNoDependencies(MDF3Util.readUInt16(MDFParser.getDataBuffer(content, 2, 4), isBigEndian()));

		if (dependancyType > 256) { // if dependency type is n-dimensional, the
									// sizes of each dimesions are stored after
									// the links.
			if (content.length > 4 + getNoDependencies() * 4) {
				int numvalues = (content.length - (4 + getNoDependencies() * 4)) / 2;
				int[] sizes = new int[numvalues];
				int readptr = 4 + getNoDependencies() * 4;
				for (int i = 0; i < numvalues; i++) {
					sizes[i] = MDF3Util.readUInt16(MDFParser.getDataBuffer(content, readptr, readptr + 2), bigendian);
					readptr += 2;
				}
			}
		}
	}

	@Override
	public void updateLinks(RandomAccessFile r) throws IOException {
		r.seek(getOutputpos() + 4L + 4L);
		MDF3GenBlock linkedblock;
		for (int i = 0; i < getLinkCount(); i++) {
			// position of links, see specification.
			if ((linkedblock = getLink(i)) != null) {
				r.write(MDF3Util.getBytesLink(linkedblock.getOutputpos(), isBigEndian()));
			} else {
				r.write(MDF3Util.getBytesLink(0, isBigEndian()));
			}
		}
	}
}
