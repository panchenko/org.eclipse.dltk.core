/*******************************************************************************
 * Copyright (c) 2012 NumberFour AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     NumberFour AG - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.ui.tests;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.internal.ui.editor.ScriptEditor;
import org.eclipse.dltk.ui.DLTKUILanguageManager;
import org.eclipse.dltk.ui.IDLTKUILanguageToolkit;
import org.eclipse.dltk.ui.text.completion.CompletionProposalCategory;
import org.eclipse.dltk.ui.text.completion.CompletionProposalComputerRegistry;
import org.eclipse.dltk.ui.text.completion.ProposalSorterRegistry;
import org.eclipse.dltk.ui.text.completion.ScriptContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorPart;
import org.junit.Assert;

public class UICompletionUtil {

	private final ScriptEditor editor;
	private Integer offset;

	private UICompletionUtil(ScriptEditor editor) {
		this.editor = editor;
	}

	public static UICompletionUtil on(IEditorPart part) {
		return new UICompletionUtil((ScriptEditor) part);
	}

	public UICompletionUtil after(String marker) {
		calculateOffset(marker, false, true);
		return this;
	}

	public UICompletionUtil afterLast(String marker) {
		calculateOffset(marker, true, true);
		return this;
	}

	private void calculateOffset(String marker, boolean last, boolean after) {
		final String text = getDocument().get();
		final int offset = last ? text.lastIndexOf(marker) : text
				.indexOf(marker);
		Assert.assertTrue(
				NLS.bind("Pattern \"{0}\" not found in {1}", marker,
						editor.getTitle()), offset != -1);
		this.offset = offset + (after ? marker.length() : 0);
	}

	private IDocument getDocument() {
		return getViewer().getDocument();
	}

	private ISourceViewer getViewer() {
		return editor.getViewer();
	}

	private int getOffset() {
		Assert.assertNotNull("Completion offset not specified", offset);
		return offset.intValue();
	}

	public List<ICompletionProposal> invokeCompletion() {

		final IDLTKLanguageToolkit toolkit = editor.getLanguageToolkit();
		final ScriptContentAssistInvocationContext context = new ScriptContentAssistInvocationContext(
				getViewer(), getOffset(), editor, toolkit.getNatureId());

		final IDLTKUILanguageToolkit ui = DLTKUILanguageManager
				.getLanguageToolkit(toolkit);

		final String contentType;
		try {
			contentType = TextUtilities.getContentType(getDocument(),
					ui.getPartitioningId(), offset, false);
		} catch (BadLocationException e) {
			throw new AssertionError(e);
		}

		final List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		for (CompletionProposalCategory cat : CompletionProposalComputerRegistry
				.getDefault().getProposalCategories()) {
			proposals.addAll(cat.computeCompletionProposals(context,
					contentType, new NullProgressMonitor()));
		}
		ProposalSorterRegistry.getDefault().getCurrentSorter()
				.sortProposals(context, proposals);
		return proposals;
	}

	public void apply(ICompletionProposal proposal) {
		((ICompletionProposalExtension2) proposal).apply(getViewer(), (char) 0,
				0, getOffset());
	}

	public String getText() {
		return getDocument().get();
	}

}
