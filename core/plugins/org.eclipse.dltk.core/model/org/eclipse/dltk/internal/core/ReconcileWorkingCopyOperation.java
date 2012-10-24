/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.internal.core;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.dltk.ast.parser.IModuleDeclaration;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IModelStatus;
import org.eclipse.dltk.core.IModelStatusConstants;
import org.eclipse.dltk.core.IProblemRequestor;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.SourceParserUtil;
import org.eclipse.dltk.core.WorkingCopyOwner;
import org.eclipse.dltk.internal.core.search.ProjectIndexerManager;
import org.eclipse.dltk.internal.core.util.Messages;

public class ReconcileWorkingCopyOperation extends ModelOperation {
	private boolean forceProblemDetection;

	private WorkingCopyOwner workingCopyOwner;

	private ModelElementDeltaBuilder deltaBuilder;

	public ReconcileWorkingCopyOperation(ISourceModule module,
			boolean forceProblemDetection, WorkingCopyOwner workingCopyOwner) {
		super(new IModelElement[] { module });
		this.forceProblemDetection = forceProblemDetection;
		this.workingCopyOwner = workingCopyOwner;
	}

	@Override
	protected void executeOperation() throws ModelException {
		if (this.progressMonitor != null) {
			if (this.progressMonitor.isCanceled())
				throw new OperationCanceledException();
			this.progressMonitor.beginTask(Messages.element_reconciling, 2);
		}
		try {
			SourceModule workingCopy = getWorkingCopy();
			// boolean wasConsistent = workingCopy.isConsistent();
			IProblemRequestor requestor = workingCopy.getPerWorkingCopyInfo();

			// create the delta builder (this remembers the current content of
			// the cu)
			this.deltaBuilder = new ModelElementDeltaBuilder(workingCopy);

			// make working copy consistent if needed and compute AST if needed
			makeConsistent(workingCopy, requestor);
			// report delta
			ModelElementDelta delta = this.deltaBuilder.delta;
			if (delta != null) {
				addReconcileDelta(workingCopy, delta);
			}
		} finally {
			if (this.progressMonitor != null)
				this.progressMonitor.done();
		}
	}

	/*
	 * Makes the given working copy consistent, computes the delta and computes
	 * an AST if needed. Returns the AST.
	 */
	public void makeConsistent(SourceModule workingCopy,
			IProblemRequestor problemRequestor) throws ModelException {
		if (!workingCopy.isConsistent()) {
			// make working copy consistent
			workingCopy.makeConsistent(this.progressMonitor);
			this.deltaBuilder.buildDeltas();
			ProjectIndexerManager.reconciled(workingCopy);
		} else if (forceProblemDetection && problemRequestor.isActive()) {
			AccumulatingProblemReporter reporter = new AccumulatingProblemReporter(
					workingCopy, problemRequestor);
			final IModuleDeclaration moduleDeclaration = SourceParserUtil
					.parse(workingCopy, reporter);
			// TODO put it to the context
			final IDLTKLanguageToolkit toolkit = DLTKLanguageManager
					.getLanguageToolkit(workingCopy);
			if (toolkit != null) {
				new ReconcileBuilder(toolkit.getNatureId(), workingCopy,
						reporter).build();
			}
			reporter.reportToRequestor();
		}
	}

	/**
	 * Returns the working copy this operation is working on.
	 */
	protected SourceModule getWorkingCopy() {
		return (SourceModule) getElementToProcess();
	}

	/**
	 * @see ModelOperation#isReadOnly
	 */
	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	protected IModelStatus verify() {
		IModelStatus status = super.verify();
		if (!status.isOK()) {
			return status;
		}
		SourceModule workingCopy = getWorkingCopy();
		if (!workingCopy.isWorkingCopy()) {
			return new ModelStatus(
					IModelStatusConstants.ELEMENT_DOES_NOT_EXIST, workingCopy); // was
			// destroyed
		}
		return status;
	}
}
