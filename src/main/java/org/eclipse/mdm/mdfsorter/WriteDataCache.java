/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter;

import java.nio.ByteBuffer;
import java.util.AbstractMap;

import org.eclipse.mdm.mdfsorter.mdf4.MDF4ProcessWriter;

/**
 * Cache for outgoing Data. This class stores data to be written, until a usuful
 * amout is collected. If this size is reached, the write out will be performed.
 *
 * Cache size should be at least as large as the process
 * writers<code>MAX_OUTPUTBLOCKSIZE</code> value, because otherwise large blocks
 * will be splitted into smaller pieces, slowing down the write operation.
 */
public class WriteDataCache {
	private static final int WRITE_CACHE_SIZE = MDF4ProcessWriter.MAX_OUTPUTBLOCKSIZE / 2;

	/**
	 * The buffer the data is send to, once the cache is full
	 */
	DataBlockBuffer buf;

	byte[] cache;
	int cachewriteposition = 0;

	public WriteDataCache(DataBlockBuffer buf) {
		this.buf = buf;
	}

	/**
	 * Puts length bytes from the given ByteBuffer into the cache.
	 *
	 * @param data
	 *            The ByteBuffer.
	 * @param length
	 *            The number of Bytes from the Buffers current position to be
	 *            written.
	 * @param reused
	 *            True, if the current buffer contains other data, that will be
	 *            overwritten or its position can be changed until output.
	 *            Therefore its content needs to be copied. False if the
	 *            Buffer.array() method can be used, for faster access to the
	 *            data bytes. Therefore the size of the ByteBuffer and length
	 *            must be equal.
	 */
	public void put(ByteBuffer data, int length, boolean reused) {
		if (reused) {
			if (length > WRITE_CACHE_SIZE) {
				byte[] dt = new byte[length];
				data.get(dt, 0, length);
				put(dt);
			} else {
				// copy to cache immediately
				// use the cache
				// first write part that still fits into cache.
				if (cache == null) {
					cache = new byte[WRITE_CACHE_SIZE];
				}
				int spaceremaining = WRITE_CACHE_SIZE - cachewriteposition;

				int bytesfirstsection = spaceremaining < length ? spaceremaining : length;
				// maybe data needs to be split up
				data.get(cache, cachewriteposition, bytesfirstsection);
				cachewriteposition += bytesfirstsection;
				CheckAndWriteout();

				// write second part if needed
				if (bytesfirstsection < length) {
					int bytessecond = length - bytesfirstsection;
					data.get(cache, cachewriteposition, bytessecond);
					cachewriteposition += bytessecond;
				}
			}
		} else {
			put(data.array());
		}
	}

	/**
	 * Puts the array data in the WriteCache.
	 *
	 * @param data
	 *            The data, which should be put in the Cache.
	 */
	public void put(byte[] data) {
		// Cache is useless;
		int length = data.length;
		if (length > WRITE_CACHE_SIZE) {
			flush(); // flush data from the cache
			buf.putData(new AbstractMap.SimpleEntry<byte[], Integer>(data, -1));
		} else {
			// use the cache
			// first write part that still fits into cache.
			if (cache == null) {
				cache = new byte[WRITE_CACHE_SIZE];
			}
			int spaceremaining = WRITE_CACHE_SIZE - cachewriteposition;

			int bytesfirstsection = spaceremaining < length ? spaceremaining : length;
			// maybe data needs to be split up

			System.arraycopy(data, 0, cache, cachewriteposition, bytesfirstsection);
			cachewriteposition += bytesfirstsection;

			CheckAndWriteout();

			// write second part if needed
			if (bytesfirstsection < length) {
				int bytessecond = length - bytesfirstsection;
				System.arraycopy(data, bytesfirstsection, cache, cachewriteposition, bytessecond);
				cachewriteposition += bytessecond;
			}
		}
	}

	/**
	 * Force data in the Cache to be written out.
	 */
	public void flush() {
		// Cache has been flushed?
		if (cache != null) {
			buf.putData(new AbstractMap.SimpleEntry<byte[], Integer>(cache, cachewriteposition));
			cachewriteposition = 0;
			cache = null;
		}
	}

	public void CheckAndWriteout() {
		if (cachewriteposition == WRITE_CACHE_SIZE) {
			// write all data out.
			buf.putData(new AbstractMap.SimpleEntry<byte[], Integer>(cache, -1));
			cache = new byte[WRITE_CACHE_SIZE];
			cachewriteposition = 0;
		}
	}
}
