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

public class ReadDataCache {
	private ByteBuffer cache;
	private long cachestart = -1;
	private long cacheend;
	private final int CACHESIZE = 65536; // 64k
	private AbstractDataProvider prov;
	private long maxreadlen;

	public ReadDataCache(AbstractDataProvider prov) {
		cache = ByteBuffer.allocate(CACHESIZE);
		this.prov = prov;
		maxreadlen = prov.getLength();
	}

	public ByteBuffer read(long startoffset, int length) throws IOException, DataFormatException {
		if (length < CACHESIZE) {
			if (startoffset >= cachestart && startoffset + length < cacheend) {
				// cache hit!
				cache.position((int) (startoffset - cachestart));
				return cache;
			} else {
				// cache miss.
				cache.rewind();
				cachestart = startoffset;
				cacheend = startoffset + CACHESIZE;
				if (cacheend > maxreadlen) {
					// maximum reached.
					cacheend = maxreadlen;
					cache = ByteBuffer.allocate((int) (maxreadlen - startoffset));
					if (maxreadlen - startoffset < length) {
						throw new RuntimeException(new StringBuilder().append("Length ").append(length).append(" Bytes are not available from ").append(startoffset).toString());
					}
				} else if (cache.capacity() < CACHESIZE) {
					cache = ByteBuffer.allocate(CACHESIZE);
				}
				prov.read(cachestart, cache);
				return cache;
			}
		} else {
			// cache is useless anyway...
			ByteBuffer data = ByteBuffer.allocate(length);
			prov.read(startoffset, data);
			return data;
		}
	}
}
