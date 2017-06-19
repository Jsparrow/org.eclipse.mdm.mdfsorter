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

/**
 * <p>
 * THE Attachment block<code>ATBLOCK</code>
 * </p>
 * The ATBLOCK specifies attached data, eiher by referencing an external file or
 * by embedding the data in the MDF-File.
 *
 * @author Christian Rechner, Tobias Leemann
 */
public class ATBLOCK extends MDF4GenBlock {

	/** Data section */

	// Plain text string
	// UTF-8 encoded, zero terminated, new line indicated by CR and LF.
	// CHAR
	private String txData;

	/**
	 * Parse a TXBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public ATBLOCK(MDF4GenBlock parent) {
		super(parent.getPos());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	// Getters and Setters

	// Link to next ATBLOCK
	public MDF4GenBlock getLnkAtNext() {
		return links[0];
	}

	// Link to TextBlock with path of the referenced file
	public MDF4GenBlock getLnkTxFilename() {
		return links[1];
	}

	// Link to the MIME-Type (as text)
	public MDF4GenBlock getLnkTxMIMEType() {
		return links[2];
	}

	// Link to MDBlock with comment
	public MDF4GenBlock getLnkMdComment() {
		return links[3];
	}

	public String getTxData() {
		return txData;
	}

	private void setTxData(String txData) {
		this.txData = txData;
	}

	@Override
	public String toString() {
		return "TXBLOCK [txData=" + txData + "]";
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
		// Read text String
		setTxData(MDF4Util.readCharsUTF8(ByteBuffer.wrap(content), content.length));
		// TODO: Bytes after zero termination?
	}

}
