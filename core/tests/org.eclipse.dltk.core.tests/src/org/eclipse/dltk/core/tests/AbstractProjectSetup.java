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
package org.eclipse.dltk.core.tests;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IProjectFragment;
import org.eclipse.dltk.core.IScriptFolder;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.core.util.Util;
import org.junit.rules.ExternalResource;

public abstract class AbstractProjectSetup extends ExternalResource {

	public abstract IProject get();

	/**
	 * Returns this project as {@link IScriptProject}
	 */
	public IScriptProject getScriptProject() {
		return DLTKCore.create(get());
	}

	/**
	 * Returns the specified file from this project.
	 */
	public IFile getFile(String name) {
		return get().getFile(name);
	}

	/**
	 * Returns the specified file from this project.
	 */
	public IFile getFile(IPath path) {
		return get().getFile(path);
	}

	/**
	 * Returns the specified package fragment root in this project, or
	 * <code>null</code> if it does not exist. If relative, the rootPath must be
	 * specified as a project relative path. The empty path refers to the
	 * package fragment root that is the project folder iteslf. If absolute, the
	 * rootPath refers to either an external zip, or a resource internal to the
	 * workspace
	 */
	public IProjectFragment getProjectFragment(String fragmentPath)
			throws ModelException {
		final IScriptProject project = getScriptProject();
		if (project == null) {
			return null;
		}
		final IPath path = new Path(fragmentPath);
		if (path.isAbsolute()) {
			final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace()
					.getRoot();
			final IResource resource = workspaceRoot.findMember(path);
			if (resource == null) {
				return null;
			}
			// resource in the workspace
			return project.getProjectFragment(resource);
		} else {
			final IProjectFragment[] roots = project.getProjectFragments();
			if (roots == null || roots.length == 0) {
				return null;
			}
			for (IProjectFragment root : roots) {
				if (root.getUnderlyingResource().getProjectRelativePath()
						.equals(path)) {
					return root;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the specified script folder in this project and the given
	 * fragment, or <code>null</code> if it does not exist. The rootPath must be
	 * specified as a project relative path. The empty path refers to the
	 * default package fragment.
	 */
	public IScriptFolder getScriptFolder(String fragmentPath, String path)
			throws ModelException {
		return getScriptFolder(fragmentPath, new Path(path));
	}

	/**
	 * Returns the specified script folder in this project and the given
	 * fragment, or <code>null</code> if it does not exist. The rootPath must be
	 * specified as a project relative path. The empty path refers to the
	 * default package fragment.
	 */
	public IScriptFolder getScriptFolder(String fragmentPath, IPath path)
			throws ModelException {
		final IProjectFragment root = getProjectFragment(fragmentPath);
		if (root == null) {
			return null;
		}
		return root.getScriptFolder(path);
	}

	/**
	 * Returns the specified source module in this project and the given root
	 * and folder or <code>null</code> if it does not exist.
	 */
	public ISourceModule getSourceModule(String rootPath, IPath path)
			throws ModelException {
		final IScriptFolder folder = getScriptFolder(rootPath,
				path.removeLastSegments(1));
		if (folder == null) {
			return null;
		}
		return folder.getSourceModule(path.lastSegment());
	}

	/**
	 * Returns the specified source module in this project and the given root
	 * and folder or <code>null</code> if it does not exist.
	 */
	public ISourceModule getSourceModule(String rootPath, String path)
			throws ModelException {
		return getSourceModule(rootPath, new Path(path));
	}

	public String getFileContentsAsString(String name) throws CoreException {
		return getFileContentsAsString(getFile(name));
	}

	public String getFileContentsAsString(IFile file) throws CoreException {
		return new String(Util.getResourceContentsAsCharArray(file));
	}

	/**
	 * Returns workspace this project belongs to.
	 */
	public IWorkspace getWorkspace() {
		return get().getWorkspace();
	}

}
