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

import org.eclipse.dltk.annotations.Nullable;

/**
 * Optional interface to be implemented {@link IProblemIdentifier problem
 * identifiers}.
 */
public interface IProblemIdentifierExtension2 {

	/**
	 * Returns the "main" problem identifier which should be used instead of
	 * this one when reading the configured problem severity in
	 * {@link DefaultProblemSeverityTranslator}.
	 */
	@Nullable
	IProblemIdentifier getPrimeIdentifier();

}
