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

import java.util.LinkedList;
import java.util.Map.Entry;

/**
 * This is a buffer to store <code>byte[]</code>-Data, which is put in by the
 * read an process thread, and taken out by a new Instance of Write-Worker, to
 * be written to the disk. Aim of this class is to store data to be passed on to
 * the write thread.
 *
 * @author Tobias Leemann
 * @see WriteWorker
 *
 */
public class DataBlockBuffer implements AutoCloseable {

	/**
	 * This list contains the pointers to the data, and an integer with the
	 * length of the data to be written, or -1, if all data should be written.
	 * Write always starts at index 0 of the data array.
	 */
	private volatile LinkedList<Entry<byte[], Integer>> data;

	/**
	 * Maximum number of entries which can be buffered in the data list.
	 */
	public static final int maxlength = 10;

	public DataBlockBuffer() {
		data = new LinkedList<Entry<byte[], Integer>>();
	}

	/**
	 * Fetches the next data section from the buffer.
	 *
	 * @return The next data section which can be written.
	 */
	public synchronized Entry<byte[], Integer> getData() {
		while (data.isEmpty()) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException("Interrupted!");
			}
		}
		Entry<byte[], Integer> ret = null;
		if (!data.isEmpty()) {
			ret = data.removeFirst();
		}
		// notify processes waiting for write.
		notifyAll();
		return ret;
	}

	/**
	 * Put a section of data (<code>byte[]</code>) into the buffer.
	 *
	 * @param dataarray
	 *            The data to put.
	 */
	public synchronized void putData(Entry<byte[], Integer> dataarray) {
		while (data.size() >= maxlength) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException("Interrupted!");
			}
		}
		data.addLast(dataarray);
		if (data.size() == 1) {
			// notify processes waiting for read.
			notifyAll();
		}
	}

	/*
	 * Puts null into the buffer, which causes the write tread using this buffer
	 * to finish.
	 * 
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() {
		putData(null);
	}

}
