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


package org.eclipse.mdm.mdfsorter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.zip.DataFormatException;

public abstract class MDFAbstractProcessWriter<MDFXGenBlock extends MDFGenBlock> {
	/**
	 * maximum size of one block that can be read into memory and then written
	 * out again. (e.g. a 2GB block cannot be read and written in one piece)
	 */
	public static final int MAX_OUTPUTBLOCKSIZE = 256 * 1024;

	/**
	 * The file tree of the Input file.
	 */
	protected MDFFileContent<MDFXGenBlock> filestructure;

	/**
	 * Programm Arguments and Flags.
	 */
	protected ArgumentStruct args;

	/**
	 * List with all block written to the file, ordered by ascending addresses.
	 * This list is used to update all links after the first write operation has
	 * been completed.
	 */
	protected LinkedList<MDFXGenBlock> writtenblocks;

	/**
	 * Write position in the output file
	 */
	protected long writeptr = 0;

	/**
	 * Cache to collect data prior to write.
	 */
	protected WriteDataCache myCache;

	public abstract void processAndWriteOut() throws IOException, DataFormatException;

	public abstract void writeSpacer(long length);

	public MDFFileContent<MDFXGenBlock> getFilestructure() {
		return filestructure;
	}

	public void setFilestructure(MDFFileContent<MDFXGenBlock> filestructure) {
		this.filestructure = filestructure;
	}

	public ArgumentStruct getArgs() {
		return args;
	}

	public void setArgs(ArgumentStruct args) {
		this.args = args;
	}

	public LinkedList<MDFXGenBlock> getWrittenblocks() {
		return writtenblocks;
	}

	public void setWrittenblocks(LinkedList<MDFXGenBlock> writtenblocks) {
		this.writtenblocks = writtenblocks;
	}

	public long getWriteptr() {
		return writeptr;
	}

	public boolean checkProblems() {
		for (MDFXGenBlock blk : filestructure.getList()) {
			blk.analyseProblems(args);
		}
		boolean ret = false;
		for (MDFXGenBlock blk : filestructure.getList()) {
			for (int i = 0; i < blk.getLinkCount(); i++) {
				if (blk.getLink(i) != null && blk.getLink(i).getProblems() != null) {
					for (MDFCompatibilityProblem p : blk.getLink(i).getProblems()) {
						p.setParentnode(blk);
					}
					ret = true;
				}
			}
		}
		return ret;
	}

	/**
	 * Copy a block.
	 *
	 * @param blk
	 *            The block to copy.
	 * @param reader
	 *            Stream to the InputFile.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public void copyBlock(MDFXGenBlock blk, FileChannel reader) throws IOException {
		reader.position(blk.getPos());
		blk.setOutputpos(writeptr);

		// write file out in 64k blocks
		long length = blk.getLength();
		// Number of Bytes written
		long written = 0;

		do {
			int bytesread;
			if (written + MAX_OUTPUTBLOCKSIZE > length) {
				ByteBuffer custombuffer = ByteBuffer.allocate((int) (length - written));
				bytesread = reader.read(custombuffer);
				performPut(custombuffer, bytesread, false);
			} else {
				ByteBuffer buffer = ByteBuffer.allocate(MAX_OUTPUTBLOCKSIZE);
				bytesread = reader.read(buffer);
				performPut(buffer, bytesread, false);
			}
			written += bytesread;
		} while (written < length);
		if (length != written) {
			throw new IOException("written length not equal to blocklength: " + length + "/" + written);
		}
		// insert space if length%8!=0
		if (length % 8 != 0) {
			writeSpacer(length);
		}
		writtenblocks.addLast(blk);
	}

	/**
	 * Writes a block to the File. This method may only be used if blk already
	 * has a set ID, length and linkcount. blk must also override the method
	 * getBodyBytes of MDFGenBlock, if any fields shall be written after the
	 * Header.
	 *
	 * @param blk
	 *            The Block to be written
	 * @param appendData
	 *            Data section of this block
	 * @throws IOException
	 *             If an Output error occurs.
	 */
	public void writeBlock(MDFXGenBlock blk, byte[] appendData) throws IOException {
		blk.setOutputpos(writeptr);

		performPut(blk.getHeaderBytes());
		performPut(blk.getBodyBytes());
		if (appendData != null) {
			performPut(appendData);
		}
		writeSpacer(blk.getLength());
		writtenblocks.add(blk);
	}

	/**
	 * Puts <code>data</code> in the Buffer <code>buf</code> and increases the
	 *
	 * @param data
	 *            The data to be written.
	 */
	public void performPut(byte[] data) {
		if (data != null && data.length != 0) {
			writeptr += data.length;
			myCache.put(data);
		}
	}

	/**
	 * Puts the first <code>len</code> bytes read from <code>buf</code> in the
	 * WriteCache and increases the writepointer.
	 *
	 * @param buf
	 *            A ByteBuffer with a position set to the start of the data.
	 * @param len
	 *            The number of bytes from data to be written (starting at byte
	 *            0)
	 * @param reused
	 *            True, if the ByteBuffer buf, may be overwritten or its
	 *            position changed and data needs to be copied before.
	 */
	public void performPut(ByteBuffer buf, int len, boolean reused) {
		writeptr += len;
		myCache.put(buf, len, reused);
	}
}
