/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.internal.core.builder;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.compiler.problem.IProblemFactory;
import org.eclipse.dltk.compiler.problem.IProblemReporter;
import org.eclipse.dltk.compiler.task.ITaskReporter;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.builder.IBuildState;
import org.eclipse.dltk.core.environment.IFileHandle;

public class SourceModuleBuildContext extends AbstractBuildContext {

	final IBuildState buildState;
	final BuildProblemReporter reporter;

	/**
	 * @param module
	 * @param buildState
	 */
	public SourceModuleBuildContext(IProblemFactory problemFactory,
			ISourceModule module, int buildType, IBuildState buildState) {
		super(module, buildType);
		this.buildState = buildState;
		final IResource resource = module.getResource();
		reporter = resource != null ? new BuildProblemReporter(problemFactory,
				resource) : null;
	}

	public IFileHandle getFileHandle() {
		return null;
	}

	public IProblemReporter getProblemReporter() {
		return reporter;
	}

	public ITaskReporter getTaskReporter() {
		return reporter;
	}

	public void recordDependency(IPath dependency, int flags) {
		if (reporter != null) {
			buildState.recordDependency(reporter.resource.getFullPath(),
					dependency, flags);
		}
	}
}
