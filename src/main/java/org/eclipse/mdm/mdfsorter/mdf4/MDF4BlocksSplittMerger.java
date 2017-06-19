/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter.mdf4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;

/**
 * Main Processor for Block Merging. Data Blocks found in the DataList can just
 * be passed on to this class, which refactors them.
 *
 * @author Tobias Leemann
 *
 */
/**
 * @author EU2IYD9
 *
 */
public class MDF4BlocksSplittMerger {

	/**
	 * Parent Object.
	 */
	private MDF4ProcessWriter ps;

	/**
	 * Pointer to block the new DataSection has to be linked is. Set in the
	 * constructor, and used in setLinks();
	 */
	private MDF4GenBlock curr;

	/**
	 * Pointer to block that is currently written. If curr is an DZBlock, write
	 * out data is buffered here, and zipped and written out, once all data for
	 * this block is read.
	 */
	private final MDF4GenBlock parentnode;

	/**
	 * Root block of this section, e.g. HL, DL or DT if only one exists. This
	 * block needs to be linked in the file.
	 */
	private MDF4GenBlock structuralroot;

	/**
	 * ListBlock that all created Datablocks have to be put in. Can be null, set
	 * during CreateStructure.
	 */
	private DLBLOCK parentlist;

	FileChannel reader;
	String blocktype;

	/**
	 * Unzip paramet (Taken form the programm arguments)
	 */
	private final boolean unzip;

	/**
	 * Maximum blocksize, size which the ouputblocks will have.
	 */
	private final long maxblocksize;

	/**
	 * Total length of the new Data section. Needs to be calculated in advance
	 * before processing blocks.
	 */
	private final long totdatalength;

	/**
	 * Amount of bytes of data written behind the output block
	 */
	private long datawritten = 0;

	/**
	 * End Address of the block that is currently written in.
	 */
	private long thisblockend = 0;

	/**
	 * Number of blocks created with this object so far.
	 */
	private long blockcounter = 0;

	/**
	 * Number of data blocks that will be needed.
	 */
	private long estimatedblockcounter = 0;

	/**
	 * Maximum number of Datablocks, to be attached to a list block. If the
	 * number is larger, a new list will be created.
	 */
	public static final int MAX_LIST_COUNT = 2048;

	/**
	 * Pointer to block that is currently read from. (if a blockwise read is
	 * done) Must be null, if the block is unknown. (non-blockwise read)
	 *
	 */
	private MDF4GenBlock towrite;

	/**
	 * An offset in the original Block, only used, if the block that is read
	 * from is known (towrite != null)
	 */
	private int BlockReadPtr = 0;

	/**
	 * An offset in the global data section, only used, if the block that is
	 * read from is not known (towrite == null)
	 */
	private long GlobalReadPtr = 0;

	/**
	 * Data Provider for reading blocks. This Object manages unzipping of
	 * blocks.
	 */
	private MDF4DataProvider prov;

	private byte[] uncompressedoutData;
	private int uncompressedWritePtr;

	/**
	 * Create a new Datasection with the given options, to write data without
	 * concern about the underlying block structure. This class creates HL, DL
	 * and the needed Data or zipped Blocks, when needed. Datasection length is
	 * automatically set to the length of the old section
	 *
	 * @param ps
	 *            The parent ProcessWriter
	 * @param blocktype
	 *            Type of the DataBlocks that will be written
	 * @param parentnode
	 *            Node that is parent of this datasection.
	 * @param oldsection
	 *            The data section that is split up. This is used to create a
	 *            provider and set section length.
	 * @param maxblocksize
	 *            The maximum size of a block in the output. Should be a
	 *            multiple of the size of one record.
	 */
	public MDF4BlocksSplittMerger(MDF4ProcessWriter ps, String blocktype, MDF4GenBlock parentnode,
			MDF4GenBlock oldsection, long maxblocksize) {
		this.ps = ps;
		this.blocktype = blocktype;
		unzip = ps.getArgs().unzip;
		this.maxblocksize = maxblocksize;
		this.parentnode = parentnode;
		reader = ps.getFilestructure().getInput();

		prov = new MDF4DataProvider(oldsection, reader);
		totdatalength = prov.getLength();
		try {
			createStructure();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Same constructor as above, except that totaldatalength and prov can be
	 * passed manually. Therfore oldsection is not needed.
	 *
	 * @param ps
	 *            The parent ProcessWriter
	 * @param blocktype
	 *            Type of the DataBlocks that will be written
	 * @param parentnode
	 *            Node that is parent of this datasection.
	 * @param totdatalength
	 *            The length of data that will be written.
	 * @param prov
	 *            The DataProvider to read from.
	 */
	public MDF4BlocksSplittMerger(MDF4ProcessWriter ps, String blocktype, MDF4GenBlock parentnode, long totdatalength,
			MDF4DataProvider prov, long maxblocksize) {
		this.ps = ps;
		this.blocktype = blocktype;
		unzip = ps.getArgs().unzip;
		this.maxblocksize = maxblocksize;
		this.parentnode = parentnode;
		reader = ps.getFilestructure().getInput();

		this.totdatalength = totdatalength;
		this.prov = prov;

		try {
			createStructure();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public MDF4GenBlock getStructuralRoot() {
		return structuralroot;
	}

	/**
	 * Main method of this class. This method writes out the data section of the
	 * Block <code>datablock</code> to the file, or buffers it for later output.
	 * The data section is merged with data of other blocks or splitted if
	 * needed in this step of processing.
	 *
	 * @param datablock
	 *            The datablock to be processed.
	 * @throws IOException
	 *             If any output errors occur.
	 * @throws DataFormatException
	 *             If zipped data is given an an invalid format.
	 */
	public void splitmerge(MDF4GenBlock datablock) throws IOException, DataFormatException {
		prov = new MDF4DataProvider(datablock, reader);
		BlockReadPtr = 0;
		long leftbytes;
		if (datablock.getId().equals("##DZ")) {
			leftbytes = ((DZBLOCK) datablock).getOrg_data_length();
		} else {
			leftbytes = datablock.getLength() - 24L;
		}
		towrite = datablock;
		appendDataFromPos(leftbytes);

	}

	/**
	 * Append the datasection beginning at pos until length to the output.
	 *
	 * @param startaddress
	 *            Startposition of the section
	 * @param length
	 *            Length of the section.
	 * @throws IOException
	 *             If an input error occurs.
	 * @throws DataFormatException
	 *             If zipped data is in an invalid format.
	 */
	public void splitmerge(long startaddress, long length) throws IOException, DataFormatException {
		// we are doing a non-blockwise read
		towrite = null;
		GlobalReadPtr = startaddress;
		appendDataFromPos(length);
	}

	/**
	 * Reads leftbytes from the current position and appends them to the output
	 * section
	 *
	 * @param leftbytes
	 *            The number of bytes remaining.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws DataFormatException
	 *             If zipped data is in an invalid format.
	 */
	public void appendDataFromPos(long leftbytes) throws IOException, DataFormatException {
		// check if space in curr-Block is available, and fill with first data,
		// or attach all data if it fits
		if (curr != null) {
			if (datawritten < thisblockend) { // Space available
				long bytestowrite = leftbytes < thisblockend - datawritten ? leftbytes : thisblockend - datawritten;
				abstractcopy(bytestowrite);
				datawritten += bytestowrite;

				checkfinalized();
				if (bytestowrite == leftbytes) { // we are done!
					return;
				} else {
					leftbytes -= bytestowrite;
				}
			}
		}

		while (datawritten + leftbytes > thisblockend) { // block doesn't fit,
			// we need at least
			// one new block
			// last block: adapt length, else use maxlength
			long newblocklength = totdatalength < (blockcounter + 1) * maxblocksize
					? totdatalength - blockcounter * maxblocksize : maxblocksize;
			curr = abstractcreate(newblocklength, blocktype); // This method
			// creates a
			// zipblock if
			// needed
			if (structuralroot == null) {
				structuralroot = curr;
			}

			thisblockend += newblocklength;

			long bytestowrite = leftbytes < newblocklength ? leftbytes : newblocklength;
			abstractcopy(bytestowrite);

			datawritten += bytestowrite;
			leftbytes -= bytestowrite;
			checkfinalized();
		}
	}

	/**
	 * Check if this block is entirely filled with data and writes a spacer for
	 * 8-byte alignment if needed. If the current block is a zip block (in which
	 * case Data is not buffered but stored internally) it is written in this
	 * step.
	 *
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public void checkfinalized() throws IOException {
		if (datawritten == thisblockend) {
			if (curr.getId().equals("##DZ")) {
				// Compress the bytes
				DZBLOCK dzblk = (DZBLOCK) curr;
				byte[] output = new byte[(int) dzblk.getOrg_data_length()];
				Deflater compresser = new Deflater();
				compresser.setInput(uncompressedoutData);
				compresser.finish();
				int compressedDataLength = compresser.deflate(output);
				compresser.end();

				// write DZBlock
				dzblk.setLength(24L + 24L + compressedDataLength);
				dzblk.setData_length(compressedDataLength);

				ps.performPut(dzblk.getHeaderBytes());
				ps.performPut(dzblk.getBodyBytes());
				ps.performPut(ByteBuffer.wrap(output), compressedDataLength, false);
				ps.writeSpacer(compressedDataLength);
			} else {
				ps.writeSpacer(curr.getLength());
			}

		}
	}

	/**
	 * This method has the same effect as if data was read from the stream.
	 * Except that if curr is a Zip-Block, the data is unzipped first.
	 *
	 * @param length
	 *            The number of bytes to read.
	 * @return A ByteBuffer where length bytes can be read from.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws DataFormatException
	 *             If zipped data is in an invalid format.
	 */
	public ByteBuffer abstractread(int length) throws IOException, DataFormatException {
		if (towrite != null) {
			// blockwise

			// System.out.println(length);

			ByteBuffer datasection = ByteBuffer.allocate(length);
			prov.read(BlockReadPtr, datasection, towrite);
			BlockReadPtr += datasection.limit();
			return datasection;
		} else {
			// not blockwise
			return prov.cachedRead(GlobalReadPtr, length);
		}

	}

	/**
	 * This method has the same effect as if data was stored to the buffer, but
	 * if data has to be zipped, it is stored and will be zipped later.
	 *
	 * @param datasection
	 *            The Data to be put in the buffer
	 */
	public void abstractput(byte[] datasection) {
		if (curr.getId().equals("##DZ")) {
			System.arraycopy(datasection, 0, uncompressedoutData, uncompressedWritePtr, datasection.length);
			uncompressedWritePtr += datasection.length;
		} else {
			ps.performPut(datasection);
		}
	}

	public void abstractput(ByteBuffer buf, int length) {
		if (curr.getId().equals("##DZ")) {
			buf.get(uncompressedoutData, uncompressedWritePtr, length);
			uncompressedWritePtr += length;
		} else {
			if (towrite != null) {
				// blockwise mode, no read cache used.
				ps.performPut(buf, length, false);
			} else {
				// not blockwise, read cache is used.
				ps.performPut(buf, length, true);
			}

		}
	}

	/**
	 * This Method creats a block. If a normal block needs to be created, it is
	 * immediately written to the disk. Zip blocks are written after they have
	 * been zipped.
	 *
	 * @param newblocklength
	 *            Datalength of the new block
	 * @param blocktype
	 *            Type of the compressed block
	 * @return new newly created block.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @see checkfinalized
	 */
	public MDF4GenBlock abstractcreate(long newblocklength, String blocktype) throws IOException {
		uncompressedWritePtr = 0;
		MDF4GenBlock ret;
		if (estimatedblockcounter != 1 && blockcounter % MAX_LIST_COUNT == 0) {
			// new list block needs to be created.
			int childblocks = (int) (estimatedblockcounter - blockcounter < MAX_LIST_COUNT
					? estimatedblockcounter - blockcounter : MAX_LIST_COUNT);
			DLBLOCK newparentlist = createDList(childblocks);
			if (parentlist == null) {
				if (structuralroot == null) {
					structuralroot = newparentlist;
				} else {
					// Structuralroot is the HLBLOCK
					structuralroot.addLink(0, newparentlist);
				}
			} else {
				parentlist.addLink(0, newparentlist);
			}

			parentlist = newparentlist;
		}

		if (unzip) {
			ret = ps.createAndWriteHeader(newblocklength, blocktype);
		} else {
			DZBLOCK dzblock = new DZBLOCK();
			dzblock.setId("##DZ");
			dzblock.setBlock_type(blocktype.substring(2));
			dzblock.setLinkCount(0);
			dzblock.setOrg_data_length(newblocklength);
			uncompressedoutData = new byte[(int) newblocklength];
			dzblock.setOutputpos(ps.getWriteptr());
			dzblock.setZip_type((byte) 0);
			ret = dzblock;
		}

		ps.getWrittenblocks().add(ret);

		if (parentlist != null) {
			parentlist.addLink((int) (blockcounter % MAX_LIST_COUNT) + 1, ret);
		}
		blockcounter++;
		return ret;
	}

	/**
	 * Creates a HLBLOCK if needed.
	 * 
	 * @throws IOException
	 *             If an I/O-Error occurs.
	 */
	public void createStructure() throws IOException {
		// Do we need a list block?
		if (totdatalength > maxblocksize) {
			// Yes we do,
			// and so we may also need a HLBLOCK, if the -zip flag is set
			HLBLOCK zlistheader = null;
			if (unzip == false) {
				// TODO: Other flags-
				zlistheader = new HLBLOCK();
				zlistheader.setFlags((byte) 1); // set equallength flag, //zip
				// algorithm implicitly set to
				// deflate
				ps.writeBlock(zlistheader, null);
			}

			// how many links to datablocks do we need?
			if (totdatalength % maxblocksize == 0) {
				estimatedblockcounter = (int) (totdatalength / maxblocksize);
			} else {
				estimatedblockcounter = (int) (totdatalength / maxblocksize) + 1;
			}

			// if we created a HLblock the root the subtree will be this one
			if (zlistheader != null) {
				zlistheader.addLink(0, parentlist);
				structuralroot = zlistheader;
			}
		} else {
			estimatedblockcounter = 1;
		}
	}

	/**
	 * Reads length bytes with <code>reader</code> and writes them out. This
	 * happens in smaller blocks.
	 *
	 * @param length
	 *            The number of bytes to copy
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws DataFormatException
	 *             If zipped data is in an invalid format.
	 */
	public void abstractcopy(long length) throws IOException, DataFormatException {
		long written = 0L;
		do {
			int bytesread = 0;
			if (written + MDF4ProcessWriter.MAX_OUTPUTBLOCKSIZE > length) {
				bytesread = (int) (length - written);
				ByteBuffer custombuffer = abstractread(bytesread);
				abstractput(custombuffer, bytesread);
			} else {
				ByteBuffer buffer = abstractread(MDF4ProcessWriter.MAX_OUTPUTBLOCKSIZE);
				bytesread = MDF4ProcessWriter.MAX_OUTPUTBLOCKSIZE;
				abstractput(buffer, bytesread);
			}
			written += bytesread;
		} while (written < length);
		if (length != written) {
			throw new IOException("written length not equal to blocklength: " + length + "/" + written);
		}
	}

	public void setLinks() {
		if (parentnode instanceof DGBLOCK) {
			parentnode.setLink(2, structuralroot);
		} else if (parentnode instanceof CNBLOCK) {
			parentnode.setLink(5, structuralroot);
		} else if (parentnode.getId().equals("##SR")) {
			parentnode.setLink(1, structuralroot);
		}
	}

	/**
	 * ONLY FOR TESTING! Used in MDFUnsorter, replaces this splitmerger's
	 * provider.
	 *
	 * @param prov
	 *            The new data provider.
	 */
	public void setProv(MDF4DataProvider prov) {
		this.prov = prov;
	}

	public DLBLOCK createDList(int childblocks) throws IOException {
		DLBLOCK ret = new DLBLOCK();
		ret.setLinkCount(childblocks + 1);
		// 24 Head, 8 next Datalink + 16 flags and equallength
		ret.setLength(24L + 8L * childblocks + 24L);
		// set equal lengh flag and other fields
		ret.setFlags((byte) 1);
		ret.setCount(childblocks);
		ret.setEqualLength(maxblocksize);
		ps.writeBlock(ret, null);
		return ret;
	}
}
