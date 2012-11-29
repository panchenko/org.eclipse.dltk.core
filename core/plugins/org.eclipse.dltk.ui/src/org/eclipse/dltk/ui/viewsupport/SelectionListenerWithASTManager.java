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
package org.eclipse.dltk.ui.viewsupport;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dltk.ast.parser.IModuleDeclaration;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.SourceParserUtil;
import org.eclipse.dltk.internal.ui.DLTKUIMessages;
import org.eclipse.dltk.internal.ui.editor.EditorUtility;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Infrastructure to share an AST for editor post selection listeners.
 */
public class SelectionListenerWithASTManager {

	private static SelectionListenerWithASTManager fgDefault;

	/**
	 * @return Returns the default manager instance.
	 */
	public static SelectionListenerWithASTManager getDefault() {
		if (fgDefault == null) {
			fgDefault = new SelectionListenerWithASTManager();
		}
		return fgDefault;
	}

	private final static class PartListenerGroup {
		protected final ITextEditor fPart;
		private ISelectionListener fPostSelectionListener;
		private ISelectionChangedListener fSelectionListener;
		private Job fCurrentJob;
		protected final ListenerList fAstListeners;
		/**
		 * Lock to avoid having more than one calculateAndInform job in
		 * parallel. Only jobs may synchronize on this as otherwise deadlocks
		 * are possible.
		 */
		protected final Object fJobLock = new Object();

		public PartListenerGroup(ITextEditor editorPart) {
			fPart = editorPart;
			fCurrentJob = null;
			fAstListeners = new ListenerList(ListenerList.IDENTITY);

			fSelectionListener = new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					ISelection selection = event.getSelection();
					if (selection instanceof ITextSelection) {
						fireSelectionChanged((ITextSelection) selection);
					}
				}
			};

			fPostSelectionListener = new ISelectionListener() {
				public void selectionChanged(IWorkbenchPart part,
						ISelection selection) {
					if (part == fPart && selection instanceof ITextSelection)
						firePostSelectionChanged((ITextSelection) selection);
				}
			};
		}

		public boolean isEmpty() {
			return fAstListeners.isEmpty();
		}

		public void install(ISelectionListenerWithAST listener) {
			if (isEmpty()) {
				fPart.getEditorSite().getPage()
						.addPostSelectionListener(fPostSelectionListener);
				ISelectionProvider selectionProvider = fPart
						.getSelectionProvider();
				if (selectionProvider != null)
					selectionProvider
							.addSelectionChangedListener(fSelectionListener);
			}
			fAstListeners.add(listener);
		}

		public void uninstall(ISelectionListenerWithAST listener) {
			fAstListeners.remove(listener);
			if (isEmpty()) {
				fPart.getEditorSite().getPage()
						.removePostSelectionListener(fPostSelectionListener);
				ISelectionProvider selectionProvider = fPart
						.getSelectionProvider();
				if (selectionProvider != null)
					selectionProvider
							.removeSelectionChangedListener(fSelectionListener);
			}
		}

		/**
		 * A selection event has occurred.
		 * 
		 * @param selection
		 *            the selection
		 */
		public void fireSelectionChanged(final ITextSelection selection) {
			if (fCurrentJob != null) {
				fCurrentJob.cancel();
			}
		}

		/**
		 * A post selection event has occurred.
		 * 
		 * @param selection
		 *            the selection
		 */
		public void firePostSelectionChanged(final ITextSelection selection) {
			cancelJob();
			IModelElement input = EditorUtility.getEditorInputModelElement(
					fPart, false);
			if (!(input instanceof ISourceModule)) {
				return;
			}
			fCurrentJob = new ASTJob(this, (ISourceModule) input, selection);
			fCurrentJob.schedule();
		}

		void cancelJob() {
			if (fCurrentJob != null) {
				fCurrentJob.cancel();
				fCurrentJob = null;
			}
		}

	}

	protected static class ASTJob extends Job {

		private final PartListenerGroup owner;
		private final ISourceModule input;
		private final ITextSelection selection;

		public ASTJob(PartListenerGroup owner, ISourceModule module,
				ITextSelection selection) {
			super(DLTKUIMessages.SelectionListenerWithASTManager_job_title);
			this.owner = owner;
			this.input = module;
			this.selection = selection;
			setPriority(Job.DECORATE);
			setSystem(true);
		}

		@Override
		protected void canceling() {
			synchronized (this) {
				wasCancel = true;
			}
		}

		private boolean wasCancel;

		@Override
		public IStatus run(IProgressMonitor monitor) {
			try {
				if (!input.isConsistent()) {
					synchronized (this) {
						if (!wasCancel) {
							schedule(1000);
						}
					}
					return Status.OK_STATUS;
				}
			} catch (ModelException e) {
				// never happens, fall thru
			}
			synchronized (owner.fJobLock) {
				// The monitor is never null
				return calculateASTandInform(monitor);
			}
		}

		protected final IStatus calculateASTandInform(IProgressMonitor monitor) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			// create AST
			try {
				IModuleDeclaration astRoot = SourceParserUtil
						.parse(input, null);

				if (astRoot != null && !monitor.isCanceled()) {
					Object[] listeners = owner.fAstListeners.getListeners();
					for (int i = 0; i < listeners.length; i++) {
						((ISelectionListenerWithAST) listeners[i])
								.selectionChanged(owner.fPart, selection,
										input, astRoot);
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
					}
					return Status.OK_STATUS;
				}
			} catch (OperationCanceledException e) {
				// thrown when canceling the AST creation
			}
			return Status.CANCEL_STATUS;
		}
	}

	private Map<ITextEditor, PartListenerGroup> fListenerGroups;

	private SelectionListenerWithASTManager() {
		fListenerGroups = new HashMap<ITextEditor, PartListenerGroup>();
	}

	/**
	 * Registers a selection listener for the given editor part.
	 * 
	 * @param part
	 *            The editor part to listen to.
	 * @param listener
	 *            The listener to register.
	 */
	public void addListener(ITextEditor part, ISelectionListenerWithAST listener) {
		synchronized (this) {
			PartListenerGroup partListener = fListenerGroups.get(part);
			if (partListener == null) {
				partListener = new PartListenerGroup(part);
				fListenerGroups.put(part, partListener);
			}
			partListener.install(listener);
		}
	}

	/**
	 * Unregisters a selection listener.
	 * 
	 * @param part
	 *            The editor part the listener was registered.
	 * @param listener
	 *            The listener to unregister.
	 */
	public void removeListener(ITextEditor part,
			ISelectionListenerWithAST listener) {
		synchronized (this) {
			PartListenerGroup partListener = fListenerGroups.get(part);
			if (partListener != null) {
				partListener.uninstall(listener);
				if (partListener.isEmpty()) {
					partListener.cancelJob();
					fListenerGroups.remove(part);
				}
			}
		}
	}
}
