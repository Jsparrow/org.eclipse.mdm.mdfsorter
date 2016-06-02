/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map.Entry;

/**
 * A thread to take byte data from a DataBlockBuffer, for writing to the
 * FileOutputStream fs until the buffer signals that no more data is going to
 * come. This is used to increase IO Throughput.
 *
 * @author Tobias Leemann
 *
 */
public class WriteWorker implements Runnable {
	FileOutputStream fs;
	DataBlockBuffer buf;

	/**
	 * Constructor. Set parameters.
	 *
	 * @param fs
	 *            FileOutput Stream to the output file.
	 * @param buf
	 *            The buffer from which the data is taken.
	 */
	public WriteWorker(FileOutputStream fs, DataBlockBuffer buf) {
		this.fs = fs;
		this.buf = buf;
	}

	/*
	 * Takes bytes of Data from the buffer until a null-Pointer signals, that no more data
	 * will be expected.
	 *
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		Entry<byte[], Integer> towrite;
		while ((towrite = buf.getData()) != null) {
			try {
				if (towrite.getValue() == -1) {
					fs.write(towrite.getKey());
				} else {
					fs.write(towrite.getKey(), 0, towrite.getValue());
				}
			} catch (IOException e) {
				throw new RuntimeException("IOException");
			}
		}
	}
}
