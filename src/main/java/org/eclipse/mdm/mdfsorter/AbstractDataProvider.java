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
import java.util.zip.DataFormatException;

/**
 * Abstract Parent Class of MDF3DataProvider and MDF4DataProvider.
 * 
 * @author Tobias Leemann
 */
public interface AbstractDataProvider {
	abstract void read(long globaloffset, ByteBuffer data) throws IOException, DataFormatException;

	ByteBuffer cachedRead(long globaloffset, int length) throws IOException, DataFormatException;

	/**
	 * Get the length of a data section.
	 * 
	 * @return The length of the data section.
	 */
	abstract long getLength();
}
