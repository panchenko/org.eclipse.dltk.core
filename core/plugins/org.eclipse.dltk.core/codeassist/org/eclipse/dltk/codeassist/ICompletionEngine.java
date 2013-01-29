/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.codeassist;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dltk.annotations.ExtensionPoint;
import org.eclipse.dltk.compiler.env.IModuleSource;
import org.eclipse.dltk.core.CompletionRequestor;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.internal.core.InternalDLTKLanguageManager;

/**
 * This interface is the entry point for source completions.
 */
@ExtensionPoint(point = InternalDLTKLanguageManager.COMPLETION_ENGINE_EXTPOINT, element = "completionEngine", attribute = "class")
public interface ICompletionEngine {
	/**
	 * Ask the engine to compute a completion at the specified position of the
	 * given source.
	 * <p>
	 * Completion results are answered through a requestor.
	 * </p>
	 * 
	 * @param module
	 *            the source where the completion is taking place.
	 * @param position
	 *            a position in the source where the completion is taking place.
	 *            This position is relative to the source provided.
	 */
	void complete(IModuleSource module, int position, int i);

	/**
	 * Sets the requestor accepting all the completion proposals from this
	 * engine.
	 */
	void setRequestor(CompletionRequestor requestor);

	/**
	 * Sets some options, most probably this method will be removed from the
	 * API.
	 */
	void setOptions(Map options);

	/**
	 * Sets the project, most probably this method will be removed from the API
	 */
	void setProject(IScriptProject project);

	/**
	 * Sets the {@link IProgressMonitor} which can be used by the long-running
	 * completion evaluation for the feedback and cancellation check.
	 * 
	 * @since 2.0
	 */
	void setProgressMonitor(IProgressMonitor progressMonitor);
}
