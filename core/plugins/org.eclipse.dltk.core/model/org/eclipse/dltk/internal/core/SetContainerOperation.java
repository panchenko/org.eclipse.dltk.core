/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.dltk.internal.core;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.core.IBuildpathContainer;
import org.eclipse.dltk.core.IBuildpathEntry;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.core.util.Util;

public class SetContainerOperation extends ChangeBuildpathOperation {

	final IPath containerPath;
	final IScriptProject[] affectedProjects;
	final IBuildpathContainer[] respectiveContainers;

	public SetContainerOperation(IPath containerPath,
			IScriptProject[] affectedProjects,
			IBuildpathContainer[] respectiveContainers) {
		super(
				new IModelElement[] { ModelManager.getModelManager().getModel() },
				!ResourcesPlugin.getWorkspace().isTreeLocked());
		this.containerPath = containerPath;
		this.affectedProjects = affectedProjects;
		this.respectiveContainers = respectiveContainers;
	}

	@Override
	protected void executeOperation() throws ModelException {
		checkCanceled();
		try {
			beginTask("", 1); //$NON-NLS-1$

			if (ModelManager.BP_RESOLVE_VERBOSE) {
				verboseSetContainer();
				verboseSetContainer_Trace();
			}

			final ModelManager manager = ModelManager.getModelManager();
			if (manager.containerPutIfInitializingWithSameEntries(
					containerPath, affectedProjects, respectiveContainers))
				return;

			final int projectLength = affectedProjects.length;
			final IScriptProject[] modifiedProjects;
			System.arraycopy(affectedProjects, 0,
					modifiedProjects = new IScriptProject[projectLength], 0,
					projectLength);
			final IBuildpathEntry[][] oldResolvedPaths = new IBuildpathEntry[projectLength][];

			// filter out unmodified project containers
			int remaining = 0;
			for (int i = 0; i < projectLength; i++) {

				if (isCanceled())
					return;

				ScriptProject affectedProject = (ScriptProject) affectedProjects[i];
				IBuildpathContainer newContainer = respectiveContainers[i];
				if (newContainer == null)
					newContainer = ModelManager.CONTAINER_INITIALIZATION_IN_PROGRESS; // 30920
				// -
				// prevent
				// infinite
				// loop
				boolean found = false;
				if (ScriptProject.hasScriptNature(affectedProject.getProject())) {
					IBuildpathEntry[] rawClasspath = affectedProject
							.getRawBuildpath();
					for (int j = 0, cpLength = rawClasspath.length; j < cpLength; j++) {
						IBuildpathEntry entry = rawClasspath[j];
						if (entry.getEntryKind() == IBuildpathEntry.BPE_CONTAINER
								&& entry.getPath().equals(containerPath)) {
							found = true;
							break;
						}
					}
				}
				if (!found) {
					modifiedProjects[i] = null; // filter out this project -
												// does
					// not reference the container path,
					// or isnt't yet script project
					manager.containerPut(affectedProject, containerPath,
							newContainer);
					continue;
				}
				IBuildpathContainer oldContainer = manager.containerGet(
						affectedProject, containerPath);
				if (oldContainer == ModelManager.CONTAINER_INITIALIZATION_IN_PROGRESS) {
					oldContainer = null;
				}
				if (oldContainer != null
						&& oldContainer.equals(respectiveContainers[i])) {
					modifiedProjects[i] = null; // filter out this project -
					// container did not change
					continue;
				}
				remaining++;
				oldResolvedPaths[i] = affectedProject.getResolvedBuildpath(
						true/* ignoreUnresolvedEntry */, false/*
															 * don't
															 * generateMarkerOnError
															 */, false/*
																	 * don't
																	 * returnResolutionInProgress
																	 */);
				manager.containerPut(affectedProject, containerPath,
						newContainer);
			}

			if (remaining == 0)
				return;

			// trigger model refresh
			try {
				for (int i = 0; i < projectLength; i++) {

					if (isCanceled())
						return;

					ScriptProject affectedProject = (ScriptProject) modifiedProjects[i];
					if (affectedProject == null)
						continue; // was filtered out

					if (ModelManager.BP_RESOLVE_VERBOSE) {
						verboseUpdateProject(affectedProject);
					}

					// force a refresh of the affected project (will compute
					// deltas)
					affectedProject.setRawBuildpath(
							affectedProject.getRawBuildpath(), progressMonitor,
							canChangeResources, oldResolvedPaths[i], false, // updating
							// -
							// no
							// need
							// for
							// early
							// validation
							false); // updating - no need to save
				}
			} catch (CoreException e) {
				if (ModelManager.BP_RESOLVE_VERBOSE) {
					verboseFailure(e);
				}
				if (e instanceof ModelException) {
					throw (ModelException) e;
				} else {
					throw new ModelException(e);
				}
			} finally {
				for (int i = 0; i < projectLength; i++) {
					if (respectiveContainers[i] == null) {
						manager.containerPut(affectedProjects[i],
								containerPath, null); // reset init in progress
														// marker
					}
				}
			}
		} finally {
			done();
		}
	}

	private void verboseFailure(CoreException e) {
		Util.verbose("CPContainer SET  - FAILED DUE TO EXCEPTION\n" + //$NON-NLS-1$
				"	container path: " + containerPath, //$NON-NLS-1$
				System.err);
		e.printStackTrace();
	}

	private void verboseUpdateProject(ScriptProject affectedProject) {
		Util.verbose("BPContainer SET  - updating affected project due to setting container\n" + //$NON-NLS-1$
				"	project: " //$NON-NLS-1$
				+ affectedProject.getElementName()
				+ '\n'
				+ "	container path: " + containerPath); //$NON-NLS-1$
	}

	private void verboseSetContainer_Trace() {
		new Exception("<Fake exception>").printStackTrace(System.out); //$NON-NLS-1$
	}

	private void verboseSetContainer() {
		Util.verbose("BPContainer SET  - setting container\n" + //$NON-NLS-1$
				"	container path: " //$NON-NLS-1$
				+ containerPath + '\n' + "	projects: {" //$NON-NLS-1$
				+ Util.toString(affectedProjects, new Util.Displayable() {
					public String displayString(Object o) {
						return ((IScriptProject) o).getElementName();
					}
				}) + "}\n	values: {\n" + //$NON-NLS-1$
				Util.toString(respectiveContainers, new Util.Displayable() {
					public String displayString(Object o) {
						StringBuffer buffer = new StringBuffer("		"); //$NON-NLS-1$
						if (o == null) {
							buffer.append("<null>"); //$NON-NLS-1$
							return buffer.toString();
						}
						IBuildpathContainer container = (IBuildpathContainer) o;
						buffer.append(container.getDescription());
						buffer.append(" {\n"); //$NON-NLS-1$
						IBuildpathEntry[] entries = container
								.getBuildpathEntries();
						if (entries != null) {
							for (int i = 0; i < entries.length; i++) {
								buffer.append(" 			"); //$NON-NLS-1$
								buffer.append(entries[i]);
								buffer.append('\n');
							}
						}
						buffer.append(" 		}"); //$NON-NLS-1$
						return buffer.toString();
					}
				}) + "\n	}\n	invocation stack trace:"); //$NON-NLS-1$
	}

}
