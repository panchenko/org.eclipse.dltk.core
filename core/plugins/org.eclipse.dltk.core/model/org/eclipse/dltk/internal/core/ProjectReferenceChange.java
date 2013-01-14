/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.internal.core;

import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.dltk.compiler.CharOperation;
import org.eclipse.dltk.core.IBuildpathEntry;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.core.util.Util;

public class ProjectReferenceChange {

	private ScriptProject project;
	private IBuildpathEntry[] oldResolvedBuildpath;

	public ProjectReferenceChange(ScriptProject project,
			IBuildpathEntry[] oldResolvedBuildpath) {
		this.project = project;
		this.oldResolvedBuildpath = oldResolvedBuildpath;
	}

	/*
	 * Update projects references so that the build order is consistent with the
	 * buildpath
	 */
	public void updateProjectReferencesIfNecessary() throws ModelException {
		if (!project.getProject().isAccessible())
			return;
		String[] oldRequired = this.oldResolvedBuildpath == null ? CharOperation.NO_STRINGS
				: this.project.projectPrerequisites(this.oldResolvedBuildpath);
		IBuildpathEntry[] newResolvedBuildpath = this.project
				.getResolvedBuildpath();
		String[] newRequired = this.project
				.projectPrerequisites(newResolvedBuildpath);
		final IProject projectResource = this.project.getProject();
		try {
			IProjectDescription description = projectResource.getDescription();

			IProject[] projectReferences = description.getDynamicReferences();

			HashSet<String> oldReferences = new HashSet<String>(
					projectReferences.length);
			for (int i = 0; i < projectReferences.length; i++) {
				String projectName = projectReferences[i].getName();
				oldReferences.add(projectName);
			}
			@SuppressWarnings("unchecked")
			HashSet<String> newReferences = (HashSet<String>) oldReferences
					.clone();

			for (int i = 0; i < oldRequired.length; i++) {
				String projectName = oldRequired[i];
				newReferences.remove(projectName);
			}
			for (int i = 0; i < newRequired.length; i++) {
				String projectName = newRequired[i];
				newReferences.add(projectName);
			}

			int newSize = newReferences.size();

			checkIdentity: {
				if (oldReferences.size() == newSize) {
					for (String newRef : newReferences) {
						if (!oldReferences.contains(newRef)) {
							break checkIdentity;
						}
					}
					return;
				}
			}
			String[] requiredProjectNames = new String[newSize];
			int index = 0;
			for (String newRef : newReferences) {
				requiredProjectNames[index++] = newRef;
			}
			Util.sort(requiredProjectNames); // ensure that if changed, the
												// order is consistent

			final IProject[] requiredProjectArray = new IProject[newSize];
			IWorkspaceRoot wksRoot = projectResource.getWorkspace().getRoot();
			for (int i = 0; i < newSize; i++) {
				requiredProjectArray[i] = wksRoot
						.getProject(requiredProjectNames[i]);
			}

			// ensure that a scheduling rule is used so that the project
			// description is not modified by another thread while we update it
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=214981
			// also ensure that if no change (checkIdentify block returned
			// above) we don't reach here
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=241751
			IWorkspace workspace = projectResource.getWorkspace();
			ISchedulingRule rule = workspace.getRuleFactory().modifyRule(
					projectResource); // scheduling rule for modifying the
										// project
			IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					IProjectDescription description = projectResource
							.getDescription();
					description.setDynamicReferences(requiredProjectArray);
					projectResource.setDescription(description,
							IResource.AVOID_NATURE_CONFIG, null);
				}
			};
			workspace.run(runnable, rule, IWorkspace.AVOID_UPDATE, null);
		} catch (CoreException e) {
			if (!ExternalScriptProject.EXTERNAL_PROJECT_NAME
					.equals(this.project.getElementName()))
				throw new ModelException(e);
		}
	}
}
