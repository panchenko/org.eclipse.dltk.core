/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.internal.core.builder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.dltk.compiler.problem.DefaultProblemFactory;
import org.eclipse.dltk.compiler.problem.IProblemFactory;
import org.eclipse.dltk.compiler.problem.IProblemReporter;
import org.eclipse.dltk.compiler.problem.IProblemSeverityTranslator;
import org.eclipse.dltk.compiler.problem.ProblemCategory;
import org.eclipse.dltk.compiler.util.Util;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.builder.IBuildChange;
import org.eclipse.dltk.core.builder.IBuildContext;
import org.eclipse.dltk.core.builder.IBuildParticipant;
import org.eclipse.dltk.core.builder.IBuildParticipantExtension;
import org.eclipse.dltk.core.builder.IBuildParticipantExtension2;
import org.eclipse.dltk.core.builder.IBuildParticipantExtension3;
import org.eclipse.dltk.core.builder.IBuildParticipantExtension4;
import org.eclipse.dltk.core.builder.IBuildParticipantFilter;
import org.eclipse.dltk.core.builder.IBuildState;
import org.eclipse.dltk.core.builder.IProjectChange;
import org.eclipse.dltk.core.builder.IScriptBuilder;
import org.eclipse.dltk.internal.core.builder.BuildParticipantManager.BuildParticipantResult;
import org.eclipse.osgi.util.NLS;

public class StandardScriptBuilder implements IScriptBuilder {
	private static final boolean DEBUG = false;

	private static final int WORK_BUILD = 100;

	public void prepare(IBuildChange change, IBuildState state,
			IProgressMonitor monitor) throws CoreException {
		if (participants != null) {
			for (int i = 0; i < participants.length; ++i) {
				final IBuildParticipant participant = participants[i];
				if (participant instanceof IBuildParticipantExtension2) {
					((IBuildParticipantExtension2) participant).prepare(change,
							state);
				}
			}
		}
	}

	public void build(IBuildChange change, IBuildState state,
			IProgressMonitor monitor) throws CoreException {
		// TODO progress reporting
		buildExternalElements(change, monitor);
		if (toolkit != null) {
			buildNatureModules(change.getScriptProject(),
					change.getBuildType(),
					change.getSourceModules(IProjectChange.DEFAULT), state,
					SubMonitor.convert(monitor, WORK_BUILD));
		}
		final List<IFile> resourceChanges = change
				.getResources(IProjectChange.DEFAULT);
		if (!resourceChanges.isEmpty()) {
			buildResources(resourceChanges, new SubProgressMonitor(monitor, 10));
		}
	}

	private void buildExternalElements(IBuildChange change,
			IProgressMonitor monitor) throws CoreException {
		final int buildType = change.getBuildType();
		beginBuild(buildType, monitor);
		final List<IBuildParticipantExtension2> extensions = selectExtension(IBuildParticipantExtension2.class);

		if (extensions != null) {
			final List<ISourceModule> externalElements = change
					.getExternalModules(IProjectChange.DEFAULT);
			int remainingWork = externalElements.size();
			for (final ISourceModule module : externalElements) {
				if (monitor.isCanceled())
					return;
				monitor.subTask(NLS.bind(
						Messages.ValidatorBuilder_buildExternalModuleSubTask,
						String.valueOf(remainingWork), module.getElementName()));
				final ExternalModuleBuildContext context = new ExternalModuleBuildContext(
						module, buildType);
				try {
					for (int i = 0; i < extensions.size(); ++i) {
						if (monitor.isCanceled()) {
							return;
						}
						extensions.get(i).buildExternalModule(context);
					}
				} catch (CoreException e) {
					DLTKCore.error(
							NLS.bind(
									Messages.StandardScriptBuilder_errorBuildingExternalModule,
									module.getElementName()), e);
				}
				--remainingWork;
			}
		}
	}

	/**
	 * @return
	 */
	private <T> List<T> selectExtension(Class<T> clazz) {
		if (participants != null) {
			int count = 0;
			for (int i = 0; i < participants.length; ++i) {
				final IBuildParticipant participant = participants[i];
				if (clazz.isInstance(participant)) {
					++count;
				}
			}
			if (count != 0) {
				final List<T> result = new ArrayList<T>(count);
				for (int i = 0; i < participants.length; ++i) {
					final IBuildParticipant participant = participants[i];
					if (clazz.isInstance(participant)) {
						@SuppressWarnings("unchecked")
						final T casted = (T) participant;
						result.add(casted);
					}
				}
				return result;
			}
		}
		return null;
	}

	private List<IProblemReporter> reporters = null;

	private void buildNatureModules(IScriptProject project, int buildType,
			final List<ISourceModule> modules, IBuildState state,
			IProgressMonitor monitor) {
		final long startTime = DEBUG ? System.currentTimeMillis() : 0;
		beginBuild(buildType, monitor);
		monitor.beginTask(Messages.ValidatorBuilder_buildingModules,
				modules.size());
		if (participants.length == 0) {
			return;
		}
		int counter = 0;
		if (reporters == null) {
			reporters = new ArrayList<IProblemReporter>(modules.size());
		}
		for (Iterator<ISourceModule> j = modules.iterator(); j.hasNext();) {
			if (monitor.isCanceled())
				return;
			final ISourceModule module = j.next();
			monitor.subTask(NLS.bind(
					Messages.ValidatorBuilder_buildModuleSubTask,
					String.valueOf(modules.size() - counter),
					module.getElementName()));
			final SourceModuleBuildContext context = new SourceModuleBuildContext(
					problemFactory, module, buildType, state);
			if (context.reporter != null) {
				buildModule(context);
				reporters.add(context.reporter);
			}
			monitor.worked(1);
			++counter;
		}
		monitor.done();
		if (DEBUG) {
			System.out.println("Build " + project.getElementName() + "(" //$NON-NLS-1$ //$NON-NLS-2$
					+ modules.size() + ") in " //$NON-NLS-1$
					+ (System.currentTimeMillis() - startTime) + "ms"); //$NON-NLS-1$
		}
	}

	/**
	 * Calls {@link IBuildParticipantExtension#beginBuild(int)} for all
	 * {@link #participants}. Returns <code>true</code> if it was called for
	 * some of them so it is required to call
	 * {@link IBuildParticipantExtension#endBuild()}. If called multiple times
	 * only first time actual work are performed, so subsequent calls returns
	 * result of the first call.
	 * 
	 * @param buildType
	 * @param monitor
	 * @return
	 */
	private void beginBuild(int buildType, IProgressMonitor monitor) {
		if (!beginBuildDone) {
			monitor.subTask(Messages.ValidatorBuilder_InitializeBuilders);
			endBuildNeeded = false;
			int count = 0;
			for (int j = 0; j < participants.length; ++j) {
				final IBuildParticipant participant = participants[j];
				final boolean useParticipant;
				if (participant instanceof IBuildParticipantExtension) {
					useParticipant = ((IBuildParticipantExtension) participant)
							.beginBuild(buildType);
					endBuildNeeded = true;
				} else {
					useParticipant = true;
				}
				if (useParticipant) {
					if (count < j) {
						participants[count] = participants[j];
					}
					++count;
				}
			}
			participants = BuildParticipantManager.copyFirst(participants,
					count);
			BuildParticipantManager.notifyDependents(participants,
					participantDependencies);
			beginBuildDone = true;
		}
	}

	private void buildModule(IBuildContext context) {
		IBuildParticipant[] selected = participants;
		for (IBuildParticipantFilter filter : filters) {
			selected = filter.filter(selected, context);
			if (selected == null || selected.length == 0) {
				return;
			}
		}
		try {
			for (int k = 0; k < selected.length; ++k) {
				selected[k].build(context);
			}
		} catch (CoreException e) {
			DLTKCore.error(Messages.StandardScriptBuilder_errorBuildingModule,
					e);
		} finally {
			for (IBuildParticipant participant : participants) {
				if (participant instanceof IBuildParticipantExtension4) {
					((IBuildParticipantExtension4) participant)
							.afterBuild(context);
				}
			}
		}
	}

	protected IStatus buildResources(List<IFile> resources,
			IProgressMonitor monitor) {
		try {
			monitor.beginTask(Util.EMPTY_STRING, resources.size());
			try {
				for (final IFile resource : resources) {
					monitor.subTask(NLS.bind(
							Messages.ValidatorBuilder_clearingResourceMarkers,
							resource.getName()));
					problemFactory.deleteMarkers(resource);
					monitor.worked(1);
				}
			} catch (CoreException e) {
				final String msg = Messages.ValidatorBuilder_errorDeleteResourceMarkers;
				DLTKCore.error(msg, e);
			}
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	public void clean(IScriptProject project, IProgressMonitor monitor) {
		final List<IBuildParticipantExtension3> extensions = selectExtension(IBuildParticipantExtension3.class);
		if (extensions != null) {
			for (IBuildParticipantExtension3 extension : extensions) {
				extension.clean();
			}
		}
		final IProject p = project.getProject();
		try {
			problemFactory.deleteMarkers(p);
		} catch (CoreException e) {
			DLTKCore.error(
					NLS.bind(Messages.StandardScriptBuilder_errorCleaning,
							p.getName()), e);
		}
	}

	private boolean beginBuildDone = false;
	private boolean endBuildNeeded = false;
	private IBuildParticipant[] participants = null;
	private Map<IBuildParticipant, List<IBuildParticipant>> participantDependencies = null;
	private IBuildParticipantFilter[] filters = null;
	private IDLTKLanguageToolkit toolkit = null;
	private IProblemFactory problemFactory = null;

	protected IDLTKLanguageToolkit getLanguageToolkit() {
		return toolkit;
	}

	public boolean initialize(IScriptProject project) {
		toolkit = project.getLanguageToolkit();
		if (toolkit != null) {
			final BuildParticipantResult result = BuildParticipantManager
					.getBuildParticipants(project, toolkit.getNatureId());
			participants = result.participants;
			participantDependencies = result.dependencies;
		}
		if (participants == null || participants.length == 0) {
			return false;
		}
		filters = BuildParticipantManager.getFilters(project,
				toolkit.getNatureId(), this);
		problemFactory = createProblemFactory();
		beginBuildDone = false;
		endBuildNeeded = false;
		return true;
	}

	protected IProblemFactory createProblemFactory() {
		if (toolkit != null) {
			return DLTKLanguageManager.getProblemFactory(toolkit.getNatureId());
		} else {
			return new DefaultProblemFactory();
		}
	}

	public void endBuild(IScriptProject project, IBuildState state,
			IProgressMonitor monitor) {
		if (endBuildNeeded) {
			monitor.subTask(Messages.ValidatorBuilder_finalizeBuild);
			final IProgressMonitor finalizeMonitor = new SubTaskProgressMonitor(
					monitor, Messages.ValidatorBuilder_finalizeBuild);
			for (int j = 0; j < participants.length; ++j) {
				final IBuildParticipant participant = participants[j];
				if (participant instanceof IBuildParticipantExtension) {
					((IBuildParticipantExtension) participant)
							.endBuild(finalizeMonitor);
				}
			}
			endBuildNeeded = false;
		}
		if (reporters != null) {
			final IProblemSeverityTranslator severityTranslator = problemFactory
					.createSeverityTranslator(project);
			for (IProblemReporter reporter : reporters) {
				final BuildProblemReporter buildReporter = (BuildProblemReporter) reporter;
				if (buildReporter.hasCategory(ProblemCategory.IMPORT)) {
					state.recordImportProblem(buildReporter.resource
							.getFullPath());
				}
				buildReporter.flush(severityTranslator);
			}
			reporters = null;
		}
		participants = null;
		participantDependencies = null;
		filters = null;
		toolkit = null;
		problemFactory = null;
		beginBuildDone = false;
	}

}
