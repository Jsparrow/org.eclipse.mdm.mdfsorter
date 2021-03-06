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

/**
 * <p>
 * THE Event BLOCK <code>EVBLOCK</code>
 * </p>
 * The TXBLOCK is very similar to the MDBLOCK but only contains a plain string
 * encoded in UTF-8. The text length results from the block size.
 *
 * @author Tobias Leemann
 */
public class EVBLOCK extends MDF4GenBlock {

	/** Data section */

	/**
	 * Parse a TXBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public EVBLOCK(MDF4GenBlock parent) {
		super(parent.getPos());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	@Override
	public String toString() {
		return "EVBLOCK";
	}

	/**
	 * Reads a TXBLOCK from its content.
	 *
	 * @param content
	 *            The data section of this block
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	@Override
	public void parse(byte[] content) throws IOException {

	}

}
