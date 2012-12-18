/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - Initial implementation
 *     xored software, Inc. - implement IHyperlink.getHyperlinkText() & getTypeLabel (Alex Panchenko)  
 *******************************************************************************/
package org.eclipse.dltk.internal.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.dltk.core.ICodeAssist;
import org.eclipse.dltk.core.ICodeSelection;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ISourceRange;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.ui.DelegatedOpen;
import org.eclipse.dltk.internal.ui.OpenDelegateManager;
import org.eclipse.dltk.internal.ui.text.ScriptWordFinder;
import org.eclipse.dltk.ui.IOpenDelegate;
import org.eclipse.dltk.ui.actions.OpenAction;
import org.eclipse.dltk.ui.infoviews.ModelElementArray;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetectorExtension;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Script element hyperlink detector.
 */
public class ModelElementHyperlinkDetector implements IHyperlinkDetector,
		IHyperlinkDetectorExtension {

	private ITextEditor fTextEditor;

	/**
	 * Creates a new Script element hyperlink detector.
	 * 
	 * @param editor
	 *            the editor in which to detect the hyperlink
	 */
	public ModelElementHyperlinkDetector(ITextEditor editor) {
		Assert.isNotNull(editor);
		fTextEditor = editor;
	}

	/*
	 * @see
	 * org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(
	 * org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion,
	 * boolean)
	 */
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer,
			IRegion region, boolean canShowMultipleHyperlinks) {
		if (region == null || !(fTextEditor instanceof ScriptEditor))
			return null;

		IAction action = fTextEditor.getAction("OpenEditor"); //$NON-NLS-1$
		if (action == null || !(action instanceof OpenAction))
			return null;
		final OpenAction openAction = (OpenAction) action;

		int offset = region.getOffset();

		IModelElement input = EditorUtility.getEditorInputModelElement(
				fTextEditor, false);
		if (input == null)
			return null;

		try {
			IDocument document = fTextEditor.getDocumentProvider().getDocument(
					fTextEditor.getEditorInput());
			IRegion wordRegion = ScriptWordFinder.findWord(document, offset);
			if (wordRegion == null || wordRegion.getLength() == 0)
				return null;

			final ICodeSelection selection = ((ICodeAssist) input)
					.codeSelectAll(wordRegion.getOffset(),
							wordRegion.getLength());
			if (selection == null) {
				return null;
			}
			final List<Object> elements = new ArrayList<Object>(
					selection.size());
			for (Object element : selection) {
				if (element instanceof IModelElement) {
					elements.add(element);
				} else {
					final IOpenDelegate adapter = OpenDelegateManager
							.findFor(element);
					if (adapter != null) {
						elements.add(new DelegatedOpen(adapter, element));
					} else {
						continue;
					}
				}
			}
			if (!elements.isEmpty()) {
				final IHyperlink link;
				if (elements.size() == 1) {
					final ISourceRange elementRange = selection
							.rangeOf(elements.get(0));
					if (elementRange != null) {
						wordRegion = new Region(elementRange.getOffset(),
								elementRange.getLength());
					}
					link = new ModelElementHyperlink(wordRegion,
							elements.get(0), openAction);
				} else {
					link = new ModelElementHyperlink(wordRegion,
							new ModelElementArray(elements), openAction);
				}
				return new IHyperlink[] { link };
			}
		} catch (ModelException e) {
			return null;
		}

		return null;
	}

	public void dispose() {
		this.fTextEditor = null;
	}
}
