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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.zip.DataFormatException;

import org.eclipse.mdm.mdfsorter.mdf3.MDF3GenBlock;
import org.eclipse.mdm.mdfsorter.mdf3.MDF3ProcessWriter;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4GenBlock;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4ProcessWriter;

/**
 * Main class of this MDF4Sorter, with all publicly accessible interfaces:
 * <code>main()</code> for command line use, <code>sortMDF()</code> for java
 * use.
 *
 * @author Tobias Leemann
 *
 */
abstract public class MDFSorter {

	/**
	 * Version of this tool as String. (Used in the file Header)
	 */
	public static final String VERSIONSTRING = "1.0.3";

	/**
	 * The logger for this application
	 */
	public static Logger log;

	/**
	 * Command-Line Interface of this tool. Please note the following syntax:
	 *
	 * @param args
	 *            <br>
	 *            args[0] = Path to the inputfile; <br>
	 *            args[1] = Path were the output shall be written; <br>
	 *            args[2-n] = Flags. the following flags are valid: <br>
	 *            -zip: Zip all Data found. <br>
	 *            -unzip: Unzip all Data found.<br>
	 *            -maxblocksize=Value: Maximum size of a DataBlock. e.g. "200M",
	 *            "3K", "1G"<br>
	 *
	 *            example call: file1.mf4 file2.mf4 -unzip -maxblocksize=800M
	 * @throws IOException
	 *             If any in/output errors occur.
	 */
	public static void main(String[] args) throws IOException {
		if (args == null || args.length == 0) {
			System.out.println("At least one argument has to be provided.");
			printUsage();
		} else {
			try {
				switch (args[0]) {
				case "help":
					printUsage();
					return;
				case "check":
					ArgumentStruct structchk = ArgumentStruct.parseArgsCheck(args);
					checkForProblems(structchk.inputname, structchk.maxblocksize, structchk.unzip);
					return;
				case "process":
					setUpLogging();
					ArgumentStruct struct = ArgumentStruct.parseArgs(args);
					handleCall(struct);
					break;
				default:
					System.out.println("Unknown command.");
					printUsage();
				}
			} catch (MDFSorterArgException e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
				printUsage();
			}
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
	public static void sortMDF(String inputfile, String outputfile, long maxblocksize, boolean unzip) {
		setUpLogging();
		ArgumentStruct struct = new ArgumentStruct();
		struct.inputname = inputfile;
		struct.outputname = outputfile;
		struct.maxblocksize = maxblocksize;
		struct.unzip = unzip;

		// Larger blocks cannot be zipped (see Specification of MDF4.1)
		if (!unzip && maxblocksize > 4L * 1024L * 1024L) {
			log.log(Level.WARNING, "Setting maxblocksize to 4MB. Larger blocks are not allowed for zipped data.");
			struct.maxblocksize = 4L * 1024L * 1024L;
		}

		handleCall(struct);
	}

	/**
	 * PUBLIC: Method to check, it processing of a file is needed, or if the
	 * file can just be passed to the ODS Server.
	 *
	 * @param inputfile
	 *            Path to the input file.
	 * @param maxblocksize
	 *            The maximum size a block may have.
	 * @param unzip
	 *            True, if zipped data needs to be unzipped. False, if the file
	 *            can legally contain zipped data blocks.
	 * @return True, if problems were found in this file. False if no problems
	 *         were found.
	 * @throws IOException
	 *             (File not found)
	 */
	public static boolean checkForProblems(String inputfile, long maxblocksize, boolean unzip) throws IOException {
		setUpLogging();
		// wrap arguments.
		ArgumentStruct args = new ArgumentStruct();
		args.unzip = unzip;
		args.maxblocksize = maxblocksize;
		args.inputname = inputfile;

		// 2. Check for Problems.
		return checkForProblems(args);
	}

	/**
	 * PUBLIC: Method to check, it processing of a file is needed, or if the
	 * file can just be passed to the ODS Server.
	 *
	 * @param inputfile
	 *            Path to the input file.
	 * @param maxblocksize
	 *            True, if zipped data needs to be unzipped. False, if the file
	 *            can legally contain zipped data blocks.
	 * @return True, if problems were found in this file. False if no problems
	 *         werde found.
	 * @throws IOException
	 *             (File not found)
	 */
	public static boolean checkForProblems(String inputfile, long maxblocksize) throws IOException {
		return checkForProblems(inputfile, maxblocksize, true);
	}

	/**
	 * Internally called Method that really performs the "check" operation.
	 * 
	 * @param struct
	 *            The Arguments for this call
	 * @return True, if problems were found, false if not.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	static boolean checkForProblems(ArgumentStruct struct) throws IOException {
		setUpLogging();
		FileInputStream bufstream;
		bufstream = new FileInputStream(struct.inputname);
		log.log(Level.INFO, "File opened.");
		// 1. Parse file and get Content-Struct
		MDFFileContent<? extends MDFGenBlock> con = MDFParser.serializeFile(bufstream.getChannel());

		// 2. Check for Problems.
		boolean ret = false;
		if (!con.isMDF3()) {
			@SuppressWarnings("unchecked")
			MDF4ProcessWriter pw = new MDF4ProcessWriter((MDFFileContent<MDF4GenBlock>) con, struct);
			ret = pw.checkProblems();
		} else {
			@SuppressWarnings("unchecked")
			MDF3ProcessWriter pw = new MDF3ProcessWriter((MDFFileContent<MDF3GenBlock>) con, struct);
			ret = pw.checkProblems();
		}

		if (ret) {
			log.info("Problems were found. Processing file recommended.");
		} else {
			log.info("No problems were found. This file needn't be processed.");
		}
		bufstream.close();
		return ret;
	}

	/**
	 * Internal method called from the Java and the command line interface. Does
	 * the Main work for the "Process" command.
	 *
	 * @param struct
	 *            The Arguments of this program call.
	 */
	@SuppressWarnings("unchecked")
	static void handleCall(ArgumentStruct struct) {
		if (struct.verbose) {
			log.setLevel(Level.FINE);
			log.getHandlers()[0].setLevel(Level.FINE);
		}
		FileInputStream bufstream;
		try {
			bufstream = new FileInputStream(struct.inputname);
			log.log(Level.INFO, "File opened.");
			// 1. Parse file and get Content-Struct
			MDFFileContent<? extends MDFGenBlock> con = MDFParser.serializeFile(bufstream.getChannel());

			// 2. Init processing and write out
			@SuppressWarnings("rawtypes")
			MDFAbstractProcessWriter processorwriter;

			if (con.isMDF3()) {
				processorwriter = new MDF3ProcessWriter((MDFFileContent<MDF3GenBlock>) con, struct);
			} else {
				processorwriter = new MDF4ProcessWriter((MDFFileContent<MDF4GenBlock>) con, struct);
			}
			processorwriter.processAndWriteOut();
			bufstream.close();

		} catch (IOException e) {
			System.err.println(e);
			log.severe("Aborted. IOException encountered.");
		} catch (DataFormatException e) {
			e.printStackTrace();
			log.severe("Aborted. DataFormatException encountered.");
		}
	}

	/**
	 * Configures the logger for this application.
	 */
	static void setUpLogging() {
		// Set up logger

		log = Logger.getLogger("org.eclipse.mdm.mdfsorter");
		log.setUseParentHandlers(false);
		for (Handler h : log.getHandlers()) {
			if (!(h instanceof MemorizingHandler)) {
				log.removeHandler(h);
			}
		}

		// Force immediate output
		Handler h = new StreamHandler(System.out, new SimpleFormatter()) {
			@Override
			public void publish(LogRecord record) {
				super.publish(record);
				flush();
			}
		};
		log.addHandler(h);
		h.setLevel(Level.INFO);

		// Do not touch, it is important for testing that all messages are
		// logged.
		log.setLevel(Level.ALL);
	}

	/**
	 * Prints a meaningful usage message.
	 */
	private static void printUsage() {
		System.out.println("Usage: MDF4Sorter <command> [<parameters>]");
		System.out.println();
		System.out.println("Valid commands are:");
		System.out.println("\"process\":");
		System.out.println(
				"\tProcess an MDF4 file for usage with an ASAM ODS Server.\n\tThis call requires the following parameters:\n\t <inputfile> <outputfile> [<flags>]");
		System.out.println("\tInputfile: The MDF4-File to process");
		System.out.println("\tOutputfile: The MDF-File where the output will be written.");
		System.out.println("\tFlags: Other parameters. Ordering of flags is not important.");
		System.out.println("\t\t-zip: Zip all Data found. ");
		System.out.println("\t\t-unzip: Unzip all Data found.");
		System.out
				.println("\t\t-maxblocksize=<Value>: Maximum size of a DataBlock. \n\t\te.g. \"200M\", \"3K\", \"1G\"");
		System.out.println("\tExample: process infile.mf4 outfile.mf4 -maxblocksize=20m -zip");
		System.out.println("\"check\":");
		System.out.println(
				"\tCheck if processing an MDF4 file for usage with an ASAM ODS Server\n\tis necessary. This call requires the following parameters:\n\t <inputfile> [<maxblocksize>] [<zipflag>]");
		System.out.println("\tInputfile: The MDF4-File to process");
		System.out
				.println("\t\t-maxblocksize=<Value>: Maximum size of a DataBlock. \n\t\te.g. \"200M\", \"3K\", \"1G\"");
		System.out.println(
				"\tzipflag: \"-zip\" or \"-unzip\", zip if all data will be zipped,\n\t\tunzipped if all data block will be unzipped.");
		System.out.println("\tExample: check infile.mf4 4M -zip");
		System.out.println("\"help\":");
		System.out.println("\tPrint this info.");
	}
}
