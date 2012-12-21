/*******************************************************************************
 * Copyright (c) 2012 NumberFour AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     NumberFour AG - initial API and Implementation (Jens von Pilgrim)
 *******************************************************************************/
package org.eclipse.dltk.ui.wizards;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IProjectFragment;
import org.eclipse.dltk.core.IScriptFolder;
import org.eclipse.dltk.core.IScriptModel;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.dltk.internal.ui.wizards.NewWizardMessages;
import org.eclipse.dltk.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.dltk.ui.ModelElementLabelProvider;
import org.eclipse.dltk.ui.dialogs.StatusInfo;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

/**
 * Wizard page that acts as a base class for wizard pages that create new Script
 * elements, supporting the notion of packages (i.e. namespaces). Source folder
 * selection is restricted to the root source folders. Its sub-folders are
 * interpreted as packages and can be selected accordingly. By enabling
 * <code>{@link #setAutoCreateMissingPackages(boolean) autoCreateMissingPackages}</code>
 * , the wizard does also create missing packages automatically when
 * {@link #createFile(IProgressMonitor) creating} the file.
 * <p>
 * This page behaves quite similar to its super class (cf. Liskov substitution
 * principle). That is, clients can use the {@link NewSourceModuleInPackagePage}
 * just like a {@link NewSourceModulePage}, by using
 * <code>{@link #setScriptFolder(IScriptFolder, boolean) set}/{@link #getScriptFolder() get}ScriptFolder(..)</code>
 * . The separation of source folder and contained packages is transparently
 * handled. Client can also access the source folder and package path
 * separately, though (cf.
 * <code>{@link #setSource(IProjectFragment, boolean) set}/{@link #getSource() get}Source(..)</code>
 * and
 * <code>{@link #setPackage(IPath, boolean) set}/{@link #getPackage() get}Package(..))</code>
 * .
 * </p>
 */
public abstract class NewSourceModuleInPackagePage extends NewSourceModulePage {

	/** Id of the package field */
	protected static final String PACKAGE = "NewPackagedSourceModulePage.package"; //$NON-NLS-1$

	/**
	 * Status of last validation of the package field
	 */
	protected IStatus packageStatus;

	private boolean sourceIsModifiable = true;

	private boolean autoCreateMissingPackages = false;

	private StringButtonDialogField fPackageDialogField;

	/**
	 * Package in terms of a path relative to the source folder (i.e.
	 * container).
	 */
	private IPath currentPackagePath = null;

	private class PackageFieldAdapter implements IStringButtonAdapter,
			IDialogFieldListener {

		public void dialogFieldChanged(DialogField field) {
			packageDialogFieldChanged();
		}

		public void changeControlPressed(DialogField field) {
			packageChangeControlPressed();
		}
	}

	public NewSourceModuleInPackagePage() {

		PackageFieldAdapter packageFieldAdapter = new PackageFieldAdapter();
		fPackageDialogField = new StringButtonDialogField(packageFieldAdapter);
		fPackageDialogField.setDialogFieldListener(packageFieldAdapter);
		fPackageDialogField
				.setButtonLabel(NewWizardMessages.NewSourceModuleInPackagePage_package_button);
		fPackageDialogField.setLabelText(getPackageLabel());

		packageStatus = new StatusInfo();
	}

	/**
	 * Returns whether missing packages are automatically created on finish,
	 * i.e. in {@link #createFile(org.eclipse.core.runtime.IProgressMonitor)}.
	 * Default value (after constrution) is false.
	 */
	public boolean isAutoCreateMissingPackages() {
		return autoCreateMissingPackages;
	}

	/**
	 * Sets whether missing packages are to be automatically created on finish
	 * or not. This also influences verification of page, as it must not be
	 * finished with non-exisiting packages in case this is set to false.
	 * Default value (after constrution) is false.
	 */
	public void setAutoCreateMissingPackages(boolean autoCreateMissingPackages) {
		this.autoCreateMissingPackages = autoCreateMissingPackages;
	}

	public void setSource(IProjectFragment src, boolean canBeModified) {
		sourceIsModifiable = canBeModified;
		if (src == null) {
			super.setScriptFolder(null, sourceIsModifiable);
		} else {
			super.setScriptFolder(src.getScriptFolder(""), sourceIsModifiable);
		}
	}

	public IProjectFragment getSource() {
		return getProjectFragment();
	}

	public void setPackage(IPath srcRelativePath, boolean canBeModified) {

		currentPackagePath = srcRelativePath;

		if (currentPackagePath == null) {
			fPackageDialogField.setText("");
		} else {
			String str = currentPackagePath.toString(); //$NON-NLS-1$
			fPackageDialogField.setText(str);
		}
		fPackageDialogField.setEnabled(canBeModified);

	}

	public IPath getPackage() {
		return currentPackagePath;
	}

	@Override
	public void setScriptFolder(IScriptFolder root, boolean canBeModified) {
		IProjectFragment src = null;
		IPath packPath = null;
		if (root != null) {
			src = (IProjectFragment) root
					.getAncestor(IModelElement.PROJECT_FRAGMENT);
			if (src != null) {
				IPath srcPath = src.getPath();
				packPath = root.getPath().makeRelativeTo(srcPath);
			}
		}
		setSource(src, canBeModified);
		setPackage(packPath, canBeModified);
	}

	@Override
	public IScriptFolder getScriptFolder() {
		IProjectFragment src = getSource();
		if (src != null) {
			if (currentPackagePath == null) {
				return src.getScriptFolder("");
			} else {
				return src.getScriptFolder(currentPackagePath);
			}
		}
		return null;
	}

	protected String getPackageLabel() {
		return NewWizardMessages.NewSourceModuleInPackagePage_package_label;
	}

	/**
	 * Returns the content of the package input field.
	 * 
	 * @return the content of the package input field
	 */
	public String getPackageText() {
		return fPackageDialogField.getText();
	}

	/**
	 * Sets the content of the package input field to the given value.
	 * 
	 * @param str
	 *            the new package input field text
	 * @param canBeModified
	 *            if <code>true</code> the package input field can be modified;
	 *            otherwise it is read-only.
	 */
	public void setPackageText(String str, boolean canBeModified) {
		fPackageDialogField.setText(str);
		fPackageDialogField.setEnabled(canBeModified);
	}

	@Override
	void containerChangeControlPressed(DialogField field) {
		IScriptFolder root = chooseContainer();
		if (root != null) {
			setSource(
					(IProjectFragment) root
							.getAncestor(IModelElement.PROJECT_FRAGMENT),
					sourceIsModifiable);
		}
	}

	/**
	 * Opens a selection dialog that allows to select a source container.
	 * 
	 * @return returns the selected package fragment root or <code>null</code>
	 *         if the dialog has been canceled. The caller typically sets the
	 *         result to the container input field.
	 *         <p>
	 *         Clients can override this method if they want to offer a
	 *         different dialog.
	 *         </p>
	 * 
	 * 
	 */
	@Override
	protected IScriptFolder chooseContainer() {
		IModelElement initElement = getProjectFragment();
		Class<?>[] shownTypes = new Class[] { IScriptModel.class,
				IScriptProject.class, IProjectFragment.class };

		ViewerFilter filter = new ContainerViewerFilter(shownTypes);

		Class<?>[] acceptedTypes = new Class[] { IProjectFragment.class };
		ISelectionStatusValidator validator = new TypedElementSelectionValidator(
				acceptedTypes, false);

		IScriptFolder scriptFolder = doChooseContainer(initElement, filter,
				validator);
		return scriptFolder;
	}

	/**
	 * Calls {@link NewContainerWizardPage#containerChanged()} and additionally
	 * checks if currently selected source folder is a Script project's source
	 * folder.
	 */
	@Override
	protected IStatus containerChanged() {
		IStatus status = super.containerChanged();
		if (status.isOK()) {
			try {
				if (getSource().getKind() == IProjectFragment.K_SOURCE
						&& !getSource().isExternal()) {
					return status;
				}
			} catch (ModelException e) {
				DLTKUIPlugin.log(e);
			}
			StatusInfo statusInfo = new StatusInfo();
			statusInfo
					.setError(NewWizardMessages.NewSourceModuleInPackagePage_error_ContainerIsNoSourceFolder);
			status = statusInfo;
		}
		return status;
	}



	private void packageChangeControlPressed() {
		IScriptFolder packageFolder = choosePackage();
		if (packageFolder == null) {
			return;
		}
		IProjectFragment projectFragment = (IProjectFragment) packageFolder
				.getAncestor(IModelElement.PROJECT_FRAGMENT);
		if (projectFragment != null) {
			IPath path = packageFolder.getPath().makeRelativeTo(
					projectFragment.getPath());
			setPackage(path, true);
		} else {
			DLTKUIPlugin
					.logErrorMessage("Illegal state, chosen package is not contained in a project fragment"); //$NON-NLS-N$
		}
	}

	/**
	 * Opens a selection dialog that allows to select a package.
	 * 
	 * @return returns the selected package or <code>null</code> if the dialog
	 *         has been canceled. The caller typically sets the result to the
	 *         package input field.
	 *         <p>
	 *         Clients can override this method if they want to offer a
	 *         different dialog.
	 *         </p>
	 * 
	 * 
	 */
	protected IScriptFolder choosePackage() {
		IScriptFolder[] packages = getAllPackages();
		ILabelProvider labelProvider = new ModelElementLabelProvider(
				ModelElementLabelProvider.SHOW_DEFAULT);

		ElementListSelectionDialog dialog = new ElementListSelectionDialog(
				getShell(), labelProvider);

		dialog.setIgnoreCase(false);
		dialog.setTitle(NewWizardMessages.NewSourceModuleInPackagePage_ChoosePackageDialog_title);
		dialog.setMessage(NewWizardMessages.NewSourceModuleInPackagePage_ChoosePackageDialog_description);
		dialog.setEmptyListMessage(NewWizardMessages.NewSourceModuleInPackagePage_ChoosePackageDialog_empty);
		dialog.setElements(packages);
		dialog.setHelpAvailable(false);

		IScriptFolder packFolder = getScriptFolder();
		if (packFolder != null) {
			dialog.setInitialSelections(new Object[] { packFolder });
		}

		if (dialog.open() == Window.OK) {
			return (IScriptFolder) dialog.getFirstResult();
		}
		return null;
	}

	/**
	 * Returns all packages in the currently selected source folder to be shown
	 * in the package selection dialog. The default implementation simply
	 * returns all sub-folders of the current source folder, including the
	 * default package (i.e. the source folder itself).
	 * <p>
	 * Subclasses may override in case other packages are to be displayed. Note
	 * that sorting of packages is done by dialog when using the default
	 * {@link #choosePackage()} implementation.
	 * </p>
	 */
	protected IScriptFolder[] getAllPackages() {
		Collection<IScriptFolder> packages = new ArrayList<IScriptFolder>();
		IProjectFragment sourceFolder = getProjectFragment();
		if (sourceFolder != null) {
			try {
				for (IModelElement e : sourceFolder.getChildren()) {
					if (e instanceof IScriptFolder) {
						packages.add((IScriptFolder) e);
					}
				}
			} catch (ModelException e) {
				DLTKUIPlugin.log(e);
			}

		}
		return packages.toArray(new IScriptFolder[packages.size()]);
	}


	private void packageDialogFieldChanged() {
		packageStatus = packageChanged();
		// tell all others
		handleFieldChanged(PACKAGE);
	}

	/*
	 * Verifies the input for the package field.
	 */
	protected IStatus packageChanged() {
		StatusInfo status = new StatusInfo();

		String packName = getPackageText();
		if (packName.length() == 0) {
			status.setError(NewWizardMessages.NewPackageWizardPage_error_EnterName);
			return status;
		}

		IProjectFragment root = getProjectFragment();
		if (root != null && root.getScriptProject().exists()) {
			IScriptFolder pack = root.getScriptFolder(packName);

			IPath srcPath = root.getPath();
			currentPackagePath = pack.getPath().makeRelativeTo(srcPath);

			try {
				// IPath rootPath = root.getPath();
				if (!pack.exists()) {
					URI location = pack.getResource().getLocationURI();
					if (location != null) {
						IFileStore store = EFS.getStore(location);
						if (store.fetchInfo().exists()) {
							status.setError(NewWizardMessages.NewPackageWizardPage_error_PackageExistsDifferentCase);
						}
					}
					if (!status.isError() && !autoCreateMissingPackages) {
						status.setError(NewWizardMessages.NewSourceModuleInPackagePage_error_PackageDoesNotExist);
					}
				}
			} catch (CoreException e) {
				DLTKUIPlugin.log(e);
			}
		}

		return status;
	}



	/**
	 * Hook method that gets called when a field on this page has changed. For
	 * this page the method gets called when the source folder field changes.
	 * <p>
	 * Every sub type is responsible to call this method when a field on its
	 * page has changed. Subtypes override (extend) the method to add
	 * verification when a own field has a dependency to an other field. For
	 * example the class name input must be verified again when the package
	 * field changes (check for duplicated class names).
	 * 
	 * @param fieldName
	 *            The name of the field that has changed (field id). For the
	 *            source folder the field id is
	 *            {@link NewContainerWizardPage#CONTAINER} , for the package
	 *            it's {@link #PACKAGE}, and for the file name
	 *            {@link NewSourceModulePage#FILE}
	 */
	@Override
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		if (PACKAGE.equals(fieldName)) {
			// super classes have to update script folder accordingly
			super.handleFieldChanged(CONTAINER);
		}
		if (CONTAINER.equals(fieldName)) {
			packageStatus = packageChanged();
		}

		// do status line update
		updateStatus(new IStatus[] { containerStatus, packageStatus,
				sourceModuleStatus });

	}

	/**
	 * Calls {@link NewSourceModulePage#createFile(IProgressMonitor)} and
	 * automatically creates missing packages, if
	 * {@link #isAutoCreateMissingPackages()} returns true.
	 */
	@Override
	public ISourceModule createFile(IProgressMonitor monitor)
			throws CoreException {
		if (autoCreateMissingPackages) {
			String packageName = getPackage().toString();
			getProjectFragment().createScriptFolder(packageName, true, monitor);
		}
		return super.createFile(monitor);
	}

	/**
	 * Creates the necessary controls (label, text field and browse button) to
	 * edit the source folder location and the package folder. The method
	 * expects that the parent composite uses a <code>GridLayout</code> as its
	 * layout manager and that the grid layout has at least 3 columns.
	 * 
	 * @param parent
	 *            the parent composite
	 * @param nColumns
	 *            the number of columns to span. This number must be greater or
	 *            equal three
	 */
	@Override
	protected void createContainerControls(Composite parent, int nColumns) {
		super.createContainerControls(parent, nColumns);
		createPackageControls(parent, nColumns);
	}

	/**
	 * Creates the controls for the package name field. Expects a
	 * <code>GridLayout</code> with at least 3 columns. This method must only be
	 * called if packages are supported.
	 * 
	 * @param composite
	 *            the parent composite
	 * @param nColumns
	 *            number of columns to span
	 */
	protected void createPackageControls(Composite composite, int nColumns) {
		fPackageDialogField.doFillIntoGrid(composite, nColumns);
		Text text = fPackageDialogField.getTextControl(null);
		LayoutUtil.setWidthHint(text, getMaxFieldWidth());
		LayoutUtil.setHorizontalGrabbing(text);
		DialogField.createEmptySpace(composite);

		TextFieldNavigationHandler.install(text);
	}

}
