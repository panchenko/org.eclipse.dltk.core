/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.internal.debug.core.model.operations;

import org.eclipse.dltk.dbgp.exceptions.DbgpException;
import org.eclipse.dltk.debug.core.model.IScriptThread;

public class DbgpSuspendOperation extends DbgpOperation {
	private static final String JOB_NAME = Messages.DbgpSuspendOperation_suspendOperation;

	public DbgpSuspendOperation(IScriptThread thread, IResultHandler finish) {
		super(thread, JOB_NAME, finish);
	}

	protected void process() throws DbgpException {
		/* boolean success = */getExtended().makeBreak();
		// TODO: think about what to do with this status
	}
}
