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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.zip.DataFormatException;

import org.eclipse.mdm.mdfsorter.mdf3.MDF3GenBlock;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4GenBlock;

/**
 * Main class of this MDF4Sorter, with all publicly accessible interfaces:
 * <code>main()</code> for command line use, <code>sortMDF()</code> for java
 * use.
 *
 * @author Tobias Leemann
 *
 */
abstract public class MDFUnsorter {

	/**
	 * Version of this tool as String. (Used in the file Header)
	 */
	public static final String VERSIONSTRING = "0.1.0";

	/**
	 * Command-Line Interface of this tool. Please note the following syntax:
	 *
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Provide two arguments!");
			return;
		} else {
			unsortMDF(args[0], args[1], Integer.MAX_VALUE, true);
		}

	}

	/**
	 * PUBLIC: This method processes an MDF4-File. It resolves all Linked Data
	 * lists and Zipped Data blocks (DZBLOCK) were possible.
	 *
	 * @param inputfile
	 *            The Path to the file to be processed.
	 * @param outputfile
	 *            The Path were the output should be written.
	 * @param maxblocksize
	 *            maximum size of a data block. (Must not be larger than 4MB if
	 *            unzip==false)
	 * @param unzip
	 *            True if all data should be unzipped in the output file. False
	 *            if all data should be Zipped (Stored in DZBlocks) in the
	 *            output file.
	 */
	@SuppressWarnings("unchecked")
	public static void unsortMDF(String inputfile, String outputfile, long maxblocksize, boolean unzip) {
		var struct = new ArgumentStruct();
		struct.inputname = inputfile;
		struct.outputname = outputfile;
		struct.maxblocksize = maxblocksize;
		struct.unzip = unzip;
		MDFSorter.setUpLogging();
		FileInputStream bufstream;
		try {
			bufstream = new FileInputStream(inputfile);
			MDFSorter.log.log(Level.INFO, "File opened.");
			// 1. Parse file and get Content-Struct
			MDFFileContent<? extends MDFGenBlock> con = MDFParser.serializeFile(bufstream.getChannel());

			MDFAbstractProcessWriter<?> processorwriter;

			if (con.isMDF3()) {
				processorwriter = new MDF3UnsortProcessor((MDFFileContent<MDF3GenBlock>) con, struct);
				processorwriter.processAndWriteOut();
			} else {
				processorwriter = new MDFUnsortProcessor((MDFFileContent<MDF4GenBlock>) con, struct);
				processorwriter.processAndWriteOut();
			}

			bufstream.close();

		} catch (IOException e) {
			System.err.println(e);
			MDFSorter.log.severe("Aborted. IOException encountered.");
		} catch (DataFormatException e) {
			e.printStackTrace();
			MDFSorter.log.severe("Aborted. DataFormatException encountered.");
		}

	}
}
