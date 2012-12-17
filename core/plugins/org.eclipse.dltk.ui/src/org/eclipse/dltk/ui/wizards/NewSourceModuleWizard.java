/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.ui.wizards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IScriptFolder;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.environment.EnvironmentManager;
import org.eclipse.dltk.core.environment.IEnvironment;
import org.eclipse.dltk.internal.ui.editor.EditorUtility;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.dltk.utils.LazyExtensionManager;
import org.eclipse.dltk.utils.LazyExtensionManager.Descriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;

public abstract class NewSourceModuleWizard extends NewElementWizard implements
		ISourceModuleWizard {

	static class WizardExtensionManager extends
			LazyExtensionManager<ISourceModuleWizardExtension> {

		static final String EXTENSION_POINT = DLTKUIPlugin.PLUGIN_ID
				+ ".sourceModuleWizardExtension"; //$NON-NLS-1$
		private final String nature;

		/**
		 * @param nature
		 */
		public WizardExtensionManager(String nature) {
			super(EXTENSION_POINT);
			this.nature = nature;
		}

		private static final String ATTR_NATURE = "nature"; //$NON-NLS-1$

		@Override
		protected boolean isValidDescriptor(
				Descriptor<ISourceModuleWizardExtension> descriptor) {
			final String extensionNatureId = descriptor
					.getAttribute(ATTR_NATURE);
			return extensionNatureId == null
					|| this.nature.equals(extensionNatureId)
					|| extensionNatureId.equals("#"); //$NON-NLS-1$
		}

	}

	private NewSourceModulePage page;

	private ISourceModule module;

	protected abstract NewSourceModulePage createNewSourceModulePage();

	private final List<ISourceModuleWizardExtension> extensions = new ArrayList<ISourceModuleWizardExtension>();

	@Override
	public void addPages() {
		super.addPages();
		page = createNewSourceModulePage();
		createExtensions();
		page.init(getSelection());
		addPage(page);
	}

	private void createExtensions() {
		final WizardExtensionManager manager = new WizardExtensionManager(page
				.getRequiredNature());
		for (Descriptor<ISourceModuleWizardExtension> descriptor : manager
				.getDescriptors()) {
			final ISourceModuleWizardExtension extension = descriptor.get();
			if (extension != null && extension.start(this)) {
				extensions.add(extension);
			}
		}
	}

	@Override
	public void createPageControls(Composite pageContainer) {
		for (ISourceModuleWizardExtension extension : extensions) {
			extension.initialize();
		}
		super.createPageControls(pageContainer);
		created = true;
	}

	/**
	 * @since 2.0
	 */
	protected List<ISourceModuleWizardExtension> getExtensions() {
		return Collections.unmodifiableList(extensions);
	}

	@Override
	public IModelElement getCreatedElement() {
		return module;
	}

	@Override
	protected void finishPage(IProgressMonitor monitor)
			throws InterruptedException, CoreException {
		module = page.createFile(monitor);
	}

	@Override
	public boolean performFinish() {
		final boolean result = super.performFinish();
		if (result && module != null) {
			openSourceModule(module);
		}
		return result;
	}

	protected void openSourceModule(final ISourceModule module) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {
					EditorUtility.openInEditor(module);
				} catch (PartInitException e) {
					final String msg = NLS.bind(
							Messages.NewSourceModuleWizard_errorInOpenInEditor,
							module.getElementName());
					DLTKUIPlugin.logErrorMessage(msg, e);
				} catch (ModelException e) {
					final String msg = NLS.bind(
							Messages.NewSourceModuleWizard_errorInOpenInEditor,
							module.getElementName());
					DLTKUIPlugin.logErrorMessage(msg, e);
				}
			}
		});
	}

	/**
	 * @since 2.0
	 */
	public IEnvironment getEnvironment() {
		return EnvironmentManager.getEnvironment(getProject());
	}

	/**
	 * @since 2.0
	 */
	public IProject getProject() {
		return getFolder().getScriptProject().getProject();
	}

	/**
	 * @since 2.0
	 */
	public IScriptFolder getFolder() {
		return page.getScriptFolder();
	}

	/**
	 * @since 2.0
	 */
	public String getFileName() {
		return page.getFileName();
	}

	/**
	 * @since 2.0
	 */
	public void validate() {
		page.handleFieldChanged(NewSourceModulePage.EXTENSIONS);
	}

	private boolean created = false;
	private String mode = null;
	private Set<String> disabledModes = null;

	/**
	 * @since 2.0
	 */
	public String getMode() {
		if (!created) {
			return mode;
		} else {
			return page.getMode();
		}
	}

	/**
	 * @since 2.0
	 */
	public void setMode(String mode) {
		if (!created) {
			this.mode = mode;
		} else {
			page.setMode(mode);
		}
	}

	/**
	 * @since 2.0
	 */
	public void enableMode(String mode, boolean enable) {
		if (!created) {
			if (enable) {
				if (disabledModes != null) {
					disabledModes.remove(mode);
				}
			} else {
				if (disabledModes == null) {
					disabledModes = new HashSet<String>();
				}
				disabledModes.add(mode);
			}
		} else {
			page.enableMode(mode, enable);
		}
	}

	boolean isModeEnabled(String mode) {
		if (disabledModes == null) {
			return true;
		}
		return !disabledModes.contains(mode);
	}

	private Map<String, ListenerList> listeners = null;

	/**
	 * @since 2.0
	 */
	public void addFieldChangeListener(String field,
			IFieldChangeListener listener) {
		if (listeners == null) {
			listeners = new HashMap<String, ListenerList>();
		}
		ListenerList list = listeners.get(field);
		if (list == null) {
			list = new ListenerList();
			listeners.put(field, list);
		}
		list.add(listener);
	}

	/**
	 * @since 2.0
	 */
	public void removeFieldChangeListener(String field,
			IFieldChangeListener listener) {
		if (listeners != null) {
			final ListenerList list = listeners.get(field);
			if (list != null) {
				list.remove(listener);
				if (list.isEmpty()) {
					listeners.remove(field);
				}
			}
		}
	}

	void fireFieldChange(String field) {
		if (listeners != null) {
			final ListenerList list = listeners.get(field);
			if (list != null) {
				for (Object listener : list.getListeners()) {
					if (listener instanceof IFieldChangeListener) {
						((IFieldChangeListener) listener).fieldChanged();
					}
				}
			}
		}
	}

}
