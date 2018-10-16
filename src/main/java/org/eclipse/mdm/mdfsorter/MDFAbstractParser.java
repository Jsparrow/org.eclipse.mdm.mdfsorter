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
import java.nio.channels.FileChannel;
import java.util.PriorityQueue;
import java.util.TreeMap;

public abstract class MDFAbstractParser<MDFXBlock extends MDFGenBlock> {
	abstract public MDFFileContent<MDFXBlock> parse() throws IOException;

	/**
	 * Queue storing all known links to blocks that have not been parsed yet.
	 */
	protected PriorityQueue<MDFXBlock> queue;

	/**
	 * Queue, containing all blocks that have been skipped, because their
	 * address is lower, that the current position of the reader in the file.
	 * These blocks will be parsed during the next run through the file.
	 */
	protected PriorityQueue<MDFXBlock> skipped;

	/**
	 * The input stream to the file.
	 */
	protected FileChannel in;

	/**
	 * Address of the last block which has been parsed
	 */
	protected long lasthandled = 0;

	// Debug purposes
	protected int fileruns = 0, foundblocks = 0;

	/**
	 * This map contains all MDFGenBlocks that have not been parsed, but their
	 * address is known. Key: The position of the block in the file. Value: The
	 * block itself. After parsing the block is moved into blocklist.
	 */
	protected TreeMap<Long, MDFXBlock> unfinished;

	/**
	 * This map contains all MDFGenBlocks that have been parsed. Key: The
	 * position of the block in the file. Value: The block itself.
	 */
	protected TreeMap<Long, MDFXBlock> blocklist;

	public MDFAbstractParser(FileChannel in) {
		this.in = in;
		// Initialize Datastructures
		queue = new PriorityQueue<MDFXBlock>();
		skipped = new PriorityQueue<MDFXBlock>();
		blocklist = new TreeMap<Long, MDFXBlock>();
		unfinished = new TreeMap<Long, MDFXBlock>();
	}

}
