/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.internal.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IBuildpathEntry;
import org.eclipse.dltk.core.IElementChangedListener;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IScriptModel;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.core.util.Util;

/**
 * Keep the global states used during script element delta processing.
 */
public class DeltaProcessingState implements IResourceChangeListener {

	/*
	 * Collection of listeners for model element deltas
	 */
	public IElementChangedListener[] elementChangedListeners = new IElementChangedListener[5];
	public int[] elementChangedListenerMasks = new int[5];
	public int elementChangedListenerCount = 0;

	/*
	 * Collection of pre script resource change listeners
	 */
	public IResourceChangeListener[] preResourceChangeListeners = new IResourceChangeListener[1];
	public int[] preResourceChangeEventMasks = new int[1];
	public int preResourceChangeListenerCount = 0;

	/*
	 * The delta processor for the current thread.
	 */
	private ThreadLocal deltaProcessors = new ThreadLocal();

	/* A table from IPath (from a buildpath entry) to RootInfo */
	public HashMap roots = new HashMap();

	/*
	 * A table from IPath (from a buildpath entry) to ArrayList of RootInfo Used
	 * when an IPath corresponds to more than one root
	 */
	public HashMap otherRoots = new HashMap();

	/*
	 * A table from IPath (from a buildpath entry) to RootInfo from the last
	 * time the delta processor was invoked.
	 */
	public HashMap oldRoots = new HashMap();

	/*
	 * A table from IPath (from a buildpath entry) to ArrayList of RootInfo from
	 * the last time the delta processor was invoked. Used when an IPath
	 * corresponds to more than one root
	 */
	public HashMap oldOtherRoots = new HashMap();

	/*
	 * A table from IScriptProject to IScriptProject[] (the list of direct
	 * dependent of the key)
	 */
	public HashMap projectDependencies = new HashMap();

	/* Whether the roots tables should be recomputed */
	public boolean rootsAreStale = true;

	/* Threads that are currently running initializeRoots() */
	private Set initializingThreads = Collections
			.synchronizedSet(new HashSet());

	/* A table from file system absoulte path (String) to timestamp (Long) */
	public PersistentTimeStampMap externalTimeStamps;

	/* A table from path (String) to timestamp (Long) */
	public PersistentTimeStampMap customTimeStamps;

	/* A table from ScriptProject to BuildpathValidation */
	private HashMap<ScriptProject, BuildpathValidation> buildpathValidations = new HashMap<ScriptProject, BuildpathValidation>();

	/* A table from ScriptProject to ProjectReferenceChange */
	private HashMap<ScriptProject, ProjectReferenceChange> projectReferenceChanges = new HashMap<ScriptProject, ProjectReferenceChange>();

	/* A table from JavaProject to ExternalFolderChange */
	private HashMap externalFolderChanges = new HashMap();

	/**
	 * Workaround for bug 15168 circular errors not reported This is a cache of
	 * the projects before any project addition/deletion has started.
	 */
	private HashSet<String> scriptProjectNamesCache;

	/*
	 * Need to clone defensively the listener information, in case some listener
	 * is reacting to some notification iteration by adding/changing/removing
	 * any of the other (for example, if it deregisters itself).
	 */
	public synchronized void addElementChangedListener(
			IElementChangedListener listener, int eventMask) {
		for (int i = 0; i < this.elementChangedListenerCount; i++) {
			if (this.elementChangedListeners[i].equals(listener)) {

				// only clone the masks, since we could be in the middle of
				// notifications and one listener decide to change
				// any event mask of another listeners (yet not notified).
				int cloneLength = this.elementChangedListenerMasks.length;
				System
						.arraycopy(
								this.elementChangedListenerMasks,
								0,
								this.elementChangedListenerMasks = new int[cloneLength],
								0, cloneLength);
				this.elementChangedListenerMasks[i] = eventMask; // could be
				// different
				return;
			}
		}
		// may need to grow, no need to clone, since iterators will have cached
		// original arrays and max boundary and we only add to the end.
		int length;
		if ((length = this.elementChangedListeners.length) == this.elementChangedListenerCount) {
			System
					.arraycopy(
							this.elementChangedListeners,
							0,
							this.elementChangedListeners = new IElementChangedListener[length * 2],
							0, length);
			System.arraycopy(this.elementChangedListenerMasks, 0,
					this.elementChangedListenerMasks = new int[length * 2], 0,
					length);
		}
		this.elementChangedListeners[this.elementChangedListenerCount] = listener;
		this.elementChangedListenerMasks[this.elementChangedListenerCount] = eventMask;
		this.elementChangedListenerCount++;
	}

	public void addPreResourceChangedListener(IResourceChangeListener listener,
			int eventMask) {
		for (int i = 0; i < this.preResourceChangeListenerCount; i++) {
			if (this.preResourceChangeListeners[i].equals(listener)) {
				this.preResourceChangeEventMasks[i] |= eventMask;
				return;
			}
		}
		// may need to grow, no need to clone, since iterators will have cached
		// original arrays and max boundary and we only add to the end.
		int length;
		if ((length = this.preResourceChangeListeners.length) == this.preResourceChangeListenerCount) {
			System
					.arraycopy(
							this.preResourceChangeListeners,
							0,
							this.preResourceChangeListeners = new IResourceChangeListener[length * 2],
							0, length);
			System.arraycopy(this.preResourceChangeEventMasks, 0,
					this.preResourceChangeEventMasks = new int[length * 2], 0,
					length);
		}
		this.preResourceChangeListeners[this.preResourceChangeListenerCount] = listener;
		this.preResourceChangeEventMasks[this.preResourceChangeListenerCount] = eventMask;
		this.preResourceChangeListenerCount++;
	}

	public DeltaProcessor getDeltaProcessor() {
		DeltaProcessor deltaProcessor = (DeltaProcessor) this.deltaProcessors
				.get();
		if (deltaProcessor != null)
			return deltaProcessor;
		deltaProcessor = new DeltaProcessor(this, ModelManager
				.getModelManager());
		this.deltaProcessors.set(deltaProcessor);
		return deltaProcessor;
	}

	public synchronized BuildpathValidation addBuildpathValidation(
			ScriptProject project) {
		BuildpathValidation validation = this.buildpathValidations.get(project);
		if (validation == null) {
			validation = new BuildpathValidation(project);
			this.buildpathValidations.put(project, validation);
		}
		return validation;
	}

	public synchronized void addExternalFolderChange(ScriptProject project,
			IBuildpathEntry[] oldResolvedClasspath) {
		ExternalFolderChange change = (ExternalFolderChange) this.externalFolderChanges
				.get(project);
		if (change == null) {
			change = new ExternalFolderChange(project, oldResolvedClasspath);
			this.externalFolderChanges.put(project, change);
		}
	}

	public synchronized void addProjectReferenceChange(ScriptProject project,
			IBuildpathEntry[] oldResolvedBuildpath) {
		ProjectReferenceChange change = this.projectReferenceChanges
				.get(project);
		if (change == null) {
			change = new ProjectReferenceChange(project, oldResolvedBuildpath);
			this.projectReferenceChanges.put(project, change);
		}
	}

	public void initializeRoots() {

		// recompute root infos only if necessary
		HashMap newRoots = null;
		HashMap newOtherRoots = null;
		HashMap newProjectDependencies = null;
		if (this.rootsAreStale) {
			Thread currentThread = Thread.currentThread();
			boolean addedCurrentThread = false;
			try {
				// if reentering initialization (through a container initializer
				// for example) no need to compute roots again
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=47213
				if (!this.initializingThreads.add(currentThread))
					return;
				addedCurrentThread = true;

				// all buildpaths in the workspace are going to be resolved
				// ensure that containers are initialized in one batch
				ModelManager.getModelManager().batchContainerInitializations = true;

				newRoots = new HashMap();
				newOtherRoots = new HashMap();
				newProjectDependencies = new HashMap();

				IScriptModel model = ModelManager.getModelManager().getModel();
				IScriptProject[] projects;
				try {
					projects = model.getScriptProjects();
				} catch (ModelException e) {
					// nothing can be done
					return;
				}
				for (int i = 0, length = projects.length; i < length; i++) {
					ScriptProject project = (ScriptProject) projects[i];
					IBuildpathEntry[] buildpath;
					try {
						buildpath = project.getResolvedBuildpath();
					} catch (ModelException e) {
						// continue with next project
						continue;
					}
					for (int j = 0, buildpathLength = buildpath.length; j < buildpathLength; j++) {
						IBuildpathEntry entry = buildpath[j];
						if (entry.getEntryKind() == IBuildpathEntry.BPE_PROJECT) {
							IScriptProject key = model.getScriptProject(entry
									.getPath().segment(0)); // TODO (jerome)
							// reuse handle
							IScriptProject[] dependents = (IScriptProject[]) newProjectDependencies
									.get(key);
							if (dependents == null) {
								dependents = new IScriptProject[] { project };
							} else {
								int dependentsLength = dependents.length;
								System
										.arraycopy(
												dependents,
												0,
												dependents = new IScriptProject[dependentsLength + 1],
												0, dependentsLength);
								dependents[dependentsLength] = project;
							}
							newProjectDependencies.put(key, dependents);
							continue;
						}

						// root path
						IPath path = entry.getPath();
						if (newRoots.get(path) == null) {
							newRoots.put(path, new DeltaProcessor.RootInfo(
									project, path, ((BuildpathEntry) entry)
											.fullInclusionPatternChars(),
									((BuildpathEntry) entry)
											.fullExclusionPatternChars(), entry
											.getEntryKind()));
						} else {
							ArrayList rootList = (ArrayList) newOtherRoots
									.get(path);
							if (rootList == null) {
								rootList = new ArrayList();
								newOtherRoots.put(path, rootList);
							}
							rootList.add(new DeltaProcessor.RootInfo(project,
									path, ((BuildpathEntry) entry)
											.fullInclusionPatternChars(),
									((BuildpathEntry) entry)
											.fullExclusionPatternChars(), entry
											.getEntryKind()));
						}
					}
				}
			} finally {
				if (addedCurrentThread) {
					this.initializingThreads.remove(currentThread);
				}
			}
		}
		synchronized (this) {
			this.oldRoots = this.roots;
			this.oldOtherRoots = this.otherRoots;
			if (this.rootsAreStale && newRoots != null) { // double check again
				this.roots = newRoots;
				this.otherRoots = newOtherRoots;
				this.projectDependencies = newProjectDependencies;
				this.rootsAreStale = false;
			}
		}
	}

	public synchronized BuildpathValidation[] removeBuildpathValidations() {
		int length = this.buildpathValidations.size();
		if (length == 0)
			return null;
		BuildpathValidation[] validations = new BuildpathValidation[length];
		this.buildpathValidations.values().toArray(validations);
		this.buildpathValidations.clear();
		return validations;
	}

	public synchronized ProjectReferenceChange[] removeProjectReferenceChanges() {
		int length = this.projectReferenceChanges.size();
		if (length == 0)
			return null;
		ProjectReferenceChange[] updates = new ProjectReferenceChange[length];
		this.projectReferenceChanges.values().toArray(updates);
		this.projectReferenceChanges.clear();
		return updates;
	}

	public synchronized void removeElementChangedListener(
			IElementChangedListener listener) {

		for (int i = 0; i < this.elementChangedListenerCount; i++) {

			if (this.elementChangedListeners[i].equals(listener)) {

				// need to clone defensively since we might be in the middle of
				// listener notifications (#fire)
				int length = this.elementChangedListeners.length;
				IElementChangedListener[] newListeners = new IElementChangedListener[length];
				System.arraycopy(this.elementChangedListeners, 0, newListeners,
						0, i);
				int[] newMasks = new int[length];
				System.arraycopy(this.elementChangedListenerMasks, 0, newMasks,
						0, i);

				// copy trailing listeners
				int trailingLength = this.elementChangedListenerCount - i - 1;
				if (trailingLength > 0) {
					System.arraycopy(this.elementChangedListeners, i + 1,
							newListeners, i, trailingLength);
					System.arraycopy(this.elementChangedListenerMasks, i + 1,
							newMasks, i, trailingLength);
				}

				// update manager listener state (#fire need to iterate over
				// original listeners through a local variable to hold onto
				// the original ones)
				this.elementChangedListeners = newListeners;
				this.elementChangedListenerMasks = newMasks;
				this.elementChangedListenerCount--;
				return;
			}
		}
	}

	public void removePreResourceChangedListener(
			IResourceChangeListener listener) {

		for (int i = 0; i < this.preResourceChangeListenerCount; i++) {

			if (this.preResourceChangeListeners[i].equals(listener)) {

				// need to clone defensively since we might be in the middle of
				// listener notifications (#fire)
				int length = this.preResourceChangeListeners.length;
				IResourceChangeListener[] newListeners = new IResourceChangeListener[length];
				int[] newEventMasks = new int[length];
				System.arraycopy(this.preResourceChangeListeners, 0,
						newListeners, 0, i);
				System.arraycopy(this.preResourceChangeEventMasks, 0,
						newEventMasks, 0, i);

				// copy trailing listeners
				int trailingLength = this.preResourceChangeListenerCount - i
						- 1;
				if (trailingLength > 0) {
					System.arraycopy(this.preResourceChangeListeners, i + 1,
							newListeners, i, trailingLength);
					System.arraycopy(this.preResourceChangeEventMasks, i + 1,
							newEventMasks, i, trailingLength);
				}

				// update manager listener state (#fire need to iterate over
				// original listeners through a local variable to hold onto
				// the original ones)
				this.preResourceChangeListeners = newListeners;
				this.preResourceChangeEventMasks = newEventMasks;
				this.preResourceChangeListenerCount--;
				return;
			}
		}
	}

	public void resourceChanged(final IResourceChangeEvent event) {
		for (int i = 0; i < this.preResourceChangeListenerCount; i++) {
			// wrap callbacks with Safe runnable for subsequent listeners to be
			// called when some are causing grief
			final IResourceChangeListener listener = this.preResourceChangeListeners[i];
			if ((this.preResourceChangeEventMasks[i] & event.getType()) != 0)
				SafeRunner.run(new ISafeRunnable() {
					public void handleException(Throwable exception) {
						Util
								.log(exception,
										"Exception occurred in listener of pre script resource change notification"); //$NON-NLS-1$
					}

					public void run() throws Exception {
						listener.resourceChanged(event);
					}
				});
		}
		try {
			getDeltaProcessor().resourceChanged(event);
		} finally {
			// TODO (jerome) see 47631, may want to get rid of following so as
			// to reuse delta processor ?
			if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
				this.deltaProcessors.set(null);
			}
		}

	}

	public Map<IPath, Long> getExternalLibTimeStamps() {
		if (this.externalTimeStamps == null) {
			this.externalTimeStamps = new PersistentTimeStampMap(
					getTimeStampsFile());
		}
		return this.externalTimeStamps.getTimestamps();
	}

	public Map<IPath, Long> getCustomTimeStamps() {
		if (this.customTimeStamps == null) {
			this.customTimeStamps = new PersistentTimeStampMap(
					getCustomTimeStampsFile());
		}
		return this.customTimeStamps.getTimestamps();
	}

	public IScriptProject findProject(String name) {
		if (getOldScriptProjectNames().contains(name))
			return ModelManager.getModelManager().getModel().getScriptProject(
					name);
		return null;
	}

	/*
	 * Workaround for bug 15168 circular errors not reported Returns the list
	 * ofscriptprojects before resource delta processing has started.
	 */
	public synchronized HashSet<String> getOldScriptProjectNames() {
		if (this.scriptProjectNamesCache == null) {
			HashSet<String> result = new HashSet<String>();
			IScriptProject[] projects;
			try {
				projects = ModelManager.getModelManager().getModel()
						.getScriptProjects();
			} catch (ModelException e) {
				return this.scriptProjectNamesCache;
			}
			for (int i = 0, length = projects.length; i < length; i++) {
				IScriptProject project = projects[i];
				result.add(project.getElementName());
			}
			return this.scriptProjectNamesCache = result;
		}
		return this.scriptProjectNamesCache;
	}

	public synchronized void resetOldScriptProjectNames() {
		this.scriptProjectNamesCache = null;
	}

	private File getTimeStampsFile() {
		return DLTKCore.getDefault().getStateLocation().append(
				"externalLibsTimeStamps").toFile(); //$NON-NLS-1$
	}

	private File getCustomTimeStampsFile() {
		return DLTKCore.getDefault().getStateLocation().append(
				"customTimeStamps").toFile(); //$NON-NLS-1$
	}

	public void saveExternalLibTimeStamps() throws CoreException {
		if (this.externalTimeStamps != null) {
			this.externalTimeStamps.save();
		}
		if (this.customTimeStamps != null) {
			this.customTimeStamps.save();
		}
	}

	/*
	 * Update the roots that are affected by the addition or the removal of the
	 * given container resource.
	 */
	public synchronized void updateRoots(IPath containerPath,
			IResourceDelta containerDelta, DeltaProcessor deltaProcessor) {
		Map updatedRoots;
		Map otherUpdatedRoots;
		if (containerDelta.getKind() == IResourceDelta.REMOVED) {
			updatedRoots = this.oldRoots;
			otherUpdatedRoots = this.oldOtherRoots;
		} else {
			updatedRoots = this.roots;
			otherUpdatedRoots = this.otherRoots;
		}
		Iterator iterator = updatedRoots.keySet().iterator();
		while (iterator.hasNext()) {
			IPath path = (IPath) iterator.next();
			if (containerPath.isPrefixOf(path) && !containerPath.equals(path)) {
				IResourceDelta rootDelta = containerDelta.findMember(path
						.removeFirstSegments(1));
				if (rootDelta == null)
					continue;
				DeltaProcessor.RootInfo rootInfo = (DeltaProcessor.RootInfo) updatedRoots
						.get(path);

				if (!rootInfo.project.getPath().isPrefixOf(path)) { // only
					// consider
					// roots
					// that are
					// not
					// included
					// in the
					// container
					deltaProcessor.updateCurrentDeltaAndIndex(rootDelta,
							IModelElement.PROJECT_FRAGMENT, rootInfo);
				}

				ArrayList rootList = (ArrayList) otherUpdatedRoots.get(path);
				if (rootList != null) {
					Iterator otherProjects = rootList.iterator();
					while (otherProjects.hasNext()) {
						rootInfo = (DeltaProcessor.RootInfo) otherProjects
								.next();
						if (!rootInfo.project.getPath().isPrefixOf(path)) { // only
							// consider
							// roots
							// that
							// are
							// not
							// included
							// in
							// the
							// container
							deltaProcessor.updateCurrentDeltaAndIndex(
									rootDelta, IModelElement.PROJECT_FRAGMENT,
									rootInfo);
						}
					}
				}
			}
		}
	}

	public void updateProjectReferences(ScriptProject project,
			IBuildpathEntry[] oldResolvedPath, boolean canChangeResources)
			throws ModelException {
		if (canChangeResources) {
			new ProjectReferenceChange(project, oldResolvedPath)
					.updateProjectReferencesIfNecessary();
		} else {
			addProjectReferenceChange(project, oldResolvedPath);
		}
	}
}
