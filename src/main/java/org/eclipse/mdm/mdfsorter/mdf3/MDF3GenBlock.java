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

import org.eclipse.mdm.mdfsorter.ArgumentStruct;
import org.eclipse.mdm.mdfsorter.MDFCompatibilityProblem;
import org.eclipse.mdm.mdfsorter.MDFGenBlock;
import org.eclipse.mdm.mdfsorter.MDFProblemType;

public class MDF3GenBlock extends MDFGenBlock implements Comparable<MDF3GenBlock> {

	// Number of links
	private int linkCount;

	/**
	 * Array storing this block's links, initialized after a call to
	 * setLinkCount
	 */
	protected MDF3GenBlock links[];

	private MDF3GenBlock precedessor;

	/**
	 * false, if nothing has been done with this block and it can be written
	 * directly from the input file to the output as it was (maybe some link
	 * addresses will change though)
	 */
	private boolean touched;

	/**
	 * Constructor, creates an unspecified type of MDF Block.
	 *
	 * @param pos
	 *            The position of the block within the MDF file.
	 */

	/**
	 * True, if this block is part of an bigendian file. False if not.
	 */
	protected boolean bigendian = false;

	public MDF3GenBlock(long pos, boolean bigendian) {

		super(pos);
		// Check with UINT32.Maxvalue.
		if (pos > 2L * Integer.MAX_VALUE + 1L) {
			throw new IllegalArgumentException("Block address " + pos + " is too large for MDF3 format.");
		}
		this.bigendian = bigendian;
	}

	public MDF3GenBlock(boolean bigendian) {
		super();
		this.bigendian = bigendian;
	}

	// Getters and Setters
	@Override
	public long getPos() {
		return pos;
	}

	@Override
	public long getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	@Override
	public void setLength(long length) {
		this.length = length;
	}

	@Override
	public int getLinkCount() {
		return linkCount;
	}

	public void setLinkCount(int linkCount) {
		this.linkCount = linkCount;
		links = new MDF3GenBlock[linkCount];
	}

	public boolean isBigEndian() {
		return bigendian;
	}

	public void setBigendian(boolean bigendian) {
		this.bigendian = bigendian;
	}

	public MDF3GenBlock getPrec() {
		return precedessor;
	}

	public void setPrec(MDF3GenBlock prec) {
		precedessor = prec;
	}

	public MDF3GenBlock[] getLinks() {
		return links;
	}

	@Override
	public MDF3GenBlock getLink(int i) {
		if (i >= 0 && i < linkCount) {
			return links[i];
		} else {
			System.err.println("Invalid getLink index.");
			return null;
		}

	}

	public void setLink(int i, MDF3GenBlock newblk) {
		if (i >= 0 && i < linkCount) {
			links[i] = newblk;
		} else {
			System.err.println("Invalid getLink index.");
		}
	}

	public void setLinks(MDF3GenBlock[] links) {
		this.links = links;
	}

	public void moreLinks(int newcount) {
		MDF3GenBlock[] newlinks = new MDF3GenBlock[newcount];
		System.arraycopy(links, 0, newlinks, 0, getLinkCount());
		links = newlinks;
		linkCount = newcount;
	}

	/**
	 * Changes the link, at <code>index</code>, to the value of
	 * <code>child</code>. This method must only be used after a call of
	 * setLinkCount().
	 *
	 * @param index
	 *            The link to be changed.
	 * @param child
	 *            The new link
	 */
	public void addLink(int index, MDF3GenBlock child) {
		links[index] = child;
	}

	@Override
	public long getOutputpos() {
		return outputpos;
	}

	@Override
	public void setOutputpos(long outputpos) {
		this.outputpos = outputpos;
	}

	public boolean gettouched() {
		return touched;
	}

	public void touch() {
		touched = true;
	}

	public void replaceLink(MDF3GenBlock old, MDF3GenBlock newblk) {
		for (int i = 0; i < linkCount; i++) {
			if (links[i] != null && links[i].equals(old)) {
				links[i] = newblk;
				break;
			}

		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BLOCK [pos=" + pos + ", id=" + id + ", length=" + length + ", linkCount=" + linkCount + "]";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (pos ^ pos >>> 32);
		return result;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof MDF3GenBlock)) {
			return false;
		}

		MDF3GenBlock other = (MDF3GenBlock) obj;
		if (pos != other.pos) {
			return false;
		}
		return true;
	}

	/**
	 * Compares Blocks according to their start address, lower start addresses
	 * are ranked higher.
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(MDF3GenBlock arg) {
		long val = pos - arg.pos;
		if (val == 0) {
			return 0;
		} else if (val > 0) {
			return 1;
		} else {
			return -1;
		}
	}

	/**
	 * Parses the body section of a block. For a subclass of MDFGenBlock, this
	 * method should fill all other fields with the correct values. DataBlock's
	 * content should normally not be included in content, because the amount of
	 * Data could be too large.
	 *
	 * @param content
	 *            The Bytes of the DataSection
	 * @throws IOException
	 *             If an input error occurs.
	 */
	public void parse(byte[] content) throws IOException {
		throw new UnsupportedOperationException("parse not valid on unspecified block.");
	}

	/**
	 * Returns a Byte-Array with the values of the other fields of a specific
	 * subclass of MDFGenBlock. The Data Section of a data block is not
	 * returned, because it would be too large.
	 *
	 * @return The Byte-Array with the correct values set in this block.
	 * @throws IOException
	 *             If an input error occurs.
	 */
	@Override
	public byte[] getBodyBytes() throws IOException {
		// Body is not defined for generic Block.
		return new byte[0];
	}

	/**
	 * This methode writes the header section of this block to a byte array,
	 * which can be written to the file.
	 *
	 * @return Returns a Byte-Array with the values of the header fields (ID,
	 *         length, linkcount)
	 */
	@Override
	public byte[] getHeaderBytes() {
		byte[] ret = new byte[4 + getLinkCount() * 4];
		byte[] idtext = getId().getBytes();
		if (idtext.length != 2) {
			System.err.println("Invalid id bytes.");
		}
		System.arraycopy(idtext, 0, ret, 0, 2);

		byte[] length = MDF3Util.getBytesUInt16((int) this.length, isBigEndian());
		System.arraycopy(length, 0, ret, 2, 2);

		return ret;
	}

	/**
	 * Replaces each block with its more precise precedessor stored in
	 * precedessor.
	 */
	public void updateChildren() {
		for (int i = 0; i < getLinkCount(); i++) {
			if (links[i] != null && links[i].getPrec() != null) {
				links[i] = links[i].getPrec();
			}
		}
	}

	/**
	 * This method checks this node for any problems.
	 *
	 * @param args
	 *            The programm arguments for this call.
	 */
	@Override
	public void analyseProblems(ArgumentStruct args) {
		// TODO Auto-generated method stub
		if (this instanceof DGBLOCK) {
			DGBLOCK dgblk = (DGBLOCK) this;

			CGBLOCK blk = (CGBLOCK) dgblk.getLnkCgFirst();
			if (blk.getLnkCgNext() != null) { // more than one channel group per
				// datagroup! Unsorted.
				addProblem(new MDFCompatibilityProblem(MDFProblemType.UNSORTED_DATA_PROBLEM, this));
				// which block will be touched?
				// all channel groups!
				dgblk.getLnkData().touch();
				do {
					blk.touch();
				} while ((blk = (CGBLOCK) blk.getLnkCgNext()) != null);

			}
		}
	}

	/**
	 * Default implementation for the links update. In an MDF3 File, each block
	 * updates the links itself, because the default block structure (header,
	 * links, data) may not be accurate for each block
	 *
	 * @param r
	 *            The opened output file as RandomAccessFile.
	 * @throws IOException
	 *             If an output error occurs.
	 */
	public void updateLinks(RandomAccessFile r) throws IOException {
		// set position to start of Block link section
		r.seek(getOutputpos() + 4L);
		MDF3GenBlock linkedblock;
		for (int i = 0; i < getLinkCount(); i++) {
			if ((linkedblock = getLink(i)) != null) {
				r.write(MDF3Util.getBytesLink(linkedblock.getOutputpos(), isBigEndian()));
			} else {
				r.write(MDF3Util.getBytesLink(0, isBigEndian()));
			}
		}
	}
}
