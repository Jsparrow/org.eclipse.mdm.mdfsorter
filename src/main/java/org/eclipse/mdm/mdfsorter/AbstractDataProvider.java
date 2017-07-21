/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

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
