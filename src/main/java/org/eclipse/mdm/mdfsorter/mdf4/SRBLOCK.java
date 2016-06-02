/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter.mdf4;

import java.io.IOException;

/**
 * <p>
 * THE Sample Reduction block <code>SRBLOCK</code>
 * </p>
 * The TXBLOCK is very similar to the MDBLOCK but only contains a plain string encoded in UTF-8. The text length results
 * from the block size.
 *
 * @author Tobias Leemann
 */
public class SRBLOCK extends MDF4GenBlock {

	/** Data section */


	/**
	 * Parse a SRBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public SRBLOCK(MDF4GenBlock parent) {
		super(parent.getPos());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}


	@Override
	public String toString() {
		return "SRBLOCK";
	}

	/**
	 * Reads a SRBLOCK from its content.
	 *
	 * @param content The data section of this block
	 * @throws IOException If an I/O error occurs.
	 */
	@Override
	public void parse(byte[] content) throws IOException {

	}

}
