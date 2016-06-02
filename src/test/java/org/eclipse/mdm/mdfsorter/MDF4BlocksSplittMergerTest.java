/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter;

import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.mdm.mdfsorter.mdf4.DGBLOCK;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4BlocksSplittMerger;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4GenBlock;
import org.eclipse.mdm.mdfsorter.mdf4.MDF4ProcessWriter;
import org.junit.BeforeClass;
import org.junit.Test;

public class MDF4BlocksSplittMergerTest {

	@BeforeClass
	public static void BeforeClass() throws IOException {
		MDFSorter.log = Logger.getLogger("vwg.audi.mdfsorter");
		MDFSorter.log.setUseParentHandlers(false);
	}

	@Test
	public void testLength0() {
		//test if a splitmerger with length 0 works, and links are correctly set to null.
		DGBLOCK blk = new DGBLOCK();
		blk.setId("##DG");


		MDFFileContent<MDF4GenBlock> con = new MDFFileContent<MDF4GenBlock>(null, null, null, false);
		MDF4ProcessWriter ps = new MDF4ProcessWriter(con, new ArgumentStruct());

		MDF4BlocksSplittMerger splitmerger = new MDF4BlocksSplittMerger(ps, "##DT", blk, 0, null, 1024);

		//test setLinks.
		//set links to a random block.
		MDF4GenBlock blablablock = new MDF4GenBlock(0xbad);
		for(int i = 0; i < blk.getLinkCount(); i++){
			blk.setLink(i, blablablock);
		}
		splitmerger.setLinks();
		assertNull(blk.getLnkData());

	}

}
