/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter;

/**
 * Class storing information of type and other infos of a found problem.
 *
 * @author Tobias Leemann
 *
 */
public class MDFCompatibilityProblem {

	/**
	 * The type of the problem.
	 */
	private final MDFProblemType type;

	/**
	 * Node which this problem is attached to.
	 */
	private MDFGenBlock startnode;

	/**
	 * Parent node of <code>startnode</code>. This node's link to startnode has
	 * to be changed, after the problem has been processed.
	 */
	private MDFGenBlock parentnode;

	/**
	 * Create a new Problem.
	 *
	 * @param t
	 *            Type of the Problem.
	 * @param n
	 *            Node which this problem is attached to.
	 */
	public MDFCompatibilityProblem(MDFProblemType t, MDFGenBlock n) {
		startnode = n;
		type = t;
	}

	public MDFGenBlock getStartnode() {
		return startnode;
	}

	public void setStartnode(MDFGenBlock startnode) {
		this.startnode = startnode;
	}

	public MDFGenBlock getParentnode() {
		return parentnode;
	}

	public void setParentnode(MDFGenBlock parentnode) {
		this.parentnode = parentnode;
	}

	public MDFProblemType getType() {
		return type;
	}
}
