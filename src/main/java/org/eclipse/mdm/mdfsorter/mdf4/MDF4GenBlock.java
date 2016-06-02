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
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.mdm.mdfsorter.ArgumentStruct;
import org.eclipse.mdm.mdfsorter.MDFCompatibilityProblem;
import org.eclipse.mdm.mdfsorter.MDFGenBlock;
import org.eclipse.mdm.mdfsorter.MDFProblemType;

/**
 * MDF Generic Block (MDFGenBlock): Base class for all blocks. If only the
 * address of a block is known, an MDFGenBlock can be created and later be
 * replaced by a more specific block.
 *
 * @author Tobias Leemann
 */
public class MDF4GenBlock extends MDFGenBlock
implements Comparable<MDF4GenBlock> {

	// Number of links
	// UINT64
	private long linkCount;

	/**
	 * Array storing this block's links, initialized after a call to
	 * setLinkCount
	 */
	protected MDF4GenBlock links[];

	private MDF4GenBlock precedessor;

	/**
	 * false, if nothing has been done with this block and it can be written
	 * directly from the input file to the output as it was (maybe some link
	 * addresses will change though)
	 */
	private boolean touched;

	/**
	 * List with problems with this node
	 */
	private List<MDFCompatibilityProblem> problems;

	/**
	 * Constructor, creates an unspecified type of MDF Block.
	 *
	 * @param pos
	 *            The position of the block within the MDF file.
	 */
	public MDF4GenBlock(long pos) {
		super(pos);
	}

	public MDF4GenBlock() {
		super();
	}

	@Override
	public int getLinkCount() {
		return (int) linkCount;
	}

	public void setLinkCount(long linkCount) {
		this.linkCount = linkCount;
		links = new MDF4GenBlock[(int) linkCount];
	}

	public MDF4GenBlock getPrec() {
		return precedessor;
	}

	public void setPrec(MDF4GenBlock prec) {
		precedessor = prec;
	}

	public MDF4GenBlock[] getLinks() {
		return links;
	}

	@Override
	public MDF4GenBlock getLink(int i) {
		if (i >= 0 && i < linkCount) {
			return links[i];
		} else {
			System.err.println("Invalid getLink index.");
			return null;
		}

	}

	public void setLink(int i, MDF4GenBlock newblk) {
		if (i >= 0 && i < linkCount) {
			links[i] = newblk;
		} else {
			System.err.println("Invalid getLink index.");
		}
	}

	public void setLinks(MDF4GenBlock[] links) {
		this.links = links;
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
	public void addLink(int index, MDF4GenBlock child) {
		links[index] = child;
	}

	public boolean gettouched() {
		return touched;
	}

	/**
	 * Clears this node of all problems and marks it, as part of a problem
	 */
	public void touch() {
		touched = true;
		if (problems != null) {
			problems = null;
		}
	}

	@Override
	public void addProblem(MDFCompatibilityProblem m) {
		if (touched) {
			return;
		}
		if (problems == null) {
			problems = new LinkedList<MDFCompatibilityProblem>();
		}
		problems.add(m);
	}

	@Override
	public List<MDFCompatibilityProblem> getProblems() {
		return problems;
	}

	public void replaceLink(MDF4GenBlock old, MDF4GenBlock newblk) {
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
		return "BLOCK [pos=" + pos + ", id=" + id + ", length=" + length
				+ ", linkCount=" + linkCount + "]";
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
		if (!(obj instanceof MDF4GenBlock)) {
			return false;
		}

		MDF4GenBlock other = (MDF4GenBlock) obj;
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
	public int compareTo(MDF4GenBlock arg) {
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
	 * Returns the block type string at given position.
	 *
	 * @param channel
	 *            The channel to read from.
	 * @param pos
	 *            The position within the channel.
	 * @return The block type as string.
	 * @throws IOException
	 *             Error reading block type.
	 */
	protected static String getBlockType(SeekableByteChannel channel, long pos)
			throws IOException {
		// read block header
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		channel.position(pos);
		channel.read(bb);
		bb.rewind();
		return MDF4Util.readCharsISO8859(bb, 4);
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
	 *             If an I/O error occurs.
	 */
	public void parse(byte[] content) throws IOException {
		throw new UnsupportedOperationException(
				"parse not valid on unspecified block.");
	}

	/**
	 * Returns a Byte-Array with the values of the other fields of a specific
	 * subclass of MDFGenBlock. The Data Section of a data block is not
	 * returned, because it would be too large.
	 *
	 * @return The Byte-Array with the correct values set in this block.
	 * @throws IOException
	 *             If an I/O error occurs.
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
		byte[] ret = new byte[(int) (24L + 8 * linkCount)];
		byte[] idtext = getId().getBytes();
		if (idtext.length != 4) {
			System.err.println("Invalid id bytes.");
		}
		System.arraycopy(idtext, 0, ret, 0, 4);

		byte[] length = MDF4Util.getBytesUInt64(this.length);
		System.arraycopy(length, 0, ret, 8, 8);

		byte[] linkcount = MDF4Util.getBytesUInt64(linkCount);
		System.arraycopy(linkcount, 0, ret, 16, 8);

		// Links are not set, the will be during the "Update Links"-Step.
		return ret;
	}

	/**
	 * Replaces each child block with its more precise precedessor stored in
	 * precedessor.
	 */
	public void updateChildren() {
		for (int i = 0; i < getLinkCount(); i++) {
			if (links[i] != null) {
				if (links[i].getPrec() != null) {
					links[i] = links[i].getPrec();
				}
			}
		}
	}

	/**
	 * Sets touched= true for all nodes in this node's subtree.
	 */
	public void startRecursiveTouch() {
		for (int i = 0; i < getLinkCount(); i++) {
			if (links[i] != null) {
				links[i].recursiveTouch();
			}
		}
	}

	/**
	 * Sets touched= true for this node AND all nodes in this node's subtree.
	 */
	public void recursiveTouch() {
		touch();
		startRecursiveTouch();
	}

	/**
	 * This method checks this node for any problems.
	 *
	 * @param args
	 *            The programm arguments for this call.
	 */
	@Override
	public void analyseProblems(ArgumentStruct args) {
		switch (id) {
		case "##HL":
			if (!(this instanceof HLBLOCK)) {
				throw new RuntimeException("Error parsing HL block.");
			}
			MDF4GenBlock dlblk = ((HLBLOCK) this).getLnkDlFirst();
			if (!(dlblk instanceof DLBLOCK)) {
				throw new RuntimeException("Error parsing HL block.");
			}
			// is sublist improvable? If yes, we have a problem here.
			if (((DLBLOCK) dlblk).isImproveable(args)) {
				startRecursiveTouch();
				addProblem(new MDFCompatibilityProblem(
						MDFProblemType.LINKED_DATALIST_PROBLEM, this));
			} else {
				if (args.unzip) {
					// we have a problem anyway, if data should be unzipped.
					startRecursiveTouch();
					addProblem(new MDFCompatibilityProblem(
							MDFProblemType.LINKED_DATALIST_PROBLEM, this));
				}
			}
			break;
		case "##DL":
			if (!(this instanceof DLBLOCK)) {
				throw new RuntimeException("Error parsing DL block.");
			}
			if (((DLBLOCK) this).isImproveable(args)) {
				startRecursiveTouch();
				addProblem(new MDFCompatibilityProblem(
						MDFProblemType.LINKED_DATALIST_PROBLEM, this));
			}
			break;
		case "##DG":
			// check for unsorted data
			if (!(this instanceof DGBLOCK)) {
				throw new RuntimeException("Error parsing DG block.");
			}
			DGBLOCK dgblk = (DGBLOCK) this;
			CGBLOCK blk = (CGBLOCK) dgblk.getLnkCgFirst();
			if (blk.getLnkCgNext() != null) {
				// more than one channel group per datagroup! Unsorted.
				addProblem(new MDFCompatibilityProblem(
						MDFProblemType.UNSORTED_DATA_PROBLEM, this));
				// which block will be touched?
				// all channel groups!
				do {
					blk.touch();
				} while ((blk = (CGBLOCK) blk.getLnkCgNext()) != null);
				dgblk.getLnkData().recursiveTouch();
			}
			break;
		case "##DZ":
			if (args.unzip) {
				addProblem(new MDFCompatibilityProblem(
						MDFProblemType.ZIPPED_DATA_PROBLEM, this));
			}
			break;
		case "##DT":
		case "##SD":
		case "##RD":
			if (!args.unzip) {
				addProblem(new MDFCompatibilityProblem(
						MDFProblemType.UNZIPPED_DATA_PROBLEM, this));
			}
			break;
		}

	}

}