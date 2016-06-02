/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.mdfsorter;

/**
 * Exception thrown by the MDFSorter Argument parser.
 * @author Tobias Leemann
 *
 */
public class MDFSorterArgException extends IllegalArgumentException{

	public MDFSorterArgException(String string) {
		super(string);
	}

	private static final long serialVersionUID = 1618963240499723986L;

}
