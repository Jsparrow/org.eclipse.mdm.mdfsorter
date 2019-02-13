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
 * The Data Block. The data section of the DTBLOCK contains a sequence of
 * records. It contains records of all channel groups assigned to its parent
 * DGBLOCK.
 *
 * This Class can also be used for any block without own links and fields.
 *
 * @author Christian Rechner
 */
public class DTBLOCK extends MDF4GenBlock {

	/**
	 * Parse a HLBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public DTBLOCK(MDF4GenBlock parent) {
		super(parent.getPos());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setLinks(parent.getLinks());
		setId(parent.getId());
		parent.setPrec(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return new StringBuilder().append("DTBLOCK [pos=").append(getPos()).append("]").toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.mdm.mdf4sorter.MDFGenBlock#parse(byte[])
	 */
	@Override
	public void parse(byte[] content) throws IOException {
		// Nothing to be done.
	}
}
