/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter;

import java.nio.channels.FileChannel;
import java.util.LinkedList;

/**
 * Datastructure containing the root, node list, and an input stream of an
 * MDF-File.
 *
 * @author Tobias Leemann
 *
 */
public class MDFFileContent<T extends MDFGenBlock> {

	/**
	 * The FileChannel from which content can be obtained.
	 */
	public FileChannel input;

	/**
	 * Root of the MDF-File-Tree (ID-Block is skipped, root is a Block of type
	 * "HD".
	 */
	private final T root;

	/**
	 * Note: List must not contain an ID-Block.
	 */
	private final LinkedList<T> list;

	/**
	 * True, if the parsed File is of MDF 3.x Format; False if it is an MDF 4.x
	 * file. The value of this attribute also defines the generic type parameter
	 * of this object. If this attribute is set to true, the FileContent can be
	 * securely casted to FileContent&lt;MDF3GenBlock&gt;. If this attribute is
	 * set to false, the FileContent can be securely casted to
	 * FileContent&lt;MDF4GenBlock&gt;.
	 */
	private boolean isMDF3 = false;

	/**
	 * Only used in MDF3. BigEndian value from the IDBLOCK. True if numbers are
	 * stored as LittleEndian. False if they are stored BigEndian encoding.
	 */
	private boolean isBigEndian = false;

	/**
	 * Creates a new instance of the MDFFileContent data structure.
	 *
	 * @param in
	 *            Stream to the file.
	 * @param blk
	 *            Root of the MDFFile Tree, normally a HDBLOCK.
	 * @param list
	 *            List of all blocks in the file, ordered by ascending
	 *            addresses.
	 * @param isMDF3
	 *            True, if the file is of version 3.x, false if it is 4.x.
	 */
	public MDFFileContent(FileChannel in, T blk, LinkedList<T> list, boolean isMDF3) {
		this.input = in;
		this.root = blk;
		this.list = list;
		this.isMDF3 = isMDF3;
	}

	// Getters and Setters

	public T getRoot() {
		return root;
	}

	public LinkedList<T> getList() {
		return list;
	}

	public FileChannel getInput() {
		return input;
	}

	public void setInput(FileChannel input) {
		this.input = input;
	}

	public boolean isMDF3() {
		return isMDF3;
	}

	public void setMDF3(boolean isMDF3) {
		this.isMDF3 = isMDF3;
	}

	public boolean isBigEndian() {
		return isBigEndian;
	}

	public void setBigEndian(boolean isBigEndian) {
		this.isBigEndian = isBigEndian;
	}

}
