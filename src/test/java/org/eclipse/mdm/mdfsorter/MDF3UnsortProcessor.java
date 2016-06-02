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

import org.eclipse.mdm.mdfsorter.mdf3.CGBLOCK;
import org.eclipse.mdm.mdfsorter.mdf3.DGBLOCK;
import org.eclipse.mdm.mdfsorter.mdf3.MDF3BlocksSplittMerger;
import org.eclipse.mdm.mdfsorter.mdf3.MDF3DataProvider;
import org.eclipse.mdm.mdfsorter.mdf3.MDF3GenBlock;
import org.eclipse.mdm.mdfsorter.mdf3.MDF3ProcessWriter;
import org.eclipse.mdm.mdfsorter.mdf3.MDF3Util;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4Util;

public class MDF3UnsortProcessor extends MDF3ProcessWriter {

	public MDF3UnsortProcessor(MDFFileContent<MDF3GenBlock> filestructure,
			ArgumentStruct args) {
		super(filestructure, args);
	}

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

		performPut(headerbuf.array());

		for (MDF3GenBlock blk : filestructure.getList()) {
			// copy block if untouched
			if (!blk.gettouched() && blk.getProblems() == null) {
				if (blk.getId().equals("HD")) {
					reader.position(blk.getPos());
					blk.setOutputpos(writeptr);

					// write file out in 64k blocks
					long length = blk.getLength();
					ByteBuffer custombuffer = ByteBuffer
							.allocate((int) length);
					int bytesread = reader.read(custombuffer);
					// alter head entry.
					custombuffer.position(16);
					custombuffer.put(MDF3Util.getBytesUInt16(1,
							filestructure.isBigEndian()));
					performPut(custombuffer, bytesread, false);
					writtenblocks.addLast(blk);
				} else {
					copyBlock(blk, reader);
				}
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
		for (MDF3GenBlock blk : writtenblocks) {
			// set position to start of Block link section
			blk.updateLinks(r);
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
			MDF3GenBlock node = (MDF3GenBlock) prob.getStartnode();
			MDF3GenBlock parentnode = (MDF3GenBlock) prob.getParentnode();
			if (probtype == MDFProblemType.UNSORTED_DATA_PROBLEM) {
				// Refactor Channel Group!
				// We have more than one channel group in a single
				// DataGroup. We have to create new DataGroups for each
				// Channel Group.
				LinkedList<CGBLOCK> groups = getChannelGroupsfromDataGroup(
						(DGBLOCK) node);
				MDFSorter.log.log(Level.INFO,
						"Found " + groups.size() + " Channel Groups in DG.");
				MDF3GenBlock datasection = ((DGBLOCK) node).getLnkData();
				UnSortDataGroup(prob, dgroups, datasection);

			}
			// Other Problems supported later.
			return;
		}
	}

	public void UnSortDataGroup(MDFCompatibilityProblem prob,
			LinkedList<DGBLOCK> dgroups, MDF3GenBlock datasection)
					throws IOException, DataFormatException {

		boolean redundantIDs = true;
		// create master DG
		int numgroups = dgroups.size();
		DGBLOCK master = new DGBLOCK(filestructure.isBigEndian());
		master.setNumOfRecId(redundantIDs ? 2 : 1);
		master.setChannelGroups(numgroups);
		writeBlock(master, null);

		MDF3GenBlock root = (MDF3GenBlock) prob.getParentnode();
		root.setLink(0, master);

		MDF3DataProvider[] providers = new MDF3DataProvider[numgroups];
		long records[] = new long[numgroups];
		long written[] = new long[numgroups];
		long sizes[] = new long[numgroups];

		CGBLOCK last = null;
		long newsize = 0;
		long totreccount = 0;
		int count = 0;
		for (DGBLOCK dgblk : dgroups) {

			// link in channel groups
			if (count == 0) {
				master.setLink(1, dgblk.getLnkCgFirst());
			} else {
				last.setLink(0, dgblk.getLnkCgFirst());
			}
			providers[count++] = new MDF3DataProvider(dgblk.getLnkData(),
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
		newsize += totreccount * (redundantIDs ? 2 : 1);
		MDF3BlocksSplittMerger bsm = new MDF3BlocksSplittMerger(this, master,
				newsize, providers[0]);

		MDF3DataProvider idprovider = new MDF3DataProvider(new byte[] { 0 });

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

			if (redundantIDs) {
				bsm.setProv(idprovider);
				bsm.splitmerge(0, 1);
			}
			if (writtenrecords % 10000 == 0) {
				System.out.println("Wrote " + writtenrecords + " of "
						+ totreccount + " Records.");
			}
		}
		bsm.setLinks();
	}

	public LinkedList<DGBLOCK> analyse() {
		LinkedList<DGBLOCK> ret = new LinkedList<DGBLOCK>();
		MDF3GenBlock hdroot = filestructure.getRoot();
		// Set Problem to first datagroup
		MDFCompatibilityProblem prob = new MDFCompatibilityProblem(
				MDFProblemType.UNSORTED_DATA_PROBLEM, hdroot.getLink(0));
		prob.setParentnode(hdroot);
		hdroot.getLink(0).addProblem(prob);
		for (MDF3GenBlock blk : filestructure.getList()) {
			switch (blk.getId()) {
			case "DG":
				if (!blk.equals(prob.getStartnode())) {
					blk.touch();
				}
				ret.add((DGBLOCK) blk);
				break;
			case "DT":
			case "CG":
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
		} while (passed <= random && ret != records.length - 1);
		return ret;
	}

}
