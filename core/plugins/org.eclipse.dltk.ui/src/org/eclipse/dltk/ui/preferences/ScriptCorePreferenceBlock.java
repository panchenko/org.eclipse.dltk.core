/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc.  
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html  
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Andrei Sobolev)
 *******************************************************************************/
package org.eclipse.dltk.ui.preferences;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.search.indexing.IndexManager;
import org.eclipse.dltk.internal.core.ModelManager;
import org.eclipse.dltk.ui.PreferenceConstants;
import org.eclipse.dltk.ui.preferences.OverlayPreferenceStore.OverlayKey;
import org.eclipse.dltk.ui.util.SWTFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

final class ScriptCorePreferenceBlock extends
		ImprovedAbstractConfigurationBlock {

	private final class ReindexOperation implements IRunnableWithProgress {
		public void run(IProgressMonitor monitor) {
			try {
				ResourcesPlugin.getWorkspace().build(
						IncrementalProjectBuilder.FULL_BUILD, monitor);
			} catch (CoreException e) {
				if (DLTKCore.DEBUG) {
					e.printStackTrace();
				}
			}
		}
	}

	ScriptCorePreferenceBlock(OverlayPreferenceStore store, PreferencePage page) {
		super(store, page);
	}

	private static final String[] names = new String[] {
			Messages.ScriptCorePreferenceBlock_Warning,
			Messages.ScriptCorePreferenceBlock_Error };
	private static final String[] ids = new String[] { DLTKCore.WARNING,
			DLTKCore.ERROR };

	private Combo circularBuildPathCombo;

	public Control createControl(Composite parent) {
		Composite composite = SWTFactory.createComposite(parent,
				parent.getFont(), 1, 1, GridData.FILL_BOTH);

		// Group coreGroup = SWTFactory.createGroup(composite,
		// Messages.ScriptCorePreferenceBlock_coreOptions, 2, 1,
		// GridData.FILL_HORIZONTAL);

		Group editorGroup = SWTFactory.createGroup(composite,
				Messages.ScriptCorePreferenceBlock_editOptions, 2, 1,
				GridData.FILL_HORIZONTAL);

		bindControl(
				SWTFactory.createCheckButton(
						editorGroup,
						PreferencesMessages.EditorPreferencePage_evaluateTemporaryProblems,
						2),
				PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS);
		// Connection timeout
		SWTFactory.createLabel(editorGroup,
				Messages.ScriptCorePreferenceBlock_CodeAssistTimeout, 1);
		final Text connectionTimeout = SWTFactory.createText(editorGroup,
				SWT.BORDER, 1, ""); //$NON-NLS-1$
		bindControl(connectionTimeout, PreferenceConstants.CODEASSIST_TIMEOUT,
				FieldValidators.POSITIVE_NUMBER_VALIDATOR);

		Group uiGroup = SWTFactory.createGroup(composite,
				Messages.ScriptCorePreferenceBlock_UI_Options, 1, 1,
				GridData.FILL_HORIZONTAL);

		bindControl(
				SWTFactory
						.createCheckButton(
								uiGroup,
								Messages.EditorPreferencePage_ResourceShowError_InvalidResourceName),
				PreferenceConstants.RESOURCE_SHOW_ERROR_INVALID_RESOURCE_NAME);

		Group builderGroup = SWTFactory.createGroup(composite,
				Messages.ScriptCorePreferenceBlock_Builder_Options, 2, 1,
				GridData.FILL_HORIZONTAL);

		SWTFactory
				.createLabel(
						builderGroup,
						Messages.ScriptCorePreferenceBlock_Builder_CircularDependencies,
						1);
		circularBuildPathCombo = SWTFactory.createCombo(builderGroup,
				SWT.READ_ONLY | SWT.BORDER, 0, names);
		createReIndex(composite);

		return composite;
	}

	private void createReIndex(Composite composite) {
		if (DLTKCore.SHOW_REINDEX) {
			Group g = SWTFactory.createGroup(composite,
					Messages.ScriptCorePreferenceBlock_debugOptionsOperations,
					2, 1, GridData.FILL_HORIZONTAL);

			Label l = new Label(g, SWT.PUSH);
			l.setText(Messages.ScriptCorePreferencePage_manualReindex);
			Button reCreateIndex = new Button(g, SWT.PUSH);
			reCreateIndex.setText(Messages.ScriptCorePreferencePage_reindex);
			reCreateIndex.addSelectionListener(new SelectionListener() {

				public void widgetDefaultSelected(SelectionEvent e) {
				}

				public void widgetSelected(SelectionEvent e) {
					IndexManager indexManager = ModelManager.getModelManager()
							.getIndexManager();
					indexManager.rebuild();

					try {
						PlatformUI.getWorkbench().getProgressService()
								.run(false, true, new ReindexOperation());
					} catch (InvocationTargetException e3) {
						if (DLTKCore.DEBUG) {
							e3.printStackTrace();
						}
					} catch (InterruptedException e3) {
						if (DLTKCore.DEBUG) {
							e3.printStackTrace();
						}
					}
				}
			});
		}
	}

	@Override
	protected List<OverlayKey> createOverlayKeys() {
		ArrayList<OverlayKey> overlayKeys = new ArrayList<OverlayKey>();
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(
				OverlayPreferenceStore.BOOLEAN,
				PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(
				OverlayPreferenceStore.BOOLEAN,
				PreferenceConstants.RESOURCE_SHOW_ERROR_INVALID_RESOURCE_NAME));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(
				OverlayPreferenceStore.INT,
				PreferenceConstants.CODEASSIST_TIMEOUT));
		return overlayKeys;
	}

	@Override
	public void initialize() {
		super.initialize();
		initializeBuildPathField(DLTKCore.getPlugin().getPluginPreferences()
				.getString(DLTKCore.CORE_CIRCULAR_BUILDPATH));

	}

	@Override
	public void performDefaults() {
		super.performDefaults();
		initializeBuildPathField(DLTKCore.getPlugin().getPluginPreferences()
				.getDefaultString(DLTKCore.CORE_CIRCULAR_BUILDPATH));
	}

	/**
	 * @param defaultCircularBuildPath
	 */
	private void initializeBuildPathField(String defaultCircularBuildPath) {
		for (int i = 0; i < ids.length; i++) {
			if (ids[i].equals(defaultCircularBuildPath)) {
				circularBuildPathCombo.select(i);
				break;
			}
		}
	}

	@Override
	protected void initializeFields() {
		super.initializeFields();
	}

	@Override
	public void performOk() {
		super.performOk();
		final int buildPathIndex = circularBuildPathCombo.getSelectionIndex();
		if (buildPathIndex >= 0 && buildPathIndex < ids.length) {
			final Preferences prefs = DLTKCore.getDefault()
					.getPluginPreferences();
			final String value = ids[buildPathIndex];
			if (!value
					.equals(prefs.getString(DLTKCore.CORE_CIRCULAR_BUILDPATH))) {
				prefs.setValue(DLTKCore.CORE_CIRCULAR_BUILDPATH, value);
			}
		}
		DLTKCore.getDefault().savePluginPreferences();
	}

}
