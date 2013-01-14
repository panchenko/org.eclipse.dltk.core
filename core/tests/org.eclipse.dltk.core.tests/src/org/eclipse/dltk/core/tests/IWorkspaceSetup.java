/*******************************************************************************
 * Copyright (c) 2012 NumberFour AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     NumberFour AG - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.core.tests;

import java.io.File;

/**
 * Source workspace, containing initial project contents for the tests.
 * 
 * @see WorkspaceSetup
 */
public interface IWorkspaceSetup {

	void before() throws Throwable;

	/**
	 * Returns the bundle name this source workspace is contained in.
	 */
	String getBundleName();

	/**
	 * Returns the file system path to this source workspace.
	 */
	File getSourceWorkspaceDirectory();

	void after();

}
