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
package org.eclipse.dltk.testing;

/**
 * Optional interface to be implemented by {@link ITestRunnerUI} if it wants to
 * take control over the stack traces copied to the clipboard.
 */
public interface ITestRunnerUIExtension2 {

	/**
	 * Performs filtering or whatever transformation is required with the stack
	 * trace before copying to clipboard.
	 */
	String prepareStackTraceCopy(String trace);

}
