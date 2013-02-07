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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.core.IBuildpathEntry;
import org.eclipse.dltk.core.IModelElementDelta;
import org.eclipse.dltk.core.IProjectFragment;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.search.indexing.IndexManager;
import org.eclipse.dltk.internal.core.ModelManager.PerProjectInfo;
import org.eclipse.dltk.internal.core.search.ProjectIndexerManager;

public class BuildpathChange {
	public static int NO_DELTA = 0x00;
	public static int HAS_DELTA = 0x01;
	public static int HAS_PROJECT_CHANGE = 0x02;
	public static int HAS_LIBRARY_CHANGE = 0x04;

	ScriptProject project;
	IBuildpathEntry[] oldRawBuildpath;
	IBuildpathEntry[] oldResolvedBuildpath;

	public BuildpathChange(ScriptProject project,
			IBuildpathEntry[] oldRawBuildpath,
			IBuildpathEntry[] oldResolvedBuildpath) {
		this.project = project;
		this.oldRawBuildpath = oldRawBuildpath;
		this.oldResolvedBuildpath = oldResolvedBuildpath;
	}

	private void addBuildpathDeltas(ModelElementDelta delta,
			IProjectFragment[] roots, int flag) {
		for (int i = 0; i < roots.length; i++) {
			IProjectFragment root = roots[i];
			delta.changed(root, flag);
			if ((flag & IModelElementDelta.F_REMOVED_FROM_BUILDPATH) != 0
			/*
			 * || (flag & IModelElementDelta.F_SOURCEATTACHED) != 0 || (flag &
			 * IModelElementDelta.F_SOURCEDETACHED) != 0
			 */) {
				try {
					root.close();
				} catch (ModelException e) {
					// ignore
				}
			}
		}
	}

	/*
	 * Returns the index of the item in the list if the given list contains the
	 * specified entry. If the list does not contain the entry, -1 is returned.
	 */
	private int buildpathContains(IBuildpathEntry[] list, IBuildpathEntry entry) {
		IPath[] exclusionPatterns = entry.getExclusionPatterns();
		IPath[] inclusionPatterns = entry.getInclusionPatterns();
		int listLen = list == null ? 0 : list.length;
		nextEntry: for (int i = 0; i < listLen; i++) {
			IBuildpathEntry other = list[i];
			if (other.getContentKind() == entry.getContentKind()
					&& other.getEntryKind() == entry.getEntryKind()
					&& other.isExported() == entry.isExported()
					&& other.getPath().equals(entry.getPath())) {

				// check inclusion patterns
				IPath[] otherIncludes = other.getInclusionPatterns();
				if (inclusionPatterns != otherIncludes) {
					if (inclusionPatterns == null)
						continue;
					int includeLength = inclusionPatterns.length;
					if (otherIncludes == null
							|| otherIncludes.length != includeLength)
						continue;
					for (int j = 0; j < includeLength; j++) {
						// compare toStrings instead of IPaths
						// since IPath.equals is specified to ignore trailing
						// separators
						if (!inclusionPatterns[j].toString().equals(
								otherIncludes[j].toString()))
							continue nextEntry;
					}
				}
				// check exclusion patterns
				IPath[] otherExcludes = other.getExclusionPatterns();
				if (exclusionPatterns != otherExcludes) {
					if (exclusionPatterns == null)
						continue;
					int excludeLength = exclusionPatterns.length;
					if (otherExcludes == null
							|| otherExcludes.length != excludeLength)
						continue;
					for (int j = 0; j < excludeLength; j++) {
						// compare toStrings instead of IPaths
						// since IPath.equals is specified to ignore trailing
						// separators
						if (!exclusionPatterns[j].toString().equals(
								otherExcludes[j].toString()))
							continue nextEntry;
					}
				}
				return i;
			}
		}
		return -1;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof BuildpathChange))
			return false;
		return this.project.equals(((BuildpathChange) obj).project);
	}

	/*
	 * Generates a buildpath change delta for this buildpath change. Returns
	 * whether a delta was generated, and whether project reference have
	 * changed.
	 */
	public int generateDelta(ModelElementDelta delta, boolean addBuildpathChange) {
		ModelManager manager = ModelManager.getModelManager();
		DeltaProcessingState state = manager.deltaState;
		if (state.findProject(this.project.getElementName()) == null)
			// project doesn't exist yet (we're in an IWorkspaceRunnable)
			// no need to create a delta here and no need to index (see
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=133334)
			// the delta processor will create an ADDED project delta, and index
			// the project
			return NO_DELTA;

		DeltaProcessor deltaProcessor = state.getDeltaProcessor();
		IBuildpathEntry[] newResolvedBuildpath = null;
		int result = NO_DELTA;
		try {
			PerProjectInfo perProjectInfo = this.project.getPerProjectInfo();

			// get new info
			this.project.resolveBuildpath(perProjectInfo, false/*
																 * don't use
																 * previous
																 * session
																 * values
																 */,
					addBuildpathChange);
			IBuildpathEntry[] newRawBuildpath;

			// use synchronized block to ensure consistency
			synchronized (perProjectInfo) {
				newRawBuildpath = perProjectInfo.rawBuildpath;
				newResolvedBuildpath = perProjectInfo.getResolvedBuildpath();
			}

			if (newResolvedBuildpath == null) {
				// another thread reset the resolved buildpath, use a temporary
				// PerProjectInfo
				PerProjectInfo temporaryInfo = this.project.newTemporaryInfo();
				this.project.resolveBuildpath(temporaryInfo, false/*
																 * don't use
																 * previous
																 * session
																 * values
																 */,
						addBuildpathChange);
				newRawBuildpath = temporaryInfo.rawBuildpath;
				newResolvedBuildpath = temporaryInfo.getResolvedBuildpath();
			}

			// check if raw buildpath has changed
			if (this.oldRawBuildpath != null
					&& !ScriptProject.areBuildpathsEqual(this.oldRawBuildpath,
							newRawBuildpath)) {
				delta.changed(this.project,
						IModelElementDelta.F_BUILDPATH_CHANGED);
				result |= HAS_DELTA;

				// reset containers that are no longer on the buildpath
				// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=139446)
				for (int i = 0, length = this.oldRawBuildpath.length; i < length; i++) {
					IBuildpathEntry entry = this.oldRawBuildpath[i];
					if (entry.getEntryKind() == IBuildpathEntry.BPE_CONTAINER) {
						if (buildpathContains(newRawBuildpath, entry) == -1)
							manager.containerPut(this.project, entry.getPath(),
									null);
					}
				}
			}

			// if no changes to resolved buildpath, nothing more to do
			if (this.oldResolvedBuildpath != null
					&& ScriptProject.areBuildpathsEqual(
							this.oldResolvedBuildpath, newResolvedBuildpath))
				return result;

			// close cached info
			this.project.close();

			// TODO: check is this is relevant here
			// ensure caches of dependent projects are reset as well (see
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=207890)
			// deltaProcessor.projectCachesToReset.add(this.project);
		} catch (ModelException e) {
			if (DeltaProcessor.VERBOSE) {
				e.printStackTrace();
			}
			// project no longer exist
			return result;
		}

		if (this.oldResolvedBuildpath == null)
			return result;

		delta.changed(this.project,
				IModelElementDelta.F_RESOLVED_BUILDPATH_CHANGED);
		result |= HAS_DELTA;

		state.addForRefresh(this.project);
		// ensure external jars are refreshed for this project (see
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=212769 )

		Map<IPath, IProjectFragment> removedRoots = null;
		IProjectFragment[] roots = null;
		Map<IScriptProject, IProjectFragment[]> allOldRoots;
		if ((allOldRoots = deltaProcessor.oldRoots) != null) {
			roots = allOldRoots.get(this.project);
		}
		if (roots != null) {
			removedRoots = new HashMap<IPath, IProjectFragment>();
			for (int i = 0; i < roots.length; i++) {
				IProjectFragment root = roots[i];
				removedRoots.put(root.getPath(), root);
			}
		}

		int newLength = newResolvedBuildpath.length;
		int oldLength = this.oldResolvedBuildpath.length;
		for (int i = 0; i < oldLength; i++) {
			int index = buildpathContains(newResolvedBuildpath,
					this.oldResolvedBuildpath[i]);
			if (index == -1) {
				// remote project changes
				int entryKind = this.oldResolvedBuildpath[i].getEntryKind();
				if (entryKind == IBuildpathEntry.BPE_PROJECT) {
					result |= HAS_PROJECT_CHANGE;
					continue;
				}
				if (entryKind == IBuildpathEntry.BPE_LIBRARY) {
					result |= HAS_LIBRARY_CHANGE;
				}

				IProjectFragment[] pkgFragmentRoots = null;
				if (removedRoots != null) {
					IProjectFragment oldRoot = removedRoots
							.get(this.oldResolvedBuildpath[i].getPath());
					if (oldRoot != null) { // use old root if any (could be
						// none
						// if entry wasn't bound)
						pkgFragmentRoots = new IProjectFragment[] { oldRoot };
					}
				}
				if (pkgFragmentRoots == null) {
					try {
						List<IProjectFragment> accumulatedRoots = new ArrayList<IProjectFragment>();
						HashSet<String> rootIDs = new HashSet<String>(5);
						rootIDs.add(this.project.rootID());
						this.project.computeProjectFragments(
								this.oldResolvedBuildpath[i], accumulatedRoots,
								rootIDs, null, // inside original project
								false, // don't check existence
								false, // don't retrieve exported roots
								null); /* no reverse map */
						pkgFragmentRoots = accumulatedRoots
								.toArray(new IProjectFragment[accumulatedRoots
										.size()]);
					} catch (ModelException e) {
						pkgFragmentRoots = new IProjectFragment[] {};
					}
				}
				addBuildpathDeltas(delta, pkgFragmentRoots,
						IModelElementDelta.F_REMOVED_FROM_BUILDPATH);
			} else {
				// remote project changes
				if (this.oldResolvedBuildpath[i].getEntryKind() == IBuildpathEntry.BPE_PROJECT) {
					result |= HAS_PROJECT_CHANGE;
					continue;
				}
				if (index != i) { // reordering of the buildpath
					addBuildpathDeltas(
							delta,
							this.project
									.computeProjectFragments(this.oldResolvedBuildpath[i]),
							IModelElementDelta.F_REORDER);
				}
				// TODO (alex) check source attachment
			}
		}

		for (int i = 0; i < newLength; i++) {
			int index = buildpathContains(this.oldResolvedBuildpath,
					newResolvedBuildpath[i]);
			if (index == -1) {
				// remote project changes
				int entryKind = newResolvedBuildpath[i].getEntryKind();
				if (entryKind == IBuildpathEntry.BPE_PROJECT) {
					result |= HAS_PROJECT_CHANGE;
					continue;
				}
				if (entryKind == IBuildpathEntry.BPE_LIBRARY) {
					result |= HAS_LIBRARY_CHANGE;
				}
				addBuildpathDeltas(
						delta,
						this.project
								.computeProjectFragments(newResolvedBuildpath[i]),
						IModelElementDelta.F_ADDED_TO_BUILDPATH);
			} // buildpath reordering has already been generated in previous
				// loop
		}

		return result;
	}

	@Override
	public int hashCode() {
		return this.project.hashCode();
	}

	/*
	 * Request the indexing of entries that have been added, and remove the
	 * index for removed entries.
	 */
	public void requestIndexing() {
		IBuildpathEntry[] newResolvedBuildpath = null;
		try {
			newResolvedBuildpath = this.project.getResolvedBuildpath();
		} catch (ModelException e) {
			// project doesn't exist
			return;
		}

		ModelManager manager = ModelManager.getModelManager();
		IndexManager indexManager = manager.indexManager;
		if (indexManager == null)
			return;
		DeltaProcessingState state = manager.deltaState;

		int newLength = newResolvedBuildpath.length;
		int oldLength = this.oldResolvedBuildpath == null ? 0
				: this.oldResolvedBuildpath.length;
		for (int i = 0; i < oldLength; i++) {
			int index = buildpathContains(newResolvedBuildpath,
					this.oldResolvedBuildpath[i]);
			if (index == -1) {
				// remote projects are not indexed in this project
				if (this.oldResolvedBuildpath[i].getEntryKind() == IBuildpathEntry.BPE_PROJECT) {
					continue;
				}

				// Remove the source files from the index for a source folder
				// For a lib folder or a .zip file, remove the corresponding
				// index if not shared.
				IBuildpathEntry oldEntry = this.oldResolvedBuildpath[i];
				final IPath path = oldEntry.getPath();
				int changeKind = this.oldResolvedBuildpath[i].getEntryKind();
				switch (changeKind) {
				case IBuildpathEntry.BPE_SOURCE:
					char[][] inclusionPatterns = ((BuildpathEntry) oldEntry)
							.fullInclusionPatternChars();
					char[][] exclusionPatterns = ((BuildpathEntry) oldEntry)
							.fullExclusionPatternChars();
					indexManager.removeSourceFolderFromIndex(this.project,
							path, inclusionPatterns, exclusionPatterns);
					ProjectIndexerManager.removeProjectFragment(project, path);
					break;
				case IBuildpathEntry.BPE_LIBRARY:
					if (state.otherRoots.get(path) == null) { // if root was
						// not
						// shared
						indexManager.discardJobs(path.toString());
						indexManager.removeIndex(path);
						ProjectIndexerManager.removeLibrary(project, path);
					}
					break;
				}
			}
		}

		for (int i = 0; i < newLength; i++) {
			int index = buildpathContains(this.oldResolvedBuildpath,
					newResolvedBuildpath[i]);
			if (index == -1
					|| newResolvedBuildpath[i].getEntryKind() == IBuildpathEntry.BPE_LIBRARY) {
				// remote projects are not indexed in this project
				if (newResolvedBuildpath[i].getEntryKind() == IBuildpathEntry.BPE_PROJECT) {
					continue;
				}

				// Request indexing
				int entryKind = newResolvedBuildpath[i].getEntryKind();
				switch (entryKind) {
				case IBuildpathEntry.BPE_LIBRARY:
					boolean pathHasChanged = true;
					IPath newPath = newResolvedBuildpath[i].getPath();
					for (int j = 0; j < oldLength; j++) {
						IBuildpathEntry oldEntry = this.oldResolvedBuildpath[j];
						if (oldEntry.getPath().equals(newPath)) {
							pathHasChanged = false;
							break;
						}
					}
					if (pathHasChanged) {
						// IBuildpathEntry entry = newResolvedBuildpath[i];
						// char[][] inclusionPatterns = ((BuildpathEntry) entry)
						// .fullInclusionPatternChars();
						// char[][] exclusionPatterns = ((BuildpathEntry) entry)
						// .fullExclusionPatternChars();
						ProjectIndexerManager.indexLibrary(project, newPath);
					}
					break;
				case IBuildpathEntry.BPE_SOURCE:
					IBuildpathEntry entry = newResolvedBuildpath[i];
					IPath path = entry.getPath();
					// char[][] inclusionPatterns = ((BuildpathEntry) entry)
					// .fullInclusionPatternChars();
					// char[][] exclusionPatterns = ((BuildpathEntry) entry)
					// .fullExclusionPatternChars();
					ProjectIndexerManager.indexProjectFragment(project, path);
					break;
				}
			}
		}
	}

	@Override
	public String toString() {
		return "BuildpathChange: " + this.project.getElementName(); //$NON-NLS-1$
	}
}
