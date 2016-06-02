/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter.mdf3;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.DataFormatException;

import org.eclipse.mdm.mdfsorter.AbstractDataProvider;
import org.eclipse.mdm.mdfsorter.ArgumentStruct;
import org.eclipse.mdm.mdfsorter.DataBlockBuffer;
import org.eclipse.mdm.mdfsorter.MDFAbstractProcessWriter;
import org.eclipse.mdm.mdfsorter.MDFCompatibilityProblem;
import org.eclipse.mdm.mdfsorter.MDFFileContent;
import org.eclipse.mdm.mdfsorter.MDFProblemType;
import org.eclipse.mdm.mdfsorter.MDFSorter;
import org.eclipse.mdm.mdfsorter.WriteDataCache;
import org.eclipse.mdm.mdfsorter.WriteWorker;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4Util;

/**
 * Main Class for processing and writing of the output.
 *
 * @author Tobias Leemann
 *
 */
public class MDF3ProcessWriter extends MDFAbstractProcessWriter<MDF3GenBlock> {

	/**
	 * Main Constructor.
	 *
	 * @param filestructure
	 *            The result of parsing the MDF-File (MDFParser)
	 * @param args
	 *            The arguments of this programm call.
	 */
	public MDF3ProcessWriter(MDFFileContent<MDF3GenBlock> filestructure,
			ArgumentStruct args) {
		this.filestructure = filestructure;
		this.args = args;
		writtenblocks = new LinkedList<MDF3GenBlock>();
	}

	public int numberOfDatagroups = 0;

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
		checkProblems();

		// A data groups will be created for each channel, count channels!
		for (MDF3GenBlock blk : filestructure.getList()) {
			if (blk instanceof CGBLOCK) {
				numberOfDatagroups++;
			}
		}
		// Check if zip flag is not set
		if (!args.unzip) {
			throw new IllegalArgumentException(
					"MDF3.x Files mustn't be zipped!");
		}

		// Open outputfile
		FileOutputStream out = new FileOutputStream(args.outputname);

		long start; Thread t; //Start time will be stored here later.

		try(DataBlockBuffer buf = new DataBlockBuffer()){
			//automatically stop writer thread if exeptions occur (Writer Thread is stopped vie the DataBlock Buffer.

			myCache = new WriteDataCache(buf);

			// Start writer Thread
			start = System.currentTimeMillis();
			t = new Thread(new WriteWorker(out, buf));
			t.start();

			// write out blocks
			FileChannel reader = filestructure.getInput();
			reader.position(0L);
			// reader = filestructure.getIn();

			// write header block first
			ByteBuffer headerbuf = ByteBuffer.allocate(MDF4Util.headersize);
			reader.read(headerbuf);

			// If Data has to be zipped, the file version has be set to at least

			performPut(headerbuf, headerbuf.capacity(), false);

			for (MDF3GenBlock blk : filestructure.getList()) {
				// copy block if untouched and no problem block
				if (!blk.gettouched() && blk.getProblems() == null) {
					if (blk instanceof HDBLOCK) {
						((HDBLOCK) blk).setNumberOfDataGroups(numberOfDatagroups);
						writeBlock(blk, null);
					} else {
						copyBlock(blk, reader);
					}

				} else {
					if (blk.getProblems() != null) {
						for (MDFCompatibilityProblem p : blk.getProblems()) {
							MDFSorter.log.log(Level.FINE,
									"Problem of Type: " + p.getType());
						}
						solveProblem(blk.getProblems());
					} else {
						// Do nothing if block is part of a bigger Problem.
						// The Block will be written if the "head block" of the
						// Problem is processed.
					}
				}
			}

			// Flush Cache
			myCache.flush();
		}
		//Wait for completion of write operation.
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//Close output stream.
		out.close();

		MDFSorter.log.log(Level.INFO, "Wrote " + writeptr / 1000 + " kB.");
		MDFSorter.log.log(Level.INFO,
				"Writing took " + (System.currentTimeMillis() - start) + " ms");

		// Update links with RandomAccessFile
		RandomAccessFile r = new RandomAccessFile(args.outputname, "rw");
		for (MDF3GenBlock blk : writtenblocks) {
			blk.updateLinks(r);
		}
		r.close();
		MDFSorter.log.log(Level.INFO, "Links updated successfully.");
	}

	/**
	 * Main sorting function.
	 *
	 * @param l
	 *            list of problems, one specific node has.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws DataFormatException
	 *             If zipped Data is in an invalid format
	 */
	public void solveProblem(List<MDFCompatibilityProblem> l)
			throws IOException, DataFormatException {
		if (l.size() != 1) {
			System.out.println("To many Problems.");
			// This may be supported in later versions.
			return;
		} else {
			MDFCompatibilityProblem prob = l.get(0);
			MDFProblemType probtype = prob.getType();
			MDF3GenBlock node = (MDF3GenBlock) prob.getStartnode();
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
				SortDataGroup(prob, groups, datasection);

			} else {
				// Other Problems supported later.
			}
			return;
		}
	}

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

	public void SortDataGroup(MDFCompatibilityProblem prob,
			LinkedList<CGBLOCK> groups, MDF3GenBlock datasection)
					throws IOException, DataFormatException {

		DGBLOCK datagroup = (DGBLOCK) prob.getStartnode();
		// sort records.
		MDF3DataProvider prov = new MDF3DataProvider(datasection,
				filestructure.getInput());

		int idSize = 1;

		// ids at the end of the record.
		boolean redundantids = false;
		if (datagroup.getNumOfRecId() == 2) {
			redundantids = true;
		}

		int i = 0;
		Map<Integer, Integer> recNumtoArrIdx = new HashMap<Integer, Integer>();
		Map<Integer, Integer> recNumtoSize = new HashMap<Integer, Integer>();

		long[] recCounters = new long[groups.size()];

		for (CGBLOCK cgroup : groups) {

			//Loop through records, and initialize variables
			recCounters[i] = cgroup.getCycleCount();
			int recID = cgroup.getRecordId();
			recNumtoArrIdx.put(recID, i++);
			recNumtoSize.put(recID, cgroup.getDataBytes());
		}

		long[][] startaddresses = fillRecordArray(recCounters, recNumtoArrIdx, recNumtoSize, prov, redundantids);

		MDF3GenBlock last = (MDF3GenBlock) prob.getParentnode();

		// write new blocks
		for (CGBLOCK cgroup : groups) {
			int arridx = recNumtoArrIdx.get(cgroup.getRecordId());
			MDFSorter.log.fine("Writing data for Block " + arridx + ".");
			long newlength;
			// create new datagroup
			last = copyChannelInfrastructure(last, cgroup);
			long reclen = cgroup.getDataBytes();
			newlength = cgroup.getCycleCount() * cgroup.getDataBytes();
			MDF3BlocksSplittMerger splitmerger = new MDF3BlocksSplittMerger(
					this, last, newlength, prov);

			// write data sections.
			for (long l : startaddresses[arridx]) {
				splitmerger.splitmerge(l + idSize, reclen);
			}
			splitmerger.setLinks();
		}
	}


	public long[][] fillRecordArray(long[] recordCounters, Map<Integer, Integer> recNumtoArrIdx, Map<Integer,
			Integer> recNumtoSize, AbstractDataProvider prov, boolean redundantids) throws IOException, DataFormatException{

		MDFSorter.log.info("Searching Records.");

		long[][] startaddresses = new long[recordCounters.length][];
		int idSize = 1;

		//initilize array.
		int counter =0;
		long totalRecords = 0; //total number of records
		for(long i : recordCounters){
			totalRecords+=i;
			startaddresses[counter++] = new long[(int) i];
		}

		int[] foundrecCounters = new int[recordCounters.length];

		long sectionoffset = 0; //our position in the data section
		long foundrecords = 0; //number of records we found

		ByteBuffer databuf;
		while (foundrecords < totalRecords) {
			// Read Group number
			databuf = prov.cachedRead(sectionoffset, idSize);
			int foundID = MDF3Util.readUInt8(databuf);
			Integer foundsize = recNumtoSize.get(foundID);
			if(foundsize == null){ //Check if a size was found.
				throw new RuntimeException("No Size known for record ID "+ foundID+ ".");
			}
			if (redundantids) {
				// do a sanity check with the second id
				databuf = prov.cachedRead(sectionoffset, idSize + foundsize);
				int endID = MDF3Util.readUInt8(databuf);
				if (endID != foundID) {
					MDFSorter.log.warning("Found ID " + foundID
							+ " at start of records, but ID " + endID
							+ " at its end.");
				}
			}
			Integer arridx = recNumtoArrIdx.get(foundID);
			if(arridx == null){ //Check if an entry was found.
				throw new RuntimeException("Record ID "+ foundID+ " is not known.");
			}
			startaddresses[arridx][foundrecCounters[arridx]++] = sectionoffset; // remember
			// start
			// of records
			// Normal-Channel
			sectionoffset = sectionoffset + foundsize + idSize;

			// Skip id after record
			if (redundantids) {
				sectionoffset += 1;
			}
			foundrecords++;
		}
		MDFSorter.log.fine("Found " + foundrecords + " Records.");
		return startaddresses;
	}

	public DGBLOCK copyChannelInfrastructure(MDF3GenBlock last, CGBLOCK towrite)
			throws IOException {
		// Create new Data Group with default values, and write to file.
		DGBLOCK newdg = new DGBLOCK(filestructure.isBigEndian());
		newdg.setChannelGroups(1);
		writeBlock(newdg, null);
		// set link to next DGblock.
		last.setLink(0, newdg);
		// set link to child CGblock.
		newdg.setLink(1, towrite);

		// Modify and write old channel group.
		// No other channel.
		towrite.setLink(0, null);
		towrite.setRecordId(0);
		writeBlock(towrite, null);
		return newdg;
	}

	/**
	 * Creates and writes the header of a DataBlock (any Block without links)
	 *
	 * @param size
	 *            Size of this block's Data section (length is set to size +24
	 *            Bytes for header fields.
	 * @return The MDFBlock Object, which was created.
	 */
	public DTBLOCK create(long size) {
		DTBLOCK ret;
		ret = new DTBLOCK(filestructure.isBigEndian());
		ret.setLength(size);
		ret.setLinkCount(0);
		ret.setOutputpos(writeptr);
		writtenblocks.add(ret);
		return ret;
	}

	@Override
	public void writeSpacer(long length) {
		// Do nothing in MDF3
	}

}
