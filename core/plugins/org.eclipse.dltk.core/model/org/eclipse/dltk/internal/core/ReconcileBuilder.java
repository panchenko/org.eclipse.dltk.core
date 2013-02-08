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
package org.eclipse.dltk.internal.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.dltk.compiler.problem.IProblemReporter;
import org.eclipse.dltk.compiler.task.ITaskReporter;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.builder.IBuildContext;
import org.eclipse.dltk.core.builder.IBuildParticipant;
import org.eclipse.dltk.core.builder.IBuildParticipantExtension;
import org.eclipse.dltk.core.builder.IBuildParticipantExtension4;
import org.eclipse.dltk.core.builder.IBuildParticipantFilter;
import org.eclipse.dltk.core.environment.IFileHandle;
import org.eclipse.dltk.internal.core.builder.AbstractBuildContext;
import org.eclipse.dltk.internal.core.builder.BuildParticipantManager;
import org.eclipse.dltk.internal.core.builder.BuildParticipantManager.BuildParticipantResult;

class ReconcileBuilder implements ISafeRunnable {

	private static class ReconcileBuildContext extends AbstractBuildContext {

		final AccumulatingProblemReporter reporter;

		/**
		 * @param module
		 */
		protected ReconcileBuildContext(ISourceModule module,
				AccumulatingProblemReporter reporter) {
			super(module, IBuildContext.RECONCILE_BUILD);
			this.reporter = reporter;
		}

		/*
		 * @see org.eclipse.dltk.core.builder.IBuildContext#getFileHandle()
		 */
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
			// NOP
		}
	}

	private final String natureId;
	private final ISourceModule module;
	private final AccumulatingProblemReporter reporter;

	public ReconcileBuilder(String natureId, ISourceModule module,
			AccumulatingProblemReporter reporter) {
		this.natureId = natureId;
		this.module = module;
		this.reporter = reporter;
	}

	void build() {
		SafeRunner.run(this);
	}

	public void run() {
		final NullProgressMonitor monitor = new NullProgressMonitor();
		final IScriptProject project = module.getScriptProject();
		final ReconcileBuildContext context = new ReconcileBuildContext(module,
				reporter);
		IBuildParticipant[] participants = beginBuild(natureId, project);
		if (participants.length == 0) {
			return;
		}
		final IBuildParticipantFilter[] filters = BuildParticipantManager
				.getFilters(project, natureId, reporter);
		for (IBuildParticipantFilter filter : filters) {
			participants = filter.filter(participants, context);
			if (participants == null || participants.length == 0) {
				return;
			}
		}
		try {
			for (IBuildParticipant participant : participants) {
				participant.build(context);
			}
		} catch (CoreException e) {
			DLTKCore.error("error", e); //$NON-NLS-1$
		} finally {
			for (final IBuildParticipant participant : participants) {
				if (participant instanceof IBuildParticipantExtension4) {
					((IBuildParticipantExtension4) participant)
							.afterBuild(context);
				}
			}
			for (final IBuildParticipant participant : participants) {
				if (participant instanceof IBuildParticipantExtension) {
					((IBuildParticipantExtension) participant)
							.endBuild(monitor);
				}
			}
		}
	}

	private static IBuildParticipant[] beginBuild(String natureId,
			final IScriptProject project) {
		final BuildParticipantResult result = BuildParticipantManager
				.getBuildParticipants(project, natureId);
		IBuildParticipant[] participants = result.participants;
		int count = 0;
		for (int j = 0; j < participants.length; ++j) {
			final IBuildParticipant participant = participants[j];
			final boolean useParticipant;
			if (participant instanceof IBuildParticipantExtension) {
				useParticipant = ((IBuildParticipantExtension) participant)
						.beginBuild(IBuildContext.RECONCILE_BUILD);
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
		participants = BuildParticipantManager.copyFirst(participants, count);
		BuildParticipantManager.notifyDependents(participants,
				result.dependencies);
		return participants;
	}

	public void handleException(Throwable exception) {
		// Logged by SafeRunner
	}

}
