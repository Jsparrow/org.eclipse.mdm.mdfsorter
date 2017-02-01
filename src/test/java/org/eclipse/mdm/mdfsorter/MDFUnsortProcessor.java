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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.zip.DataFormatException;

import org.eclipse.mdm.mdfsorter.mdf4.CGBLOCK;
import org.eclipse.mdm.mdfsorter.mdf4.DGBLOCK;
import org.eclipse.mdm.mdfsorter.mdf4.HDBLOCK;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4BlocksSplittMerger;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4DataProvider;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4GenBlock;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4ProcessWriter;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4Util;

/**
 * Main Class for processing and writing of the output.
 *
 * @author Tobias Leemann
 *
 */
public class MDFUnsortProcessor extends MDF4ProcessWriter {

	public MDFUnsortProcessor(MDFFileContent<MDF4GenBlock> filestructure,
			ArgumentStruct args) {
		super(filestructure, args);
	}

	/**
	 * Main Function of this class.
	 *
	 * @throws IOException
	 *             If any I/O-Problems are encountered.
	 * @throws DataFormatException
	 *             If the data of any DZ-Blocks is not in a readable format.
	 */
	@Override
	public void processAndWriteOut() throws IOException, DataFormatException {

		// 1. Analyse situation
		LinkedList<DGBLOCK> dgroups = analyse();

		// Open outputfile
		FileOutputStream out = new FileOutputStream(args.outputname);

		// Start writer Thread
		DataBlockBuffer buf = new DataBlockBuffer();
		myCache = new WriteDataCache(buf);

		long start = System.currentTimeMillis();
		Thread t = new Thread(new WriteWorker(out, buf));
		t.start();

		// write out blocks
		FileChannel reader = filestructure.getInput();
		reader.position(0L);
		// reader = filestructure.getIn();

		// write header block first
		ByteBuffer headerbuf = ByteBuffer.allocate(MDF4Util.headersize);
		reader.read(headerbuf);

		// If Data has to be zipped, the file version has be set to at least
		// 4.10
		// TODO
		if (!args.unzip) {
			alterVersion(headerbuf);
		}

		performPut(headerbuf.array());

		for (MDF4GenBlock blk : filestructure.getList()) {
			// copy block if untouched
			if (!blk.gettouched() && blk.getProblems() == null) {
				copyBlock(blk, reader);
			} else {
				if (blk.getProblems() != null) {
					for (MDFCompatibilityProblem p : blk.getProblems()) {
						MDFSorter.log.log(Level.FINE,
								"Problem of Type: " + p.getType());
					}
					solveProblem(blk.getProblems(), buf, dgroups);
				} else {
					// Do nothing if block is part of a bigger Problem.
					// The Block will be written if the "head block" of the
					// Problem is processed.
				}
			}

		}

		// Write updated File History Block.
		updateFileHistory();

		// signal buffer that all data is send, and wait for completion of the
		// write operation.
		myCache.flush();
		buf.putData(null);
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		out.close();
		MDFSorter.log.log(Level.INFO, "Wrote " + writeptr / 1000 + " kB.");
		MDFSorter.log.log(Level.INFO,
				"Writing took " + (System.currentTimeMillis() - start) + " ms");

		// Update links with RandomAccessFile
		RandomAccessFile r = new RandomAccessFile(args.outputname, "rw");
		for (MDF4GenBlock blk : writtenblocks) {
			// set position to start of Block link section
			r.seek(blk.getOutputpos() + 24L);
			MDF4GenBlock linkedblock;
			for (int i = 0; i < blk.getLinkCount(); i++) {
				if ((linkedblock = blk.getLink(i)) != null) {
					r.write(MDF4Util.getBytesLink(linkedblock.getOutputpos()));
				} else {
					r.write(MDF4Util.getBytesLink(0));
				}

			}
		}
		r.close();
		MDFSorter.log.log(Level.FINE, "Links updated successfully.");
	}

	/**
	 * Main sorting function.
	 *
	 * @param l
	 * @param buf
	 * @throws IOException
	 * @throws DataFormatException
	 */
	public void solveProblem(List<MDFCompatibilityProblem> l,
			DataBlockBuffer buf, LinkedList<DGBLOCK> dgroups)
					throws IOException, DataFormatException {
		if (l.size() != 1) {
			System.out.println("To many Problems.");
			// This may be supported in later versions.
			return;
		} else {
			MDFCompatibilityProblem prob = l.get(0);
			MDFProblemType probtype = prob.getType();
			MDF4GenBlock node = (MDF4GenBlock) prob.getStartnode();
			if (probtype == MDFProblemType.UNSORTED_DATA_PROBLEM) {
				// Refactor Channel Group!
				// We have more than one channel group in a single
				// DataGroup. We have to create new DataGroups for each
				// Channel Group.
				LinkedList<CGBLOCK> groups = getChannelGroupsfromDataGroup(
						(DGBLOCK) node);
				MDFSorter.log.log(Level.INFO,
						"Found " + groups.size() + " Channel Groups in DG.");
				MDF4GenBlock datasection = ((DGBLOCK) node).getLnkData();
				UnSortDataGroup(prob, dgroups, datasection, buf);

			}
			// Other Problems supported later.
			return;
		}
	}

	@Override
	public LinkedList<CGBLOCK> getChannelGroupsfromDataGroup(
			DGBLOCK startDataGroup) {
		LinkedList<CGBLOCK> ret = new LinkedList<CGBLOCK>();
		CGBLOCK next = (CGBLOCK) startDataGroup.getLnkCgFirst();
		while (next != null) {
			ret.add(next);
			next = (CGBLOCK) next.getLnkCgNext();
		}
		return ret;
	}

	public void UnSortDataGroup(MDFCompatibilityProblem prob,
			LinkedList<DGBLOCK> dgroups, MDF4GenBlock datasection,
			DataBlockBuffer buf) throws IOException, DataFormatException {
		// create master DG
		DGBLOCK master = new DGBLOCK();
		master.setRecIdSize((byte) 1);
		writeBlock(master, null);

		MDF4GenBlock root = (MDF4GenBlock) prob.getParentnode();
		root.setLink(0, master);

		int numgroups = dgroups.size();
		MDF4DataProvider[] providers = new MDF4DataProvider[numgroups];
		long records[] = new long[numgroups];
		long written[] = new long[numgroups];
		long sizes[] = new long[numgroups];

		CGBLOCK last = null;
		long newsize = 0;
		long totreccount = 0;
		int count = 0;
		for (DGBLOCK dgblk : dgroups) {
			if (count == 0) {
				master.setLink(1, dgblk.getLnkCgFirst());
			} else {
				last.setLink(0, dgblk.getLnkCgFirst());
			}
			providers[count++] = new MDF4DataProvider(dgblk.getLnkData(),
					filestructure.getInput());
			last = (CGBLOCK) dgblk.getLnkCgFirst();
			last.setRecordId(count);
			newsize += last.getCycleCount() * last.getDataBytes();
			totreccount += last.getCycleCount();
			records[count - 1] = last.getCycleCount();
			sizes[count - 1] = last.getDataBytes();
			writeBlock(last, null);
		}

		// unsort records.
		newsize += totreccount;
		MDF4BlocksSplittMerger bsm = new MDF4BlocksSplittMerger(this, "##DT",
				master, newsize, providers[0], args.maxblocksize);

		MDF4DataProvider idprovider = new MDF4DataProvider(new byte[] { 0 });

		long writtenrecords = 0;
		while (writtenrecords < totreccount) {

			int rectowrite = getRandomRecordnum(records, totreccount);
			while (written[rectowrite] == records[rectowrite]) {
				rectowrite = (rectowrite + 1) % numgroups;
			}
			// write id
			idprovider.setDataArray(new byte[] { (byte) (rectowrite + 1) });
			bsm.setProv(idprovider);
			bsm.splitmerge(0, 1);

			// write record
			bsm.setProv(providers[rectowrite]);
			bsm.splitmerge(written[rectowrite] * sizes[rectowrite],
					sizes[rectowrite]);
			written[rectowrite]++;
			writtenrecords++;
			if (rectowrite == 10) {
				System.out.println("Wrote " + writtenrecords + " of "
						+ totreccount + " Records.");
			}
		}
		bsm.setLinks();
	}

	public long parseID(ByteBuffer buf) {
		switch (buf.limit()) {
		case 1:
			return MDF4Util.readUInt8(buf);
		case 2:
			return MDF4Util.readUInt16(buf);
		case 4:
			return MDF4Util.readUInt32(buf);
		case 8:
			return MDF4Util.readUInt64(buf);
		default:
			System.err.println("Invalid bit size");
			throw new RuntimeException("Invalid bit size.");
		}
	}

	public LinkedList<DGBLOCK> analyse() {
		LinkedList<DGBLOCK> ret = new LinkedList<DGBLOCK>();
		HDBLOCK hdroot = (HDBLOCK) filestructure.getRoot();
		// Set Problem to first datagroup
		MDFCompatibilityProblem prob = new MDFCompatibilityProblem(
				MDFProblemType.UNSORTED_DATA_PROBLEM, hdroot.getLnkDgFirst());
		prob.setParentnode(hdroot);
		hdroot.getLnkDgFirst().addProblem(prob);
		for (MDF4GenBlock blk : filestructure.getList()) {
			switch (blk.getId()) {
			case "##DG":
				if (!blk.equals(prob.getStartnode())) {
					blk.touch();
				}
				ret.add((DGBLOCK) blk);
				break;
			case "##HL":
			case "##DT":
			case "##DL":
			case "##CG":
				blk.touch();
			}
		}
		return ret;
	}

	public int getRandomRecordnum(long[] records, long total) {
		int ret = -1;
		int random = new Random().nextInt((int) total);
		int passed = 0;
		do {
			ret++;
			passed += records[ret];
		} while (passed <= random);
		return ret;
	}

	public DGBLOCK copyChannelInfrastructure(MDF4GenBlock last, CGBLOCK towrite,
			DataBlockBuffer buf) throws IOException {
		// Create new Data Group with default values, and write to file.
		DGBLOCK newdg = new DGBLOCK();
		writeBlock(newdg, null);
		// set link to next DGblock.
		last.setLink(0, newdg);
		// set link to child CGblock.
		newdg.setLink(1, towrite);

		// Modify and write old channel group.
		// No other channel.
		towrite.setLink(0, null);
		// No VLSD
		towrite.setIsVlSD(false);
		writeBlock(towrite, null);

		return newdg;
	}
}
