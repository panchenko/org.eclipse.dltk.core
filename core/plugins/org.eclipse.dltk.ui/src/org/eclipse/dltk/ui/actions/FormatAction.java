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
package org.eclipse.dltk.ui.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IModelElementVisitor;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.PreferencesLookupDelegate;
import org.eclipse.dltk.internal.ui.actions.ActionMessages;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.dltk.ui.formatter.IScriptFormatter;
import org.eclipse.dltk.ui.formatter.IScriptFormatterFactory;
import org.eclipse.dltk.ui.formatter.ScriptFormatterManager;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IWorkbenchSite;

public class FormatAction extends SelectionDispatchAction {

	/**
	 * @param site
	 */
	protected FormatAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.FormatAction_label);
	}

	static class ModuleCollector implements IModelElementVisitor {

		final List<ISourceModule> modules = new ArrayList<ISourceModule>();

		public boolean visit(IModelElement element) {
			if (element instanceof ISourceModule) {
				final ISourceModule module = (ISourceModule) element;
				if (!module.isReadOnly()) {
					modules.add((ISourceModule) element);
				}
				return false;
			}
			return element.getElementType() < IModelElement.SOURCE_MODULE;
		}

	}

	public void run(IStructuredSelection selection) {
		final ModuleCollector collector = new ModuleCollector();
		for (Iterator<?> i = selection.iterator(); i.hasNext();) {
			final Object obj = i.next();
			if (obj instanceof IModelElement) {
				try {
					((IModelElement) obj).accept(collector);
				} catch (ModelException e) {
					DLTKUIPlugin.log(e);
				}
			}
		}
		if (!collector.modules.isEmpty()) {
			for (final ISourceModule module : collector.modules) {
				final IResource resource = module.getResource();
				if (resource != null && resource.getType() == IResource.FILE
						&& resource.exists()) {
					final IScriptProject project = module.getScriptProject();
					final IScriptFormatterFactory formatterFactory = ScriptFormatterManager
							.getSelected(project);
					if (formatterFactory != null) {
						try {
							final String source = module.getSource();
							final Document document = new Document(source);
							final String lineDelimiter = TextUtilities
									.getDefaultLineDelimiter(document);
							final Map<String, String> preferences = formatterFactory
									.retrievePreferences(new PreferencesLookupDelegate(
											project));
							final IScriptFormatter formatter = formatterFactory
									.createFormatter(lineDelimiter, preferences);
							final TextEdit edit = formatter.format(source, 0,
									source.length(), 0);
							if (edit != null) {
								edit.apply(document);
								final String newSource = document.get();
								if (!source.equals(newSource)) {
									module.becomeWorkingCopy(null, null);
									module.getBuffer().setContents(newSource);
									module.commitWorkingCopy(true, null);
								}
							}
						} catch (Exception e) {
							DLTKUIPlugin.log(e);
							break;
						}
					}
				}
			}
		}
	}

}
