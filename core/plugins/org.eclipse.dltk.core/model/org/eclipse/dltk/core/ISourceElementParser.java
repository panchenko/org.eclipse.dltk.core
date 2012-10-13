/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.dltk.core;

import org.eclipse.dltk.compiler.ISourceElementRequestor;
import org.eclipse.dltk.compiler.env.IModuleSource;
import org.eclipse.dltk.compiler.problem.IProblemReporter;

/**
 * A source element parser extracts structural and reference information from a
 * piece of source.
 * 
 * <p>
 * The structural investigation includes:
 * </p>
 * <ul>
 * <li>the package statement
 * <li>import statements
 * <li>types
 * <li>fields
 * <li>methods
 * </ul>
 * 
 * <p>
 * If reference information is requested, then all source constructs are
 * investigated and type, field & method references are provided as well.
 * </p>
 * 
 * <p>
 * Any (parsing) problem encountered is also provided.
 * </p>
 * 
 * <p>
 * Language specific implementation is contributed via
 * <code>org.eclipse.dltk.core.sourceElementParsers</code>.
 * </p>
 * 
 * @see ISourceElementRequestor
 */
public interface ISourceElementParser {
	/**
	 * Parses contents of the module and report all information to the
	 * requestor.
	 */
	void parseSourceModule(IModuleSource module);

	void setRequestor(ISourceElementRequestor requestor);

	void setReporter(IProblemReporter reporter);
}
