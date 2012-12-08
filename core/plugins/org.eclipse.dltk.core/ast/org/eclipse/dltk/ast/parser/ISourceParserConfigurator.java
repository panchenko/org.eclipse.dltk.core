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
package org.eclipse.dltk.ast.parser;

import org.eclipse.core.resources.IProject;
import org.eclipse.dltk.annotations.ExtensionPoint;
import org.eclipse.dltk.annotations.NonNull;
import org.eclipse.dltk.annotations.Nullable;

/**
 * Implementations of this interface allow 3rd parties to configure newly
 * created parser instances for the specified project.
 */
@ExtensionPoint(point = SourceParserManager.SOURCE_PARSER_EXT_POINT, element = SourceParserManager.PARSER_CONFIGURATOR_TAG, attribute = SourceParserManager.PARSER_CONFIGURATOR_CLASS)
public interface ISourceParserConfigurator {

	/**
	 * Configure the specified parser instance.
	 * 
	 * @param parser
	 *            parser instance
	 * @param project
	 *            project or <code>null</code> if parser was instantiated
	 *            without specifying the project
	 */
	void configure(@NonNull ISourceParser parser, @Nullable IProject project);

}
