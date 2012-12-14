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
package org.eclipse.dltk.core;

import java.util.List;

import org.eclipse.dltk.annotations.NonNull;
import org.eclipse.dltk.annotations.Nullable;
import org.eclipse.dltk.codeassist.ISelectionEngine;
import org.eclipse.dltk.codeassist.ISelectionRequestor;

/**
 * Code selection result returned from the
 * {@link ICodeAssist#codeSelectAll(int, int)} methods.
 */
public interface ICodeSelection extends Iterable<Object> {

	/**
	 * Returns the number of elements in this selection, never {@code 0}.
	 */
	int size();

	/**
	 * Returns the elements in this selection as an array.
	 */
	@NonNull
	Object[] toArray();

	/**
	 * Returns the elements in this selection as a List.
	 */
	@NonNull
	List<Object> toList();

	/**
	 * Returns only {@link IModelElement}s of this selection as List.
	 */
	@NonNull
	List<IModelElement> getModelElements();

	/**
	 * Returns the proposed source range of the reference to the specified
	 * element of this selection or <code>null</code> if it wasn't reported. By
	 * default the word/identifier at the specified location is used as the
	 * range, however some languages allow use of non-alphanumeric characters in
	 * the identifiers, so {@link ISelectionEngine}s can report the exact source
	 * range of the reference via additional methods of
	 * {@link ISelectionRequestor}.
	 */
	@Nullable
	ISourceRange rangeOf(Object element);

}
