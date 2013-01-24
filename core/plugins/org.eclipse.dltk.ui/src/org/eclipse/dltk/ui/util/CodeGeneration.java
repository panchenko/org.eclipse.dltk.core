/*******************************************************************************
 * Copyright (c) 2009 xored software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.ui.util;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.dltk.compiler.util.Util;
import org.eclipse.dltk.core.IBuffer;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.corext.util.Strings;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.swt.SWT;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;

public class CodeGeneration {

	public static String evaluateTemplate(TemplateContext context,
			Template template, String[] fullLineVariables) throws CoreException {
		TemplateBuffer buffer;
		try {
			buffer = context.evaluate(template);
			if (buffer == null)
				return null;
			String str = fixFullLineVariables(buffer, fullLineVariables);
			if (Strings.containsOnlyWhitespaces(str)) {
				return null;
			}
			return str;
		} catch (BadLocationException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		} catch (TemplateException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
	}

	// remove lines for empty variables, prefix multi-line variables
	private static String fixFullLineVariables(TemplateBuffer buffer,
			String[] variables) throws MalformedTreeException,
			BadLocationException {
		IDocument doc = new Document(buffer.getString());
		int nLines = doc.getNumberOfLines();
		MultiTextEdit edit = new MultiTextEdit();
		Set<Integer> removedLines = new HashSet<Integer>();
		for (int i = 0; i < variables.length; i++) {
			TemplateVariable position = findVariable(buffer, variables[i]);
			if (position == null) {
				continue;
			}
			if (position.getLength() > 0) {
				int[] offsets = position.getOffsets();
				for (int j = 0; j < offsets.length; j++) {
					final int offset = offsets[j];
					try {
						int startLine = doc.getLineOfOffset(offset);
						int startOffset = doc.getLineOffset(startLine);
						int endLine = doc.getLineOfOffset(offset
								+ position.getLength());
						String prefix = doc.get(startOffset, offset
								- startOffset);
						if (prefix.length() > 0 && startLine < endLine) {
							for (int line = startLine + 1; line <= endLine; ++line) {
								int lineOffset = doc.getLineOffset(line);
								edit.addChild(new InsertEdit(lineOffset, prefix));
							}
						}
					} catch (BadLocationException exc) {
						break;
					}
				}
			} else {
				int[] offsets = position.getOffsets();
				for (int k = 0; k < offsets.length; k++) {
					int line = doc.getLineOfOffset(offsets[k]);
					IRegion lineInfo = doc.getLineInformation(line);
					int offset = lineInfo.getOffset();
					String str = doc.get(offset, lineInfo.getLength());
					if (Strings.containsOnlyWhitespaces(str)
							&& nLines > line + 1 && removedLines.add(line)) {
						int nextStart = doc.getLineOffset(line + 1);
						int length = nextStart - offset;
						edit.addChild(new DeleteEdit(offset, length));
					}
				}
			}
		}
		edit.apply(doc, 0);
		return doc.get();
	}

	private static TemplateVariable findVariable(TemplateBuffer buffer,
			String variable) {
		TemplateVariable[] positions = buffer.getVariables();
		for (int i = 0; i < positions.length; i++) {
			TemplateVariable curr = positions[i];
			if (variable.equals(curr.getType())) {
				return curr;
			}
		}
		return null;
	}

	/**
	 * Examines a string and returns the first line delimiter found.
	 */
	public static String getLineDelimiterUsed(IModelElement elem)
			throws ModelException {
		if (elem == null)
			return Util.EMPTY_STRING;

		ISourceModule cu = (ISourceModule) elem
				.getAncestor(IModelElement.SOURCE_MODULE);
		if (cu != null && cu.exists()) {
			IBuffer buf = cu.getBuffer();
			int length = buf.getLength();
			for (int i = 0; i < length; i++) {
				char ch = buf.getChar(i);
				if (ch == SWT.CR) {
					if (i + 1 < length) {
						if (buf.getChar(i + 1) == SWT.LF) {
							return "\r\n"; //$NON-NLS-1$
						}
					}
					return "\r"; //$NON-NLS-1$
				} else if (ch == SWT.LF) {
					return "\n"; //$NON-NLS-1$
				}
			}
		}
		return getProjectLineDelimiter(elem.getScriptProject());
	}

	private static String getProjectLineDelimiter(IScriptProject cProject) {
		IProject project = null;
		if (cProject != null)
			project = cProject.getProject();

		String lineDelimiter = getLineDelimiterPreference(project);
		if (lineDelimiter != null)
			return lineDelimiter;

		return Util.LINE_SEPARATOR;
	}

	public static String getLineDelimiterPreference(IProject project) {
		IScopeContext[] scopeContext;
		if (project != null) {
			// project preference
			scopeContext = new IScopeContext[] { new ProjectScope(project) };
			String lineDelimiter = Platform.getPreferencesService().getString(
					Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null,
					scopeContext);
			if (lineDelimiter != null)
				return lineDelimiter;
		}
		// workspace preference
		scopeContext = new IScopeContext[] { InstanceScope.INSTANCE };
		String platformDefault = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		return Platform.getPreferencesService().getString(Platform.PI_RUNTIME,
				Platform.PREF_LINE_SEPARATOR, platformDefault, scopeContext);
	}

}
