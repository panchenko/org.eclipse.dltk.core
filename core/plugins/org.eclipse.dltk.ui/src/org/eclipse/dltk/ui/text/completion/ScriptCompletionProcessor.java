/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.ui.text.completion;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.ui.DLTKUILanguageManager;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.dltk.ui.IDLTKUILanguageToolkit;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.IEditorPart;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Script completion processor.
 */
public abstract class ScriptCompletionProcessor extends ContentAssistProcessor {

	private IContextInformationValidator fValidator;

	protected final IEditorPart fEditor;

	public ScriptCompletionProcessor(IEditorPart editor,
			ContentAssistant assistant, String partition) {
		super(assistant, partition);
		fEditor = editor;
	}

	/**
	 * Tells this processor to restrict its proposal to those element visible in
	 * the actual invocation context.
	 * 
	 * @param restrict
	 *            <code>true</code> if proposals should be restricted
	 */
	public void restrictProposalsToVisibility(boolean restrict) {
		final IDLTKLanguageToolkit toolkit = DLTKLanguageManager
				.getLanguageToolkit(getNatureId());
		if (toolkit == null) {
			return;
		}
		final String preferenceQualifier = toolkit.getPreferenceQualifier();
		if (preferenceQualifier == null) {
			return;
		}
		final IEclipsePreferences node = new InstanceScope()
				.getNode(preferenceQualifier);
		if (node == null) {
			return;
		}
		final String value = node.get(DLTKCore.CODEASSIST_VISIBILITY_CHECK,
				null);
		final String newValue = restrict ? DLTKCore.ENABLED : DLTKCore.DISABLED;
		if (!newValue.equals(value)) {
			node.put(DLTKCore.CODEASSIST_VISIBILITY_CHECK, newValue);
			try {
				node.flush();
			} catch (BackingStoreException e) {
				DLTKUIPlugin.log(e);
			}
		}
	}

	/**
	 * Tells this processor to restrict is proposals to those starting with
	 * matching cases.
	 * 
	 * @param restrict
	 *            <code>true</code> if proposals should be restricted
	 */
	public void restrictProposalsToMatchingCases(boolean restrict) {
		// not yet supported
	}

	/*
	 * @seeorg.eclipse.jface.text.contentassist.IContentAssistProcessor#
	 * getContextInformationValidator()
	 */
	// should be final? breaks api?
	@Override
	public IContextInformationValidator getContextInformationValidator() {
		if (fValidator == null) {
			fValidator = createContextInformationValidator();
		}
		return fValidator;
	}

	/**
	 * 
	 */
	protected IContextInformationValidator createContextInformationValidator() {
		return new ScriptParameterListValidator();
	}

	@Override
	protected List<ICompletionProposal> filterAndSortProposals(
			List<ICompletionProposal> proposals, IProgressMonitor monitor,
			ContentAssistInvocationContext context) {
		ProposalSorterRegistry.getDefault().getCurrentSorter()
				.sortProposals(context, proposals);
		return proposals;
	}

	protected abstract String getNatureId();

	@Deprecated
	protected final CompletionProposalLabelProvider getProposalLabelProvider() {
		return null;
	}

	@Override
	protected final IPreferenceStore getPreferenceStore() {
		final IDLTKUILanguageToolkit toolkit = DLTKUILanguageManager
				.getLanguageToolkit(getNatureId());
		if (toolkit != null) {
			return toolkit.getPreferenceStore();
		}
		return DLTKUIPlugin.getDefault().getPreferenceStore();
	}

	@Override
	protected void setContextInformationMode(
			ContentAssistInvocationContext context) {
		((ScriptContentAssistInvocationContext) context)
				.setContextInformationMode(true);
	}

	@Override
	protected ContentAssistInvocationContext createContext(ITextViewer viewer,
			int offset) {
		return new ScriptContentAssistInvocationContext(viewer, offset,
				fEditor, getNatureId());
	}

	protected static class ScriptParameterListValidator implements
			IContextInformationValidator, IContextInformationPresenter {

		private int initialOffset;

		public void install(IContextInformation info, ITextViewer viewer,
				int offset) {
			initialOffset = offset;
		}

		public boolean isContextInformationValid(int offset) {
			return Math.abs(offset - initialOffset) < 5;
		}

		public boolean updatePresentation(int documentPosition,
				TextPresentation presentation) {
			return false;
		}
	}

}
