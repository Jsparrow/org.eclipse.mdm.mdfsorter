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


package org.eclipse.mdm.mdfsorter.mdf4;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
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
import org.eclipse.mdm.mdfsorter.MDFGenBlock;
import org.eclipse.mdm.mdfsorter.MDFProblemType;
import org.eclipse.mdm.mdfsorter.MDFSorter;
import org.eclipse.mdm.mdfsorter.WriteDataCache;
import org.eclipse.mdm.mdfsorter.WriteWorker;

/**
 * Main Class for processing and writing of the output.
 *
 * @author Tobias Leemann
 *
 */
public class MDF4ProcessWriter extends MDFAbstractProcessWriter<MDF4GenBlock> {

	/**
	 * Main Constructor.
	 *
	 * @param filestructure
	 *            The result of parsing the MDF-File (MDFParser)
	 * @param args
	 *            The arguments of this programm call.
	 */
	public MDF4ProcessWriter(MDFFileContent<MDF4GenBlock> filestructure, ArgumentStruct args) {
		this.filestructure = filestructure;
		this.args = args;
		writtenblocks = new LinkedList<MDF4GenBlock>();
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
		checkProblems();

		// Open outputfile
		FileOutputStream out = new FileOutputStream(args.outputname);

		// Start writer Thread

		Thread t;
		long start; // Variables used inside try.

		try (DataBlockBuffer buf = new DataBlockBuffer()) {
			start = System.currentTimeMillis();
			t = new Thread(new WriteWorker(out, buf));
			t.start();
			myCache = new WriteDataCache(buf);
			// write out blocks
			FileChannel reader = filestructure.getInput();
			reader.position(0L);
			// reader = filestructure.getIn();

			// write header block first
			ByteBuffer headerbuf = ByteBuffer.allocate(MDF4Util.headersize);
			reader.read(headerbuf);

			// If Data has to be zipped, the file version has be set to at least
			// 4.10
			if (!args.unzip) {
				alterVersion(headerbuf);
			}

			performPut(headerbuf, headerbuf.capacity(), false);

			for (MDF4GenBlock blk : filestructure.getList()) {
				// copy block if untouched and no problem block
				if (!blk.gettouched() && blk.getProblems() == null) {
					copyBlock(blk, reader);
				} else {
					if (blk.getProblems() != null) {
						for (MDFCompatibilityProblem p : blk.getProblems()) {
							MDFSorter.log.log(Level.FINE, "Problem of Type: " + p.getType());
						}
						solveProblem(blk.getProblems());
					} else {
						// Do nothing if block is part of a bigger Problem.
						// The Block will be written if the "head block" of the
						// Problem is processed.
					}
				}

			}

			// Write updated File History Block.
			updateFileHistory();

			// Flush Cache
			myCache.flush();
		}
		// signal buffer that all data is send (try), and wait for completion of
		// the
		// write operation.
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		out.close();
		MDFSorter.log.log(Level.INFO, "Wrote " + writeptr / 1000 + " kB.");
		MDFSorter.log.log(Level.INFO, "Writing took " + (System.currentTimeMillis() - start) + " ms");

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
		MDFSorter.log.log(Level.INFO, "Links updated successfully.");
	}

	@Override
	public boolean checkProblems() {
		for (MDF4GenBlock blk : filestructure.getList()) {
			blk.analyseProblems(args);
		}
		boolean ret = false;
		for (MDF4GenBlock blk : filestructure.getList()) {
			for (int i = 0; i < blk.getLinkCount(); i++) {
				if (blk.getLink(i) != null && blk.getLink(i).getProblems() != null) {
					for (MDFCompatibilityProblem p : blk.getLink(i).getProblems()) {
						p.setParentnode(blk);
					}
					ret = true;
				}
			}
		}
		return ret;
	}

	/**
	 * Copy a block.
	 *
	 * @param blk
	 *            The block to copy.
	 * @param reader
	 *            Stream to the InputFile.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	@Override
	public void copyBlock(MDF4GenBlock blk, FileChannel reader) throws IOException {
		reader.position(blk.getPos());
		blk.setOutputpos(writeptr);

		// write file out in 64k blocks
		long length = blk.getLength();
		// Number of Bytes written
		long written = 0;

		do {
			int bytesread;
			if (written + MAX_OUTPUTBLOCKSIZE > length) {
				ByteBuffer custombuffer = ByteBuffer.allocate((int) (length - written));
				bytesread = reader.read(custombuffer);
				performPut(custombuffer, bytesread, false);
			} else {
				ByteBuffer buffer = ByteBuffer.allocate(MAX_OUTPUTBLOCKSIZE);
				bytesread = reader.read(buffer);
				performPut(buffer, bytesread, false);
			}
			written += bytesread;
		} while (written < length);
		if (length != written) {
			throw new IOException("written length not equal to blocklength: " + length + "/" + written);
		}
		// insert space if length%8!=0
		if (length % 8 != 0) {
			writeSpacer(length);
		}
		writtenblocks.addLast(blk);
	}

	/**
	 * Updates this file's version to 4.10. (needed if the output will contain
	 * any zipped data)
	 *
	 * @param headerbuf
	 *            ByteBuffer containing the old header bits of the file.
	 * @throws IOException
	 *             If an I/O-Error occurs.
	 */
	protected void alterVersion(ByteBuffer headerbuf) throws IOException {
		headerbuf.position(8);
		headerbuf.put(MDF4Util.getBytesCharsUTF8("4.10    "));
		headerbuf.position(28);
		headerbuf.put(MDF4Util.getBytesUInt16(410));
	}

	/**
	 * Main sorting function.
	 *
	 * @param l
	 *            The list with problems a block causes.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws DataFormatException
	 *             If zipped data is in an invalid format.
	 */
	public void solveProblem(List<MDFCompatibilityProblem> l) throws IOException, DataFormatException {
		if (l.size() != 1) {
			System.out.println("To many Problems.");
			// This may be supported in later versions.
			return;
		} else {
			MDFCompatibilityProblem prob = l.get(0);
			MDFProblemType probtype = prob.getType();
			MDF4GenBlock node = (MDF4GenBlock) prob.getStartnode();
			MDF4GenBlock parentnode = (MDF4GenBlock) prob.getParentnode();

			if (probtype == MDFProblemType.LINKED_DATALIST_PROBLEM || probtype == MDFProblemType.UNZIPPED_DATA_PROBLEM
					|| prob.getType() == MDFProblemType.ZIPPED_DATA_PROBLEM) {

				// What types of Elements are stored in the list? Possible ##DT,
				// ##SD, ##RD
				String blocktype;

				// First data node.
				MDF4GenBlock typechecknode = node;

				// First list node.
				MDF4GenBlock firstlistnode = node;

				// Skip HLBlock
				if (node instanceof HLBLOCK) {
					typechecknode = typechecknode.getLink(0);
					firstlistnode = firstlistnode.getLink(0);
				}

				// Skip DL block to first child
				if (typechecknode instanceof DLBLOCK) {
					if (typechecknode.getLinkCount() > 1) {
						// Data list with children
						typechecknode = typechecknode.getLink(1);
					} else {
						// Data list with no children, can just be omitted,
						// remove link
						parentnode.replaceLink(typechecknode, null);
						return;
					}

				}

				if (typechecknode instanceof DZBLOCK) {
					blocktype = "##" + ((DZBLOCK) typechecknode).getBlock_type();
				} else {
					blocktype = typechecknode.getId();
				}

				// calculate realnew blocksize in order that records are not
				// split up.
				long realmaxblksize = args.maxblocksize;
				long recordlength = -1;
				if (parentnode instanceof DGBLOCK) {
					MDF4GenBlock cgBlock = ((DGBLOCK) parentnode).getLnkCgFirst();
					int recIDsize = ((DGBLOCK) parentnode).getRecIdSize();
					if (cgBlock instanceof CGBLOCK) {
						recordlength = ((CGBLOCK) cgBlock).getDataBytes() + recIDsize
								+ ((CGBLOCK) cgBlock).getInvalBytes();
					}
				}

				if (recordlength != -1) {
					// at least one record has to be included.
					realmaxblksize = recordlength > args.maxblocksize ? recordlength
							: recordlength * (args.maxblocksize / recordlength);
				}

				// Create new SplitMerger for this section.
				MDF4BlocksSplittMerger bsm = new MDF4BlocksSplittMerger(this, blocktype, parentnode, node,
						realmaxblksize);

				// Now attach data sections
				if (probtype == MDFProblemType.LINKED_DATALIST_PROBLEM) {
					if (!(firstlistnode instanceof DLBLOCK)) {
						MDFSorter.log.severe("List header is no DL Node. Aborting.");
						throw new RuntimeException("List header is no DL Node.");
					}
					DLBLOCK dlnode = (DLBLOCK) firstlistnode;
					do {
						for (int i = 1; i < dlnode.getLinkCount(); i++) {
							bsm.splitmerge(dlnode.links[i]);
						}
					} while ((dlnode = (DLBLOCK) dlnode.links[0]) != null); // Next
					// DL-Block
				} else {
					bsm.splitmerge(node);
				}

				// set links to new datasection correctly.
				bsm.setLinks();

			} else {
				if (probtype == MDFProblemType.UNSORTED_DATA_PROBLEM) {
					// Refactor Channel Group!
					// We have more than one channel group in a single
					// DataGroup. We have to create new DataGroups for each
					// Channel Group.
					LinkedList<CGBLOCK> groups = getChannelGroupsfromDataGroup((DGBLOCK) node);
					MDFSorter.log.log(Level.INFO, "Found " + groups.size() + " Channel Groups in DG.");
					MDF4GenBlock datasection = ((DGBLOCK) node).getLnkData();
					SortDataGroup(prob, groups, datasection);

				}
				// Other Problems supported later.
			}
			return;
		}
	}

	public LinkedList<CGBLOCK> getChannelGroupsfromDataGroup(DGBLOCK startDataGroup) {
		LinkedList<CGBLOCK> ret = new LinkedList<CGBLOCK>();
		CGBLOCK next = (CGBLOCK) startDataGroup.getLnkCgFirst();
		while (next != null) {
			ret.add(next);
			next = (CGBLOCK) next.getLnkCgNext();
		}
		return ret;
	}

	/**
	 * Writes an Update to the File History block.
	 *
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public void updateFileHistory() throws IOException {

		// create FHblock (File History Block)
		FHBLOCK fhblk = new FHBLOCK();
		Date dte = new Date();
		fhblk.setTime_ns(dte.getTime() * 1000L * 1000L);

		writeBlock(fhblk, null);

		// create MetaDataBlock
		String metaData = "<FHcomment><TX>Change</TX><tool_id>MDFSorter for ODS Database</tool_id><tool_vendor>AUDI AG</tool_vendor><tool_version>"
				+ MDFSorter.VERSIONSTRING + "</tool_version></FHcomment>";
		MDF4GenBlock mdblk = new MDF4GenBlock();
		mdblk.setLength(metaData.length() + 1 + 24L);
		mdblk.setId("##MD");

		writeBlock(mdblk, MDF4Util.getBytesCharsUTF8WithTerminator(metaData));

		// Link from FH to Metadata
		fhblk.addLink(1, mdblk);

		// Attach new FileHistory
		MDF4GenBlock pre;
		if ((pre = filestructure.getRoot().getLink(1)) != null) {
			while (pre.getLink(0) != null) {
				pre = pre.getLink(0);
			}
			pre.addLink(0, fhblk);
		} else {
			MDFSorter.log.warning("Cannot attach file history. No suitable Block found.");
		}

		// After link update, all connections will be set correctly
	}

	/**
	 * Main sorting function. Sorts a datagroup consisting of more than on
	 * channel group.
	 * 
	 * @param prob
	 *            The MDFCompatibilityProblem that describes the situation.
	 * @param groups
	 *            List of ChannelGroups contained in that block.
	 * @param datasection
	 *            Data section of the DGBLOCK.
	 * @throws IOException
	 *             If an I/O-Error occurs.
	 * @throws DataFormatException
	 *             If zipped data is in an invalid format.
	 */
	public void SortDataGroup(MDFCompatibilityProblem prob, LinkedList<CGBLOCK> groups, MDF4GenBlock datasection)
			throws IOException, DataFormatException {

		DGBLOCK datagroup = (DGBLOCK) prob.getStartnode();
		// sort records.
		MDF4DataProvider prov = new MDF4DataProvider(datasection, filestructure.getInput());

		int[] recCounters = new int[groups.size()];

		byte idSize = datagroup.getRecIdSize();

		int i = 0;
		Map<Long, Integer> recNumtoArrIdx = new HashMap<Long, Integer>();
		Map<Long, Long> recNumtoSize = new HashMap<Long, Long>();

		for (CGBLOCK cgroup : groups) {
			// create an Array for the startaddresses of each record
			// TODO: Maybe improve, long number of records
			recCounters[i] = (int) cgroup.getCycleCount();
			long recID = cgroup.getRecordId();
			recNumtoArrIdx.put(recID, i++);
			if (cgroup.isVLSDChannel()) {
				recNumtoSize.put(recID, -1L);
			} else {
				recNumtoSize.put(recID, cgroup.getDataBytes() + cgroup.getInvalBytes());
			}
		}

		long[][] startaddresses = fillRecordArray(recCounters, recNumtoArrIdx, recNumtoSize, prov, idSize);

		MDF4GenBlock last = (MDF4GenBlock) prob.getParentnode();
		// write new blocks
		for (CGBLOCK cgroup : groups) {
			int arridx = recNumtoArrIdx.get(cgroup.getRecordId());
			MDFSorter.log.fine("Writing data for Block " + arridx + ".");
			long newlength;
			if (!cgroup.isVLSDChannel()) {
				// create new datagroup
				// only normal channels.
				last = copyChannelInfrastructure(last, cgroup);
				newlength = cgroup.getCycleCount() * cgroup.getDataBytes();
				long reclen = cgroup.getDataBytes() + cgroup.getInvalBytes();
				newlength = cgroup.getCycleCount() * reclen;

				// at least one record has to be included.
				long realmaxblksize = reclen > args.maxblocksize ? reclen : reclen * (args.maxblocksize / reclen);

				MDF4BlocksSplittMerger splitmerger = new MDF4BlocksSplittMerger(this, "##DT", last, newlength, prov,
						realmaxblksize);

				// write data sections.
				for (long l : startaddresses[arridx]) {
					splitmerger.splitmerge(l + idSize, reclen);
				}
				splitmerger.setLinks();

				// write corresponding SD-Blocks if needed.
				List<CNBLOCK> vlsdchanlist = cgroup.getVLSDChannels();
				if (vlsdchanlist.size() != 0) {
					for (CNBLOCK vlsdchan : vlsdchanlist) {
						MDFGenBlock signaldata = vlsdchan.getLnkData();
						if (signaldata == null) {
							MDFSorter.log.severe("VLSD-Block without attached Data found!");
							continue;
						}
						if (signaldata instanceof CGBLOCK) {
							// we need to write a sdblock...
							CGBLOCK vlsdcg = (CGBLOCK) signaldata;
							int parsingidx = recNumtoArrIdx.get(vlsdcg.getRecordId());
							long expectedlength = vlsdcg.getVLSDlength() + vlsdcg.getCycleCount() * 4L;
							MDF4BlocksSplittMerger signalsplitmerger = new MDF4BlocksSplittMerger(this, "##SD",
									vlsdchan, expectedlength, prov, args.maxblocksize);
							ByteBuffer databuf;
							// write data sections.
							for (long l : startaddresses[parsingidx]) {
								databuf = ByteBuffer.allocate(4);
								prov.read(l + idSize, databuf);
								long vllen = MDF4Util.readUInt32(databuf);
								signalsplitmerger.splitmerge(l + idSize, vllen + 4L);
							}
							signalsplitmerger.setLinks();
						}
					}
				}
			}
		}
	}

	public long[][] fillRecordArray(int[] recordCounters, Map<Long, Integer> recNumtoArrIdx,
			Map<Long, Long> recNumtoSize, AbstractDataProvider prov, int idSize)
			throws IOException, DataFormatException {

		MDFSorter.log.info("Searching Records.");
		long[][] startaddresses = new long[recordCounters.length][];

		// initilize array.
		int counter = 0;
		long totalRecords = 0; // total number of records
		for (long i : recordCounters) {
			totalRecords += i;
			startaddresses[counter++] = new long[(int) i];
		}

		int[] foundrecCounters = new int[recordCounters.length];

		long sectionoffset = 0; // our position in the data section
		long foundrecords = 0; // number of records we found

		ByteBuffer databuf;
		while (foundrecords < totalRecords) {
			// Read Group number
			databuf = prov.cachedRead(sectionoffset, idSize);
			long foundID = parseID(databuf, idSize);
			int arridx = recNumtoArrIdx.get(foundID);

			long foundsize = recNumtoSize.get(foundID);
			startaddresses[arridx][foundrecCounters[arridx]++] = sectionoffset; // remember
			if (foundsize == -1) {
				// VLSD-Channel, read length
				databuf = prov.cachedRead(sectionoffset + idSize, 4);
				long vllen = MDF4Util.readUInt32(databuf);
				sectionoffset = sectionoffset + vllen + idSize + 4L;
			} else {
				// Normal-Channel
				sectionoffset = sectionoffset + foundsize + idSize;
			}
			foundrecords++;
		}
		MDFSorter.log.fine("Found " + foundrecords + " Records.");
		return startaddresses;
	}

	public long parseID(ByteBuffer buf, int idSize) {
		switch (idSize) {
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

	public DGBLOCK copyChannelInfrastructure(MDF4GenBlock last, CGBLOCK towrite) throws IOException {
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
		towrite.setRecordId(0);
		writeBlock(towrite, null);

		return newdg;
	}

	/**
	 * Creates and writes the header of a DataBlock (any Block without links)
	 *
	 * @param size
	 *            Size of this block's Data section (length is set to size +24
	 *            Bytes for header fields)
	 * @param id
	 *            id of the Block to create, e.g. "##DT", "##SD", "##RD"...
	 * @return The MDFBlock Object, which was created.
	 */
	public MDF4GenBlock createAndWriteHeader(long size, String id) {
		MDF4GenBlock ret;
		ret = new MDF4GenBlock(0);
		ret.setLength(size + 24L);
		ret.setId(id);
		ret.setLinkCount(0);
		ret.setOutputpos(writeptr);
		performPut(ret.getHeaderBytes());
		writtenblocks.add(ret);
		return ret;
	}

	/**
	 * Writes a block to the File. This method may only be used if blk already
	 * has a set ID, length and linkcount. blk must also override the method
	 * getBodyBytes of MDFGenBlock, if any fields shall be written after the
	 * Header.
	 *
	 * @param blk
	 *            The Block to be written
	 * @param appendData
	 *            Data section of this block
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	@Override
	public void writeBlock(MDF4GenBlock blk, byte[] appendData) throws IOException {
		blk.setOutputpos(writeptr);

		performPut(blk.getHeaderBytes());
		performPut(blk.getBodyBytes());
		if (appendData != null) {
			performPut(appendData);
		}
		writeSpacer(blk.getLength());
		writtenblocks.add(blk);
	}

	/**
	 * Writes some 0-Bytes for 8 Byte alignment of blocks.
	 *
	 * @param length
	 *            The length of the block. 8-(length%8) Bytes are written.
	 */
	@Override
	public void writeSpacer(long length) {
		if (length % 8 == 0) {
			return;
		}
		int spcsize = 8 - (int) (length % 8L);
		byte[] spacer = new byte[spcsize];
		performPut(spacer);
		if (writeptr % 8L != 0) {
			System.err.println("Wrote spacer of size " + spcsize + " but writeptr is still wrong. Len:" + length
					+ " PTR:" + writeptr);
		}
	}
}
