/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.compiler;

import org.eclipse.dltk.core.ISourceElementParser;

/**
 * Part of the {@link ISourceElementParser} responsible for building the output.
 * It gets notified of structural information as they are detected, relying on
 * the requestor to assemble them together, based on the notifications it got.
 * 
 * <p>
 * The structural investigation includes:
 * </p>
 * <ul>
 * <li>package statement
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
 * All positions are relative to the exact source fed to the parser.
 * </p>
 * 
 * <p>
 * Elements which are complex are notified in two steps:
 * <li><code>enterElement</code> : once the element header has been identified
 * <li><code>exitElement</code> : once the element has been fully consumed
 * 
 * <p>
 * other simpler elements (package, import) are read all at once: -
 * <code>acceptElement</code>.
 * </p>
 */
public interface ISourceElementRequestor extends IElementRequestor {
	/**
	 * Adds selected field only if it isn't already added. If field is added
	 * into a method, then field name is also compared with the method
	 * parameters names.
	 * 
	 * @param info
	 * @return <code>true</code> if field has been just added or
	 *         <code>false</code> if another field with the same was found.
	 * @since 2.0
	 */
	boolean enterFieldCheckDuplicates(IElementRequestor.FieldInfo info);

	/**
	 * equivalent to enterMethod except for removing previous declared methods
	 * with same name.
	 * 
	 * @param info
	 * @since 2.0
	 */
	void enterMethodRemoveSame(IElementRequestor.MethodInfo info);

	/**
	 * If type with same name already exist, then enter it instead.
	 * 
	 * @param info
	 * @return boolean false if no such type found.
	 */
	boolean enterTypeAppend(String fullName, String delimiter);

}
