/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.dltk.internal.core;

import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dltk.core.IBuildpathEntry;
import org.eclipse.dltk.core.ModelException;

public class ExternalFolderChange {

	private ScriptProject project;
	private IBuildpathEntry[] oldResolvedBuildpath;

	public ExternalFolderChange(ScriptProject project,
			IBuildpathEntry[] oldResolvedBuildpath) {
		this.project = project;
		this.oldResolvedBuildpath = oldResolvedBuildpath;
	}

	/*
	 * Update external folders
	 */
	public void updateExternalFoldersIfNecessary(boolean refreshIfExistAlready,
			IProgressMonitor monitor) throws ModelException {
		HashSet<IPath> oldFolders = ExternalFoldersManager
				.getExternalFolders(this.oldResolvedBuildpath);
		IBuildpathEntry[] newResolvedBuildpath = this.project
				.getResolvedBuildpath();
		HashSet<IPath> newFolders = ExternalFoldersManager
				.getExternalFolders(newResolvedBuildpath);
		if (newFolders == null)
			return;
		ExternalFoldersManager foldersManager = ModelManager
				.getExternalManager();
		for (IPath folderPath : newFolders) {
			if (oldFolders == null || !oldFolders.remove(folderPath)) {
				try {
					foldersManager.createLinkFolder(folderPath,
							refreshIfExistAlready, monitor);
				} catch (CoreException e) {
					ModelException.propagate(e);
				}
			}
		}
		// removal of linked folders is done during save
	}

	public String toString() {
		return "ExternalFolderChange: " + this.project.getElementName(); //$NON-NLS-1$
	}
}
