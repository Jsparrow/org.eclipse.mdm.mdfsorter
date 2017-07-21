/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

abstract public class MDFGenBlock {

	/** Header section */

	// Block type identifier, e.g. "##HD", "##MD", in MDF4 or "HD", "CG" in MDF3
	protected String id = "";

	// the position of the block within the input MDF file
	protected final long pos;

	// Length of block
	// UINT64
	protected long length;

	/**
	 * List with problems with this node
	 */
	private List<MDFCompatibilityProblem> problems;

	// the position of the block within the output MDF file
	protected long outputpos;

	public MDFGenBlock() {
		pos = 0;
	}

	public MDFGenBlock(long pos) {
		this.pos = pos;
	}

	public abstract void analyseProblems(ArgumentStruct args);

	public abstract MDFGenBlock getLink(int i);

	public abstract int getLinkCount();

	public abstract byte[] getHeaderBytes();

	public abstract byte[] getBodyBytes() throws IOException;

	// Getters and Setters
	public void addProblem(MDFCompatibilityProblem m) {
		if (problems == null) {
			problems = new LinkedList<MDFCompatibilityProblem>();
		}
		problems.add(m);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<MDFCompatibilityProblem> getProblems() {
		return problems;
	}

	public long getOutputpos() {
		return outputpos;
	}

	public void setOutputpos(long outputpos) {
		this.outputpos = outputpos;
	}

	public long getPos() {
		return pos;
	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}
}
