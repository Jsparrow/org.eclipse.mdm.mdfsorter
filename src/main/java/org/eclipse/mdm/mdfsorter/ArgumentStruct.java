/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter;

import java.util.logging.Level;

/**
 * Structure to store the program arguments.
 *
 * @author Tobias Leemann
 *
 */
public class ArgumentStruct {
	public String inputname;
	public String outputname;
	public boolean unzip = true;
	public long maxblocksize = 2_147_483_648L; // 2GB

	public boolean overrideOldSize;
	public boolean verbose;

	private boolean zipflagset;

	/**
	 * Parse Arguments given via the Command-Line-Inferface (only used for the
	 * "process" call.)
	 *
	 * @param argv
	 *            The Arguments of the call.
	 * @return An ArgumentStruct containing all values for this call.
	 * @throws IllegalArgumentException
	 *             If the arguments are not valid.
	 */
	public static ArgumentStruct parseArgs(String[] argv)
			throws IllegalArgumentException {
		if (argv.length < 3) {
			throw new IllegalArgumentException(
					"At least two arguments must be provided.");
		} else {
			ArgumentStruct args = new ArgumentStruct();
			args.inputname = argv[1];
			args.outputname = argv[2];
			for (int i = 3; i < argv.length; i++) {
				String[] splitted = argv[i].split("=");
				switch (splitted[0]) {
				case "-unzip":
					if (args.zipflagset) {
						throw new MDFSorterArgException(
								"Ambigous zip flags.");
					}
					args.unzip = true;
					args.zipflagset = true;
					break;
				case "-overridesize":
					args.overrideOldSize = true;
					break;
				case "-verbose":
					args.verbose = true;
					break;
				case "-zip":
					if (args.zipflagset) {
						throw new MDFSorterArgException(
								"Ambigous zip flags.");
					}
					args.unzip = false;
					args.zipflagset = true;
					if (args.zipflagset) {
						break;
					}
				case "-maxblocksize":
					if (splitted.length < 2) {
						throw new MDFSorterArgException(
								"Argument must be provided after \"-maxblocksize=\" flag.");
					} else {
						args.maxblocksize = parseLong(splitted[1]);
					}
					break;
				default:
					throw new MDFSorterArgException(
							"Unknown Argument " + splitted[0]);
				}
			}

			if (!args.unzip && args.maxblocksize > 4 * 1024L * 1024L) {
				MDFSorter.log.log(Level.WARNING,
						"Setting maxblocksize to 4MB. Larger blocks are not allowed for zipped data.");
				args.maxblocksize = 4 * 1024L * 1024L;
			}

			return args;
		}
	}

	public static ArgumentStruct parseArgsCheck(String[] argv)
			throws IllegalArgumentException {
		ArgumentStruct args = new ArgumentStruct();
		if (argv.length < 1) {
			throw new MDFSorterArgException(
					"At least one arguments must be provided.");
		}
		args.inputname = argv[1];

		if (argv.length <= 2) {
			args.maxblocksize = parseLong(argv[1]);
			if (argv.length == 3) {
				if (argv[2].equals("-unzip")) {
					args.unzip = true;
				} else if (argv[2].equals("-zip")) {
					args.unzip = false;
				} else {
					throw new MDFSorterArgException("Unknown zipflag");
				}
			} else if (argv.length > 3) {
				throw new MDFSorterArgException("Too many arguments.");
			}
		}

		return args;
	}

	/**
	 * Parse a long from a String with decimal prefix, e.g. "100M", "2G", ...
	 *
	 * @param arg
	 *            The String to parse.
	 * @return The value of the String as long.
	 */
	static long parseLong(String arg) {

		char c = arg.charAt(arg.length() - 1);
		// Numerical value only
		if (c > 47 && c < 58) {
			return Long.parseLong(arg);
		} else if (c == 'M' || c == 'm' || c == 'K' || c == 'k' || c == 'G'
				|| c == 'g') {
			String numval = arg.substring(0, arg.length() - 1);
			long l = Long.parseLong(numval);
			switch (c) {
			case 'G':
			case 'g':
				l *= 1024L;
			case 'M':
			case 'm':
				l *= 1024L;
			case 'k':
			case 'K':
				l *= 1024L;
				break;
			default:
				l = 2_000_000_000L;
			}
			return l;
		} else {
			throw new MDFSorterArgException("Illegal numerical value");
		}
	}
}
