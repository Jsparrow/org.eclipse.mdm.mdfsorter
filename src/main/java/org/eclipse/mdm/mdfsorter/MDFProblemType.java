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

/**
 * The different problem types.<br>
 * LINKED_DATALIST_PROBLEM: Data is stored in a linked list, which should be
 * merged into a single block if possible.<br>
 * ZIPPED_DATA_PROBLEM: Data is stored in a single DZBLOCK, and the unzip flag
 * is set, so it has to be unzipped<br>
 * UNZIPPED_DATA_PROBLEM: Data is stored in a single DTBLOCK (or similar) and
 * the zip flag is set, so it has to be zipped.
 *
 * @author Tobias Leemann
 *
 */
public enum MDFProblemType {
	LINKED_DATALIST_PROBLEM, ZIPPED_DATA_PROBLEM, UNZIPPED_DATA_PROBLEM, UNSORTED_DATA_PROBLEM;
}