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
import java.nio.ByteBuffer;

/**
 * <p>
 * THE CHANNEL HIERARCHY BLOCK <code>CHBLOCK</code>
 * </p>
 * The CHBLOCKs describe a logical ordering of the channels in a tree-like
 * structure. This only serves to structure the channels and is totally
 * independent to the data group and channel group structuring. A channel even
 * may not be referenced at all or more than one time.<br>
 * Each CHBLOCK can be seen as a node in a tree which has a number of channels
 * as leafs and which has a reference to its next sibling and its first child
 * node (both CHBLOCKs). The reference to a channel is always a triple link to
 * the CNBLOCK of the channel and its parent CGBLOCK and DGBLOCK. Each CHBLOCK
 * can have a name.
 *
 * @author Christian Rechner, Tobias Leemann
 */
public class CHBLOCK extends MDF4GenBlock {

	/**
	 * Parse a CHBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public CHBLOCK(MDF4GenBlock parent) {
		super(parent.getPos());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	/**
	 * Number of N Channels referenced by this CHBLOCK
	 */
	private long elementCount;

	/**
	 * Type of the hierachy this CHBLOCK maintaines.
	 */
	private byte hirarchyType;

	// Getters and Setters
	// Link to next CHBLOCK
	public MDF4GenBlock getLnkChNext() {
		return links[0];
	}

	// Link to first child CHBLOCK
	public MDF4GenBlock getLnkChFirst() {
		return links[1];
	}

	// Link to name of this block
	public MDF4GenBlock getLnkTxName() {
		return links[2];
	}

	// Link to MDBlock
	public MDF4GenBlock getLnkMdComment() {
		return links[3];
	}

	// Links to DG, CG, CN of element i
	public MDF4GenBlock getLnkElement(int i) {
		if (i >= getElementCount()) {
			System.out.println(new StringBuilder().append("Invalid acces to element ").append(i).append(".").toString());
			return null;
		}
		MDF4GenBlock[] ret = new MDF4GenBlock[3];
		System.arraycopy(links, 4 + 3 * i, ret, 0, 3);
		return links[3];
	}

	public long getElementCount() {
		return elementCount;
	}

	public void setElementCount(long elementCount) {
		this.elementCount = elementCount;
	}

	public byte getHirarchyType() {
		return hirarchyType;
	}

	public void setHirarchyType(byte hirarchyType) {
		this.hirarchyType = hirarchyType;
	}

	/**
	 * Reads a CHBLOCK from its content.
	 *
	 * @param content
	 *            The data section of this block
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	@Override
	public void parse(byte[] content) throws IOException {
		ByteBuffer bb = ByteBuffer.wrap(content);
		// UINT32: Number of Channels Referenced.
		setElementCount(MDF4Util.readUInt32(bb));

		// UINT8: Hierarchy type
		setHirarchyType(MDF4Util.readUInt8(bb));
	}

}
