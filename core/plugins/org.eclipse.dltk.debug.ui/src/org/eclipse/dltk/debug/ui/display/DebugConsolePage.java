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
package org.eclipse.dltk.debug.ui.display;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.dltk.console.ScriptConsoleConstants;
import org.eclipse.dltk.console.ui.ScriptConsole;
import org.eclipse.dltk.console.ui.internal.ScriptConsolePage;
import org.eclipse.dltk.debug.core.model.IScriptStackFrame;
import org.eclipse.dltk.debug.core.model.IScriptThread;
import org.eclipse.dltk.debug.ui.DLTKDebugUIPlugin;
import org.eclipse.dltk.internal.debug.ui.ScriptEvaluationContextManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextDropTargetEffect;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.texteditor.IUpdate;

public class DebugConsolePage extends ScriptConsolePage {

	private DebugEventListener debugEventListener = null;

	/**
	 * @param console
	 * @param view
	 * @param cfg
	 */
	public DebugConsolePage(ScriptConsole console, IConsoleView view,
			SourceViewerConfiguration cfg) {
		super(console, view, cfg);
	}

	protected IAction createTerminateConsoleAction() {
		return null;
	}

	private IAction runAction;
	private IAction resetOnLaunchAction;

	private boolean resetOnLaunch;
	private IHandlerActivation pasteHandler;
	private IHandlerActivation copyHandler;
	private IHandlerActivation cutHandler;
	private IHandlerActivation selectAllHandler;

	protected void createActions() {
		super.createActions();
		final IActionBars actionBars = getSite().getActionBars();
		final IToolBarManager tbManager = actionBars.getToolBarManager();
		tbManager.appendToGroup(ScriptConsoleConstants.SCRIPT_GROUP,
				new OpenInputFieldAction(this));
		runAction = new RunInputFieldAction(this);
		tbManager.appendToGroup(ScriptConsoleConstants.SCRIPT_GROUP, runAction);
		resetOnLaunchAction = new ResetOnLaunchAction(this);
		resetOnLaunchAction.setChecked(resetOnLaunch);
		actionBars.getMenuManager().add(resetOnLaunchAction);
		updateActions();
	}

	private SashForm sash;
	private StyledText inputField;
	private boolean enabled = true;

	/**
	 * @param value
	 */
	private void setEnabled(final boolean value) {
		if (value != this.enabled) {
			this.enabled = value;
			if (inputField != null)
				inputField.setEditable(value);
			getViewer().setEditable(value);
			final Control control = getViewer().getControl();
			control.setBackground(value ? null : control.getDisplay()
					.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		}
	}

	@Override
	public Control getControl() {
		return sash != null ? sash : super.getControl();
	}

	/*
	 * @see TextConsolePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		sash = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);
		inputField = new StyledText(sash, SWT.V_SCROLL | SWT.H_SCROLL);
		inputField.addFocusListener(new FocusListener() {

			public void focusLost(FocusEvent e) {
				if (pasteHandler != null) {
					IHandlerService service = (IHandlerService) getSite()
							.getService(IHandlerService.class);
					service.deactivateHandler(pasteHandler);
					service.deactivateHandler(copyHandler);
					service.deactivateHandler(cutHandler);
					service.deactivateHandler(selectAllHandler);
					pasteHandler = null;
					copyHandler = null;
					cutHandler = null;
					selectAllHandler = null;
				}
			}

			public void focusGained(FocusEvent e) {
				IHandlerService service = (IHandlerService) getSite()
						.getService(IHandlerService.class);
				Expression expression = new Expression() {
					public final EvaluationResult evaluate(
							final IEvaluationContext context) {
						return EvaluationResult.TRUE;
					}

					public final void collectExpressionInfo(
							final ExpressionInfo info) {
						info.addVariableNameAccess(ISources.ACTIVE_EDITOR_NAME);
						info.addVariableNameAccess(ISources.ACTIVE_CURRENT_SELECTION_NAME);
					}
				};
				pasteHandler = service.activateHandler(
						IWorkbenchCommandConstants.EDIT_PASTE,
						new ActionHandler(new PasteAction(inputField)),
						expression);
				copyHandler = service.activateHandler(
						IWorkbenchCommandConstants.EDIT_COPY,
						new ActionHandler(new CopyAction(inputField)),
						expression);
				cutHandler = service.activateHandler(
						IWorkbenchCommandConstants.EDIT_CUT, new ActionHandler(
								new CutAction(inputField)), expression);
				selectAllHandler = service.activateHandler(
						IWorkbenchCommandConstants.EDIT_SELECT_ALL,
						new ActionHandler(new SelectAllAction(inputField)),
						expression);

			}
		});
		DropTarget target = new DropTarget(inputField, DND.DROP_DEFAULT
				| DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
		target.setTransfer(new Transfer[] { TextTransfer.getInstance() });
		target.addDropListener(new StyledTextDropTargetEffect(inputField) {
			public void dragEnter(DropTargetEvent e) {
				super.dragEnter(e);
				if (e.detail == DND.DROP_DEFAULT)
					e.detail = DND.DROP_COPY;
			}

			public void dragOperationChanged(DropTargetEvent e) {
				super.dragOperationChanged(e);
				if (e.detail == DND.DROP_DEFAULT)
					e.detail = DND.DROP_COPY;
			}

			public void drop(DropTargetEvent e) {
				super.drop(e);
				Point selection = inputField.getSelectionRange();
				inputField.replaceTextRange(selection.x, selection.y,
						(String) e.data);
			}
		});
		inputField.setEditable(true);
		super.createControl(sash);
		inputField.setFont(getViewer().getControl().getFont());
		inputField.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				updateActions();
			}

		});
		sash.setMaximizedControl(getViewer().getControl());
		setEnabled(isDebuggerAvailable());
		if (debugEventListener == null) {
			debugEventListener = new DebugEventListener();
			DebugPlugin.getDefault().addDebugEventListener(debugEventListener);
		}
		enableUpdateJob.schedule(500);
	}

	private boolean isDebuggerAvailable() {
		final IPageSite site = getSite();
		if (site == null) {
			return false;
		}
		final IWorkbenchPage page = site.getPage();
		if (page == null) {
			return false;
		}
		final IWorkbenchPart part = page.getActivePart();
		if (part == null) {
			return false;
		}
		final IScriptStackFrame frame = ScriptEvaluationContextManager
				.getEvaluationContext(part);
		if (frame != null) {
			final IScriptThread thread = frame.getScriptThread();
			if (thread != null) {
				return thread.isSuspended();
			}
		}
		return false;
	}

	/**
	 * @see org.eclipse.dltk.console.ui.internal.ScriptConsolePage#dispose()
	 */
	public void dispose() {
		if (debugEventListener != null) {
			DebugPlugin.getDefault().removeDebugEventListener(
					debugEventListener);
			debugEventListener = null;
		}
		if (pasteHandler != null) {
			IHandlerService service = (IHandlerService) getSite().getService(
					IHandlerService.class);
			service.deactivateHandler(pasteHandler);
			service.deactivateHandler(copyHandler);
			service.deactivateHandler(cutHandler);
			service.deactivateHandler(selectAllHandler);
		}
		super.dispose();
	}

	public boolean canExecuteInputField() {
		return sash != null && sash.getMaximizedControl() == null
				&& inputField.getText().length() != 0;
	}

	public void openInputField() {
		if (sash != null) {
			sash.setWeights(new int[] { 30, 70 });
			sash.setMaximizedControl(null);
			inputField.setFocus();
		}
		updateActions();
	}

	public void closeInputField() {
		if (sash != null) {
			final Control consoleControl = getViewer().getControl();
			sash.setMaximizedControl(consoleControl);
			consoleControl.setFocus();
		}
		updateActions();
	}

	private void updateActions() {
		if (runAction instanceof IUpdate) {
			((IUpdate) runAction).update();
		}
	}

	public void executeInputField() {
		if (inputField != null) {
			final String input = inputField.getText();
			((ScriptConsole) getConsole()).executeCommand(input);
		}
	}

	private final Job enableUpdateJob = new Job("Enable update") { //$NON-NLS-1$

		protected IStatus run(IProgressMonitor monitor) {
			DLTKDebugUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
				public void run() {
					setEnabled(isDebuggerAvailable());
				}
			});
			return Status.OK_STATUS;
		}

	};

	private final class DebugEventListener implements IDebugEventSetListener {
		public void handleDebugEvents(DebugEvent[] events) {
			enableUpdateJob.schedule(500);
			if (resetOnLaunch && isTargetCreate(events)) {
				DLTKDebugUIPlugin.getStandardDisplay().asyncExec(
						new Runnable() {
							public void run() {
								((DebugConsole) getConsole()).clearConsole();
							}
						});
			}
		}
	}

	private static boolean isTargetCreate(DebugEvent[] events) {
		for (int i = 0; i < events.length; ++i) {
			final DebugEvent event = events[i];
			if (event.getKind() == DebugEvent.CREATE
					&& event.getSource() instanceof IDebugTarget) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the resetOnLaunch
	 */
	public boolean isResetOnLaunch() {
		return resetOnLaunch;
	}

	/**
	 * @param resetOnLaunch
	 *            the resetOnLaunch to set
	 */
	public void setResetOnLaunch(boolean resetOnLaunch) {
		this.resetOnLaunch = resetOnLaunch;
	}

	private static class PasteAction extends Action {
		private StyledText text;

		public PasteAction(StyledText text) {
			this.text = text;
		}

		@Override
		public void run() {
			text.paste();
		}
	}

	private static class CopyAction extends Action {
		private StyledText text;

		public CopyAction(StyledText text) {
			this.text = text;
		}

		@Override
		public void run() {
			text.copy();
		}
	}

	private static class CutAction extends Action {
		private StyledText text;

		public CutAction(StyledText text) {
			this.text = text;
		}

		@Override
		public void run() {
			text.cut();
		}
	}

	private static class SelectAllAction extends Action {
		private StyledText text;

		public SelectAllAction(StyledText text) {
			this.text = text;
		}

		@Override
		public void run() {
			text.selectAll();
		}
	}

}
