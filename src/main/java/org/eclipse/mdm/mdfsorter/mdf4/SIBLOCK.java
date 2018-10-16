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

/**
 * <p>
 * THE SOURCE INFORMATION BLOCK <code>SIBLOCK</code>
 * </p>
 * The SIBLOCK describes the source of an acquisition mode or of a signal. The
 * source information is also used to ensure a unique identification of a
 * channel.
 *
 * @author Christian Rechner
 */
public class SIBLOCK extends MDF4GenBlock {

	/** Data section */

	// Source type: additional classification of source:
	// 0 = OTHER source type does not fit into given categories or is unknown
	// 1 = ECU source is an ECU
	// 2 = BUS source is a bus (e.g. for bus monitoring)
	// 3 = I/O source is an I/O device (e.g. analog I/O)
	// 4 = TOOL source is a software tool (e.g. for tool generated
	// signals/events)
	// 5 = USER source is a user interaction/input
	// (e.g. for user generated events)
	// UINT8
	private byte sourceType;

	// Bus type: additional classification of used bus (should be 0 for si_type
	// ≥ 3):
	// 0 = NONE no bus
	// 1 = OTHER bus type does not fit into given categories or is unknown
	// 2 = CAN
	// 3 = LIN
	// 4 = MOST
	// 5 = FLEXRAY
	// 6 = K_LINE
	// 7 = ETHERNET
	// 8 = USB
	// Vender defined bus types can be added starting with value 128.
	// UINT8
	private byte busType;

	// Flags
	// The value contains the following bit flags (Bit 0 = LSB):
	// Bit 0: simulated source
	// Source is only a simulation (can be hardware or software simulated)
	// Cannot be set for si_type = 4 (TOOL).
	// UINT8
	private byte flags;

	/**
	 * Parse a SIBLOCK from an existing MDFGenBlock
	 *
	 * @param parent
	 *            The already existing MDF Generic Block.
	 */
	public SIBLOCK(MDF4GenBlock parent) {
		super(parent.getPos());
		setLength(parent.getLength());
		setLinkCount(parent.getLinkCount());
		setId(parent.getId());
		setLinks(parent.getLinks());
		parent.setPrec(this);
	}

	private void setSourceType(byte sourceType) {
		this.sourceType = sourceType;
	}

	private void setBusType(byte busType) {
		this.busType = busType;
	}

	private void setFlags(byte flags) {
		this.flags = flags;
	}

	public MDF4GenBlock getLnkTxName() {
		return links[0];
	}

	public MDF4GenBlock getLnkTxPath() {
		return links[1];
	}

	public MDF4GenBlock getLnkMdComment() {
		return links[2];
	}

	public byte getSourceType() {
		return sourceType;
	}

	public byte getBusType() {
		return busType;
	}

	public byte getFlags() {
		return flags;
	}

	@Override
	public String toString() {
		return "SIBLOCK [sourceType=" + sourceType + ", busType=" + busType + ", flags=" + flags + "]";
	}

	/**
	 * Reads a SIBLOCK from its content.
	 *
	 * @param content
	 *            The data section of this block
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	@Override
	public void parse(byte[] content) throws IOException {
		ByteBuffer bb = ByteBuffer.wrap(content);
		// UINT8: Source type: additional classification of source:
		setSourceType(MDF4Util.readUInt8(bb));

		// UINT8: Bus type: additional classification of used bus (should be 0
		// for si_type ≥ 3):
		setBusType(MDF4Util.readUInt8(bb));

		// UINT8: Flags
		setFlags(MDF4Util.readUInt8(bb));
	}
}
