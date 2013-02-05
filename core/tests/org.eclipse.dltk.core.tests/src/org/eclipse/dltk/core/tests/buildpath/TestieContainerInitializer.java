/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.core.tests.buildpath;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.core.BuildpathContainerInitializer;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IBuildpathContainer;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.environment.EnvironmentManager;
import org.eclipse.dltk.core.environment.EnvironmentPathUtils;

public class TestieContainerInitializer extends BuildpathContainerInitializer {
	@Override
	public void initialize(IPath containerPath, IScriptProject project)
			throws CoreException {
		int size = containerPath.segmentCount();
		IPath path = EnvironmentPathUtils.getFullPath(
				EnvironmentManager.getEnvironment(project),
				containerPath.removeFirstSegments(1));
		path = path.makeAbsolute();
		if (size > 0) {
			TestieContainer container = new TestieContainer(path);
			DLTKCore.setBuildpathContainer(containerPath,
					new IScriptProject[] { project },
					new IBuildpathContainer[] { container }, null);
		}
	}
}
