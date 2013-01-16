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
package org.eclipse.dltk.internal.ui.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.dltk.annotations.Internal;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.utils.DLTKLogging;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class DLTKLoggingPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private static class Option {
		final String id;
		final String name;

		public Option(String id, String name) {
			this.id = id;
			this.name = name;
		}

	}

	@Internal
	static class OptionLableProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof Option) {
				return ((Option) element).name;
			}
			return super.getText(element);
		}
	}

	public DLTKLoggingPreferencePage() {
		setDescription("Logging options for dynamic languages");
	}

	private CheckboxTableViewer viewer;

	@Override
	protected Control createContents(Composite parent) {
		viewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER
				| SWT.FULL_SELECTION);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new OptionLableProvider());
		final List<Option> options = collectOptions();
		viewer.setInput(options);
		final Set<String> optionIds = new HashSet<String>();
		for (Option option : options) {
			optionIds.add(option.id);
		}
		final Map<String, Boolean> state = DLTKLogging.getState(optionIds);
		final List<Option> checked = new ArrayList<Option>();
		for (Option option : options) {
			final Boolean value = state.get(option.id);
			if (value != null && value.booleanValue()) {
				checked.add(option);
			}
		}
		viewer.setCheckedElements(checked.toArray());
		return viewer.getControl();
	}

	private List<Option> collectOptions() {
		final IConfigurationElement[] elements = Platform
				.getExtensionRegistry().getConfigurationElementsFor(
						DLTKCore.PLUGIN_ID + ".loggingOptions"); //$NON-NLS-1$
		final List<Option> options = new ArrayList<Option>();
		for (IConfigurationElement element : elements) {
			if ("loggingOption".equals(element.getName())) { //$NON-NLS-1$
				final String id = element.getAttribute("id"); //$NON-NLS-1$
				final String name = element.getAttribute("name"); //$NON-NLS-1$
				if (id != null && id.length() != 0 && name != null
						&& name.length() != 0) {
					String qId = element.getContributor().getName() + "/" + id; //$NON-NLS-1$
					options.add(new Option(qId, name));
				}
			}
		}
		return options;
	}

	private boolean saveValues() {
		final Object input = viewer.getInput();
		final Map<String, Boolean> state = new HashMap<String, Boolean>();
		if (input instanceof List<?>) {
			for (Iterator<?> i = ((List<?>) input).iterator(); i.hasNext();) {
				final Object item = i.next();
				if (item instanceof Option) {
					state.put(((Option) item).id, Boolean.FALSE);
				}
			}
		}
		final Set<String> enabled = new HashSet<String>();
		for (Object checked : viewer.getCheckedElements()) {
			if (checked instanceof Option) {
				final Option option = (Option) checked;
				state.put(option.id, Boolean.TRUE);
				enabled.add(option.id);
			}
		}
		DLTKLogging.setState(state);
		return true;
	}

	@Override
	public boolean performOk() {
		return saveValues() && super.performOk();
	}

	@Override
	protected void performApply() {
		if (saveValues()) {
			super.performApply();
		}
	}

	public void init(IWorkbench workbench) {
	}

}
