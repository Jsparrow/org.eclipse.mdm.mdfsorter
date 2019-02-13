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
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Level;

import org.eclipse.mdm.mdfsorter.MDFAbstractParser;
import org.eclipse.mdm.mdfsorter.MDFFileContent;
import org.eclipse.mdm.mdfsorter.MDFSorter;

/**
 * A parser for the MDF file structure. This class provides functions to read a
 * file and transform it into a tree structure.
 *
 * @author EU2IYD9
 *
 */
public class MDF4Parser extends MDFAbstractParser<MDF4GenBlock> {

	public MDF4Parser(FileChannel in) {
		super(in);
	}

	/**
	 * Helper Method to read to an array from a FileChannel.
	 *
	 * @param bytes
	 *            The number of bytes to read.
	 * @param in
	 *            The FileChannel to read from.
	 * @return A byte-Array with <code>length=bytes</code> filled with the next
	 *         bytes from the Channel.
	 * @throws IOException
	 *             If an input error occurs.
	 */
	private static byte[] readBytes(int bytes, FileChannel in) throws IOException {
		ByteBuffer chunk = ByteBuffer.allocate(bytes);
		int bytesread = 0;
		if ((bytesread = in.read(chunk)) != bytes) {
			System.err.println(new StringBuilder().append("Read only ").append(bytesread).append(" Bytes instead of ").append(bytes).toString());
		}
		return chunk.array();
	}

	/**
	 * Parses the file and returns the root of the tree structure.
	 *
	 * @return The HeaderBlock that is root of the file
	 * @throws IOException
	 */
	private MDF4GenBlock parseBlocks() throws IOException {
		// Add headerblock to the queue
		MDF4GenBlock g = new MDF4GenBlock(64);
		queue.add(g);
		var ret = g;
		do {
			skipped.clear();
			while (!queue.isEmpty()) {
				MDF4GenBlock next = queue.poll();

				if (blocklist.containsKey(next.getPos())) {
					throw new RuntimeException("Duplicate Block in list.");
				}

				if (next.getPos() < lasthandled) {
					skipped.add(next);
					continue;
				}

				// parse.
				getBlockHeader(next);
				forceparse(next);

				// Add (if possible the more precise) block to the blocklist
				unfinished.remove(next.getPos());
				if (next.getPrec() != null) {
					blocklist.put(next.getPos(), next.getPrec());
				} else {
					blocklist.put(next.getPos(), next);
				}

				lasthandled = next.getPos();

				foundblocks++;
			}
			in.position(0L);
			queue.addAll(skipped);
			lasthandled = 0;
			fileruns++;
		} while (!skipped.isEmpty()); // another run is needed

		MDFSorter.log.log(Level.INFO, new StringBuilder().append("Needed ").append(fileruns).append(" runs.").toString());
		MDFSorter.log.log(Level.INFO, new StringBuilder().append("Found ").append(blocklist.size()).append(" blocks.").toString());
		MDFSorter.log.log(Level.FINE, "ValidatorListSize: " + (foundblocks + 1)); // Expected
																					// number
		// of node in Vector
		// MDFValidators
		// node list for
		// this file

		return ret;
	}

	/**
	 * Reads the Block Header of an MDFGenBlock, where only the position value
	 * is set. The header includes ID, linkcount and length and also the links
	 * of the link section!
	 *
	 * @throws IOException
	 *             If a reading error occurs.
	 */
	private void getBlockHeader(MDF4GenBlock start) throws IOException {
		in.position(start.getPos());
		byte[] head = readBytes(24, in);
		// Read header of this block
		// String blktyp = MDFTypesHelper.getSTRING(head, 0, 4);
		String blktyp = MDF4Util.readCharsUTF8(getDataBuffer(head, 0, 4), 4);
		start.setId(blktyp);
		long blklength = MDF4Util.readUInt64(getDataBuffer(head, 8, 16));
		start.setLength(blklength);
		long blklinkcount = MDF4Util.readUInt64(getDataBuffer(head, 16, 24));
		start.setLinkCount(blklinkcount);
		// Read links and create new blocks
		head = readBytes((int) (blklinkcount * 8), in);
		for (int i = 0; i < blklinkcount; i++) {
			long nextlink = MDF4Util.readLink(getDataBuffer(head, i * 8, (i + 1) * 8));
			if (nextlink != 0) {
				if (blocklist.containsKey(nextlink)) {
					start.addLink(i, blocklist.get(nextlink));
					foundblocks++;
				} else if (unfinished.containsKey(nextlink)) {
					start.addLink(i, unfinished.get(nextlink));
					foundblocks++;
				} else {
					MDF4GenBlock child = new MDF4GenBlock(nextlink);
					start.addLink(i, child);
					queue.add(child);
					unfinished.put(nextlink, child);
				}
			}
		}
	}

	/**
	 * Changes a section of a byte-Array to a ByteBuffer, which can be used in
	 * the parsing Methods of MDF4Util, for example to redeem an int-Value.
	 *
	 * @param data
	 *            The byte-Array, containing the data
	 * @param start
	 *            The first index of the section.
	 * @param end
	 *            The first index not included in the section.
	 * @return The section of the array, as a ByteBuffer.
	 */
	public static ByteBuffer getDataBuffer(byte[] data, int start, int end) {
		if (start >= 0 && end <= data.length) {
			return java.nio.ByteBuffer.wrap(Arrays.copyOfRange(data, start, end));
		} else {
			// just for testing
			throw new ArrayIndexOutOfBoundsException(
					new StringBuilder().append("Tried to access bytes ").append(start).append(" to ").append(end).append("with array length ")
							.append(data.length).toString());
		}
	}

	/**
	 * This method creates the corresponding specialized block and calls the
	 * parse method on Block blk
	 *
	 * @param blk
	 * @throws IOException
	 */
	private void forceparse(MDF4GenBlock blk) throws IOException {
		long sectionsize = blk.getLength() - 24L - 8L * blk.getLinkCount();

		byte[] content = null;
		// parse special blocktypes more precisely.

		MDF4GenBlock sp = null;
		switch (blk.getId()) {
		case "##AT":
			// sp = new ATBLOCK(blk);
			break;
		case "##CA":
			// sp = new CABLOCK(blk);
			break;
		// throw new UnsupportedOperationException("CA Block found!");
		case "##CC":
			// sp = new CCBLOCK(blk);
			break;
		case "##CG":
			sp = new CGBLOCK(blk);
			break;
		case "##CN":
			sp = new CNBLOCK(blk);
			break;
		case "##CH":
			// sp = new CHBLOCK(blk);
			break;
		case "##DG":
			sp = new DGBLOCK(blk);
			break;
		case "##DL":
			sp = new DLBLOCK(blk);
			break;
		case "##DT":
		case "##SD":
		case "##RD":
			sp = new DTBLOCK(blk);
			break;
		case "##DZ":
			sp = new DZBLOCK(blk);
			break;
		case "##EV":
			// sp = new EVBLOCK(blk);
			break;
		case "##FH":
			sp = new FHBLOCK(blk);
			break;
		case "##HD":
			sp = new HDBLOCK(blk);
			break;
		case "##HL":
			sp = new HLBLOCK(blk);
			break;
		case "##MD":
			sp = new MDBLOCK(blk);
			break;
		case "##SI":
			// sp = new SIBLOCK(blk);
			break;
		case "##SR":
			// sp = new SRBLOCK(blk);
			break;
		case "##TX":
			sp = new TXBLOCK(blk);
			break;
		default:
			System.err.println(new StringBuilder().append("Unknown block of type ").append(blk.getId()).append(" found.").toString());
		}

		if ("##DZ".equals(blk.getId())) {
			content = readBytes(24, in);
		} else if (sp != null) {
			content = readBytes((int) sectionsize, in);
		}

		if (sp != null) {
			sp.parse(content);
		}
	}

	private LinkedList<MDF4GenBlock> getBlocklist() {
		LinkedList<MDF4GenBlock> writelist = new LinkedList<>();
		while (!blocklist.isEmpty()) {
			writelist.addFirst(blocklist.pollLastEntry().getValue());

		}
		return writelist;
	}

	@Override
	public MDFFileContent<MDF4GenBlock> parse() throws IOException {

		var tree = parseBlocks();

		// 2. run through tree, and change all blocks to their more special
		// precedessors if they
		// exist
		if (tree.getPrec() != null) {
			tree = tree.getPrec();
		}

		var structlist = getBlocklist();
		structlist.forEach(MDF4GenBlock::updateChildren);

		return new MDFFileContent<>(in, tree, structlist, false);
	}

}
