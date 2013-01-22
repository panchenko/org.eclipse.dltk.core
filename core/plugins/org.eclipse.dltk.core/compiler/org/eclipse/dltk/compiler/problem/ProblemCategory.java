/*******************************************************************************
 * Copyright (c) 2011 NumberFour AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     NumberFour AG - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.compiler.problem;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.dltk.core.builder.IBuildState;

/**
 * Predefined problem categories.
 */
public enum ProblemCategory implements IProblemCategory {
	/**
	 * Special problem category for the problems with "imports". The files with
	 * problems from this category are always recompiled during incremental
	 * builds in a hope that missing dependency had appeared.
	 * 
	 * <p>
	 * This category is kind of virtual, as it has no predefined contents,
	 * instead of that {@link IProblemIdentifier}s implementing
	 * {@link IProblemIdentifierExtension3} interface are queried at runtime if
	 * they belong to this category.
	 * </p>
	 * 
	 * @see IProblemIdentifierExtension3#belongsTo(IProblemCategory)
	 * @see IBuildState#recordImportProblem(org.eclipse.core.runtime.IPath)
	 */
	IMPORT;

	public Collection<IProblemIdentifier> contents() {
		return Collections.emptyList();
	}
}
