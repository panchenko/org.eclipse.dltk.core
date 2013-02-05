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

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ModelException;

/*
 * Abstract class for operations that change the buildpath
 */
public abstract class ChangeBuildpathOperation extends ModelOperation {

	protected boolean canChangeResources;

	public ChangeBuildpathOperation(IModelElement[] elements,
			boolean canChangeResources) {
		super(elements);
		this.canChangeResources = canChangeResources;
	}

	@Override
	protected boolean canModifyRoots() {
		// changing the buildpath can modify roots
		return true;
	}

	/*
	 * The resolved buildpath of the given project may have changed: - generate
	 * a delta - trigger indexing - update project references - create resolved
	 * buildpath markers
	 */
	protected void buildpathChanged(BuildpathChange change)
			throws ModelException {
		// reset the project's caches early since some clients rely on the
		// project's caches being up-to-date when run inside an
		// IWorkspaceRunnable
		// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=212769#c5 )
		ScriptProject project = change.project;
		project.resetCaches();

		if (this.canChangeResources) {
			// workaround for
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=177922
			if (isTopLevelOperation()
					&& !ResourcesPlugin.getWorkspace().isTreeLocked()) {
				new BuildpathValidation(project).validate();
			}

			// delta, indexing and buildpath markers are going to be created by
			// the delta processor
			// while handling the .buildpath file change

			// however ensure project references are updated
			// since some clients rely on the project references when run inside
			// an IWorkspaceRunnable
			new ProjectReferenceChange(project, change.oldResolvedBuildpath)
					.updateProjectReferencesIfNecessary();

			// and ensure that external folders are updated as well
			new ExternalFolderChange(project, change.oldResolvedBuildpath)
					.updateExternalFoldersIfNecessary(true/*
														 * refresh if external
														 * linked folder already
														 * exists
														 */, null);

		} else {
			DeltaProcessingState state = ModelManager.getModelManager().deltaState;
			ModelElementDelta delta = new ModelElementDelta(getModel());
			int result = change.generateDelta(delta);
			if ((result & BuildpathChange.HAS_DELTA) != 0) {
				// create delta
				addDelta(delta);

				// need to recompute root infos
				state.rootsAreStale = true;

				// ensure indexes are updated
				change.requestIndexing();

				// ensure buildpath is validated on next build
				state.addBuildpathValidation(project);
			}
			if ((result & BuildpathChange.HAS_PROJECT_CHANGE) != 0) {
				// ensure project references are updated on next build
				state.addProjectReferenceChange(project,
						change.oldResolvedBuildpath);
			}
			if ((result & BuildpathChange.HAS_LIBRARY_CHANGE) != 0) {
				// ensure external folders are updated on next build
				state.addExternalFolderChange(project,
						change.oldResolvedBuildpath);
			}
		}
	}

	@Override
	protected ISchedulingRule getSchedulingRule() {
		return null; // no lock taken while changing buildpath
	}

	@Override
	public boolean isReadOnly() {
		return !this.canChangeResources;
	}

}
