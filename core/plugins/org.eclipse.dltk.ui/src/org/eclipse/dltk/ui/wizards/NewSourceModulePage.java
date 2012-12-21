/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.ui.wizards;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.compiler.util.Util;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IProjectFragment;
import org.eclipse.dltk.core.IScriptFolder;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.ScriptModelUtil;
import org.eclipse.dltk.core.environment.EnvironmentManager;
import org.eclipse.dltk.core.environment.IEnvironment;
import org.eclipse.dltk.internal.ui.util.SWTUtil;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.ComboDialogField;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.dltk.ui.ModelElementLabelProvider;
import org.eclipse.dltk.ui.dialogs.StatusInfo;
import org.eclipse.dltk.ui.preferences.CodeTemplatesPreferencePage;
import org.eclipse.dltk.ui.text.templates.ICodeTemplateArea;
import org.eclipse.dltk.ui.text.templates.SourceModuleTemplateContext;
import org.eclipse.dltk.ui.util.CodeGeneration;
import org.eclipse.dltk.ui.wizards.ISourceModuleWizard.ICreateContext;
import org.eclipse.dltk.ui.wizards.ISourceModuleWizard.ICreateStep;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Wizard page that acts as a base class for wizard pages that create new source
 * module elements, such as types. The class provides provides an input field
 * for the source module name along with validation methods.
 * 
 * @see NewSourceModuleInPackagePage
 */
public abstract class NewSourceModulePage extends NewContainerWizardPage {

	static final String FILE = "NewSourceModulePage.file"; //$NON-NLS-1$
	private static final String TEMPLATE = "NewSourceModulePage.template"; //$NON-NLS-1$
	static final String EXTENSIONS = "NewSourceModulePage.extensions"; //$NON-NLS-1$

	/**
	 * The status of the last validation of the file (i.e. source module).
	 */
	protected IStatus sourceModuleStatus;
	private final List<IStatus> extensionStatus = new ArrayList<IStatus>();

	private StringDialogField fileDialogField;

	private IStatus fileChanged() {
		StatusInfo status = new StatusInfo();

		if (getFileText().length() == 0) {
			status.setError(Messages.NewSourceModulePage_pathCannotBeEmpty);
		} else {
			if (!Path.EMPTY.isValidSegment(getFileText())) {
				status.setError(Messages.NewSourceModulePage_InvalidFileName);
			}
			if (getScriptFolder() != null) {
				ISourceModule module = getScriptFolder()
						.getSourceModule(getFileName());
				if (module.exists()) {
					status.setError(Messages.NewSourceModulePage_fileAlreadyExists);
				} else {
					IResource resource = module.getResource();
					if (resource != null) {
						URI location = resource.getLocationURI();
						if (location != null) {
							try {
								IFileStore store = EFS.getStore(location);
								if (store.fetchInfo().exists()) {
									status.setError(Messages.NewSourceModulePage_error_TypeNameExistsDifferentCase);
								}
							} catch (CoreException e) {
								status.setError(Messages.NewSourceModulePage_error_uri_location_unkown);
							}
						}
					}
				}
			}
		}

		return status;
	}

	/**
	 * The wizard owning this page is responsible for calling this method with
	 * the current selection. The selection is used to initialize the fields of
	 * the wizard page.
	 * 
	 * @param selection
	 *            used to initialize the fields
	 */
	public void init(IStructuredSelection selection) {
		if (getTemplateArea() != null) {
			createTemplateField();
		}

		IModelElement element = getInitialScriptElement(selection);

		initContainerPage(element);
		updateTemplates();

		updateStatus(new IStatus[] { containerStatus, fileChanged() });
	}

	protected void createFileControls(Composite parent, int nColumns) {
		fileDialogField.doFillIntoGrid(parent, nColumns - 1);
		Text text = fileDialogField.getTextControl(null);
		LayoutUtil.setWidthHint(text, getMaxFieldWidth());
		LayoutUtil.setHorizontalGrabbing(text);
		DialogField.createEmptySpace(parent);
	}

	private static final String NO_TEMPLATE = Util.EMPTY_STRING;
	private Template[] fTemplates;
	private ComboDialogField fTemplateDialogField = null;

	protected void createTemplateControls(Composite parent, int nColumns) {
		fTemplateDialogField.doFillIntoGrid(parent, nColumns - 1);
		LayoutUtil.setWidthHint(fTemplateDialogField.getComboControl(null),
				getMaxFieldWidth());
		final Button configureTemplates = new Button(parent, SWT.PUSH);
		GridData configureData = new GridData(SWT.FILL, SWT.NONE, false, false);
		configureData.widthHint = SWTUtil
				.getButtonWidthHint(configureTemplates);
		configureTemplates.setLayoutData(configureData);
		configureTemplates
				.setText(Messages.NewSourceModulePage_ConfigureTemplates);
		configureTemplates.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String templateName = null;
				final Template template = getSelectedTemplate();
				if (template != null) {
					templateName = template.getName();
				}
				Map<String, Object> data = null;
				if (templateName != null) {
					data = new HashMap<String, Object>();
					data.put(CodeTemplatesPreferencePage.DATA_SELECT_TEMPLATE,
							templateName);
				}
				// TODO handle project specific preferences if any?
				final String prefPageId = getTemplateArea()
						.getTemplatePreferencePageId();
				final PreferenceDialog dialog = PreferencesUtil
						.createPreferenceDialogOn(getShell(), prefPageId,
								new String[] { prefPageId }, data);
				if (dialog.open() == Window.OK) {
					updateTemplates();
				}
			}
		});
	}

	protected void updateTemplates() {
		if (fTemplateDialogField != null) {
			Template selected = getSelectedTemplate();
			String name = selected != null ? selected.getName()
					: getLastUsedTemplateName();
			fTemplates = getApplicableTemplates();
			int idx = 0;
			String[] names = new String[fTemplates.length + 1];
			for (int i = 0; i < fTemplates.length; i++) {
				names[i + 1] = fTemplates[i].getName();
				if (name != null && name.equals(names[i + 1])) {
					idx = i + 1;
				}
			}
			if (idx == 0) {
				final Template template = getDefaultTemplate();
				if (template != null) {
					for (int i = 0; i < fTemplates.length; ++i) {
						if (template == fTemplates[i]) {
							idx = i + 1;
							break;
						}
					}
				}
			}
			names[0] = Messages.NewSourceModulePage_noTemplate;
			fTemplateDialogField.setItems(names);
			fTemplateDialogField.selectItem(idx);
		}
	}

	protected Template getDefaultTemplate() {
		final String defaultTemplateId = getDefaultCodeTemplateId();
		if (defaultTemplateId != null) {
			final ICodeTemplateArea templateArea = getTemplateArea();
			if (templateArea != null) {
				final TemplateStore store = templateArea.getTemplateAccess()
						.getTemplateStore();
				return store.findTemplateById(defaultTemplateId);
			}
		}
		return null;
	}

	protected Template[] getApplicableTemplates() {
		final List<Template> result = new ArrayList<Template>();
		final ICodeTemplateArea templateArea = getTemplateArea();
		if (templateArea != null) {
			final TemplateStore store = templateArea.getTemplateAccess()
					.getTemplateStore();
			final String[] contextTypeIds = getCodeTemplateContextTypeIds();
			for (int i = 0; i < contextTypeIds.length; ++i) {
				Template[] templates = store.getTemplates(contextTypeIds[i]);
				Arrays.sort(templates, new Comparator<Template>() {
					public int compare(Template t0, Template t1) {
						return t0.getName().compareToIgnoreCase(t1.getName());
					}
				});
				for (int j = 0; j < templates.length; ++j) {
					result.add(templates[j]);
				}
			}
		}
		return result.toArray(new Template[result.size()]);
	}

	protected String getLastUsedTemplateKey() {
		return getClass().getName() + "_LAST_USED_TEMPLATE"; //$NON-NLS-1$
	}

	/**
	 * @return the name of the template used in the previous dialog invocation.
	 */
	protected String getLastUsedTemplateName() {
		final IDialogSettings dialogSettings = getDialogSettings();
		return dialogSettings != null ? dialogSettings
				.get(getLastUsedTemplateKey()) : null;
	}

	/**
	 * Saves the name of the last used template.
	 * 
	 * @param name
	 *            the name of a template, or an empty string for no template.
	 */
	protected void saveLastUsedTemplateName(String name) {
		final IDialogSettings dialogSettings = getDialogSettings();
		if (dialogSettings != null) {
			dialogSettings.put(getLastUsedTemplateKey(), name);
		}
	}

	protected Template getSelectedTemplate() {
		if (fTemplateDialogField != null) {
			int index = fTemplateDialogField.getSelectionIndex() - 1;
			if (index >= 0 && index < fTemplates.length) {
				return fTemplates[index];
			}
		}
		return null;
	}

	public NewSourceModulePage() {
		super("wizardPage"); //$NON-NLS-1$
		setTitle(getPageTitle());
		setDescription(getPageDescription());

		sourceModuleStatus = new StatusInfo();

		// fileDialogField
		fileDialogField = new StringDialogField();
		fileDialogField.setLabelText(Messages.NewSourceModulePage_file);
		fileDialogField.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				sourceModuleStatus = fileChanged();
				handleFieldChanged(FILE);
			}
		});
	}

	protected void createTemplateField() {
		fTemplateDialogField = new ComboDialogField(SWT.READ_ONLY);
		fTemplateDialogField
				.setLabelText(Messages.NewSourceModulePage_Template);
	}

	@Override
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		if (fieldName == CONTAINER) {
			sourceModuleStatus = fileChanged();
		}
		if (fieldName == FILE || fieldName == CONTAINER) {
			final IWizard wizard = getWizard();
			if (wizard != null) {
				((NewSourceModuleWizard) wizard).fireFieldChange(fieldName);
			}
		}
		if (fieldName == EXTENSIONS) {
			extensionStatus.clear();
			for (ISourceModuleWizardExtension extension : getExtensions()) {
				final IStatus status = extension.validate();
				if (status != null) {
					extensionStatus.add(status);
				}
			}
		}
		final List<IStatus> statuses = new ArrayList<IStatus>();
		if (containerStatus != null) {
			statuses.add(containerStatus);
		}
		if (sourceModuleStatus != null) {
			statuses.add(sourceModuleStatus);
		}
		statuses.addAll(extensionStatus);
		updateStatus(statuses.toArray(new IStatus[statuses.size()]));
	}

	private static class CreateContext implements ICreateContext {

		final IScriptFolder scriptFolder;
		final ISourceModule sourceModule;

		/**
		 * @param fileName
		 * @param scriptFolder
		 * @param sourceModule
		 */
		public CreateContext(IScriptFolder scriptFolder,
				ISourceModule sourceModule) {
			this.scriptFolder = scriptFolder;
			this.sourceModule = sourceModule;
		}

		public IEnvironment getEnvironment() {
			IEnvironment environment = EnvironmentManager
					.getEnvironment(getScriptFolder());
			if (environment == null) {
				environment = EnvironmentManager.getLocalEnvironment();
			}
			return environment;
		}

		public IScriptFolder getScriptFolder() {
			return scriptFolder;
		}

		public IScriptProject getScriptProject() {
			return getScriptFolder().getScriptProject();
		}

		public ISourceModule getSourceModule() {
			return sourceModule;
		}

		private static class StepEntry {
			final String kind;
			final int priority;
			final ICreateStep step;

			public StepEntry(String kind, int priority, ICreateStep step) {
				this.kind = kind;
				this.priority = priority;
				this.step = step;
			}

		}

		final List<StepEntry> entries = new ArrayList<StepEntry>();

		public void addStep(String kind, int priority, ICreateStep step) {
			entries.add(new StepEntry(kind, priority, step));
		}

		public ICreateStep[] getSteps(String kind) {
			final List<StepEntry> selection = new ArrayList<StepEntry>();
			for (StepEntry entry : entries) {
				if (kind.equals(entry.kind)) {
					selection.add(entry);
				}
			}
			Collections.sort(selection, new Comparator<StepEntry>() {
				public int compare(StepEntry e1, StepEntry e2) {
					return e1.priority - e1.priority;
				}
			});
			final ICreateStep[] steps = new ICreateStep[selection.size()];
			for (int i = 0; i < selection.size(); ++i) {
				steps[i] = selection.get(i).step;
			}
			return steps;
		}

		private String content = Util.EMPTY_STRING;

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

	}

	class InitializeFileContent implements ICreateStep {

		public void execute(ICreateContext context, IProgressMonitor monitor)
				throws CoreException {
			context.setContent(getFileContent(context.getSourceModule()));
		}

	}

	static class CreateSourceModuleStep implements ICreateStep {

		public void execute(ICreateContext context, IProgressMonitor monitor)
				throws CoreException {
			context.getScriptFolder().createSourceModule(
					context.getSourceModule().getElementName(),
					context.getContent(), true, monitor);
		}

	}

	/**
	 * Creates new source module in current script folder. This method is called
	 * by the wizard on finish.
	 */
	public ISourceModule createFile(IProgressMonitor monitor)
			throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		final String fileName = getFileName();
		final ISourceModule module = getScriptFolder()
				.getSourceModule(fileName);
		CreateContext context = new CreateContext(getScriptFolder(), module);
		context.addStep(ICreateStep.KIND_PREPARE, 0,
				new InitializeFileContent());
		context.addStep(ICreateStep.KIND_EXECUTE, 0,
				new CreateSourceModuleStep());
		for (ISourceModuleWizardExtension extension : getExtensions()) {
			extension.prepare(context);
		}
		final List<ICreateStep> steps = new ArrayList<ICreateStep>();
		Collections.addAll(steps, context.getSteps(ICreateStep.KIND_PREPARE));
		Collections.addAll(steps, context.getSteps(ICreateStep.KIND_EXECUTE));
		Collections.addAll(steps, context.getSteps(ICreateStep.KIND_FINALIZE));
		for (ICreateStep step : steps) {
			step.execute(context, monitor);
		}
		return module;
	}

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		final int nColumns = 3;

		Composite composite = new Composite(parent, SWT.NONE);
		GridData layoutData = new GridData(SWT.FILL, SWT.NONE, true, false);
		composite.setLayoutData(layoutData);

		composite.setFont(parent.getFont());

		GridLayout layout = new GridLayout();
		layout.numColumns = nColumns;
		composite.setLayout(layout);

		createContentControls(composite, nColumns);

		setControl(composite);
		Dialog.applyDialogFont(composite);
	}

	private static class WorkspaceMode implements ISourceModuleWizardMode {

		public WorkspaceMode() {
		}

		public void createControl(Composite parent, int columns) {
			// empty
		}

		public String getId() {
			return ISourceModuleWizard.MODE_WORKSPACE;
		}

		public String getName() {
			return "&workspace";
		}

		public void setEnabled(boolean enabled) {
			// empty
		}

	}

	private static class ModeEntry {
		final SelectionButtonDialogField field;
		final ISourceModuleWizardMode template;

		public ModeEntry(SelectionButtonDialogField field,
				ISourceModuleWizardMode template) {
			this.field = field;
			this.template = template;
		}

		private Boolean enabled = null;

		public boolean getSelection() {
			return enabled != null && enabled.booleanValue();
		}

		public void setSelection(boolean value) {
			if (enabled == null || value != enabled.booleanValue()) {
				if (field != null) {
					field.setSelection(value);
				}
				template.setEnabled(value);
				enabled = Boolean.valueOf(value);
			}
		}

	}

	final List<ModeEntry> modes = new ArrayList<ModeEntry>();

	private ModeEntry getActiveMode() {
		for (ModeEntry f : modes) {
			if (f.getSelection()) {
				return f;
			}
		}
		if (!modes.isEmpty()) {
			return modes.get(0);
		}
		throw new IllegalStateException("No modes");
	}

	private final IDialogFieldListener modeSelectionUpdater = new IDialogFieldListener() {
		public void dialogFieldChanged(DialogField field) {
			ModeEntry selection = null;
			for (ModeEntry f : modes) {
				if (f.field != null && f.field.isSelected()) {
					selection = f;
				}
			}
			if (selection != null) {
				for (ModeEntry f : modes) {
					f.setSelection(f == selection);
				}
			}
			handleFieldChanged(TEMPLATE);
		}
	};

	/**
	 * Creates content controls on the specified composite.
	 * 
	 * @param composite
	 * @param nColumns
	 */
	protected void createContentControls(Composite composite, final int nColumns) {
		createContainerControls(composite, nColumns);

		// createPackageControls(composite, nColumns);
		createFileControls(composite, nColumns);
		final List<ISourceModuleWizardMode> modes = new ArrayList<ISourceModuleWizardMode>();
		for (ISourceModuleWizardExtension extension : getExtensions()) {
			modes.addAll(extension.getModes());
		}
		if (!modes.isEmpty()) {
			final Group contents = new Group(composite, SWT.NONE);
			contents.setText("Location");
			GridData ggd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
			ggd.horizontalSpan = nColumns;
			contents.setLayoutData(ggd);
			contents.setLayout(new GridLayout(nColumns, false));
			ModeEntry wsEntry = addMode(contents, nColumns, new WorkspaceMode());
			((GridData) wsEntry.field.getSelectionButton().getLayoutData()).horizontalSpan = nColumns;
			for (ISourceModuleWizardMode template : modes) {
				addMode(contents, nColumns, template);
			}
			for (ModeEntry modeEntry : this.modes) {
				modeEntry.field.setDialogFieldListener(modeSelectionUpdater);
			}
		} else {
			final ModeEntry entry = new ModeEntry(null, new WorkspaceMode());
			entry.setSelection(true);
			this.modes.add(entry);
		}
		if (fTemplateDialogField != null) {
			createTemplateControls(composite, nColumns);
		}
	}

	private List<ISourceModuleWizardExtension> getExtensions() {
		return ((NewSourceModuleWizard) getWizard()).getExtensions();
	}

	private ModeEntry addMode(Composite parent, int nColumns,
			ISourceModuleWizardMode template) {
		SelectionButtonDialogField field = new SelectionButtonDialogField(
				SWT.RADIO);
		field.setLabelText(template.getName());
		field.doFillIntoGrid(parent, 1);
		// Composite parent = new Composite(contents, SWT.NONE);
		// GridLayout layout = new GridLayout(3, false);
		// layout.marginHeight = 0;
		// layout.marginWidth = 0;
		// parent.setLayout(layout);
		// GridData ld = new GridData(SWT.FILL, SWT.DEFAULT, true,
		// false);
		// ld.horizontalSpan = nColumns - 1;
		// parent.setLayoutData(ld);
		template.createControl(parent, nColumns - 1);
		final ModeEntry entry = new ModeEntry(field, template);
		this.modes.add(entry);
		String activeMode = ((NewSourceModuleWizard) getWizard()).getMode();
		if (activeMode == null) {
			activeMode = ISourceModuleWizard.MODE_WORKSPACE;
		}
		entry.setSelection(template.getId().equals(activeMode));
		entry.field.setEnabled(((NewSourceModuleWizard) getWizard())
				.isModeEnabled(template.getId()));
		return entry;
	}

	protected String getFileText() {
		return fileDialogField.getText();
	}

	/**
	 * Sets the file input field's text (simple name without path or extension)
	 * to the given value. Method doesn't update the model.
	 * 
	 * @param name
	 *            the new file name
	 * @param canBeModified
	 *            if <code>true</code> the type name field is editable;
	 *            otherwise it is read-only.
	 */
	public void setFileText(String name, boolean canBeModified) {
		fileDialogField.setText(name);
		fileDialogField.setEnabled(canBeModified);
	}

	protected String getFileName() {
		final String fileText = getFileText();

		String[] extensions = getFileExtensions();
		for (int i = 0; i < extensions.length; ++i) {
			String extension = extensions[i];
			if (extension.length() > 0 && fileText.endsWith("." + extension)) { //$NON-NLS-1$
				return fileText;
			}
		}

		return fileText + "." + extensions[0]; //$NON-NLS-1$
	}

	protected String[] getFileExtensions() {
		String requiredNature = getRequiredNature();

		IDLTKLanguageToolkit toolkit = DLTKLanguageManager
				.getLanguageToolkit(requiredNature);
		String[] extensions = ScriptModelUtil.getFileExtensions(toolkit);
		if (extensions != null) {
			return extensions;
		}

		return new String[] { Util.EMPTY_STRING };
	}

	protected IScriptFolder chooseScriptFolder() {
		ILabelProvider labelProvider = new ModelElementLabelProvider(
				ModelElementLabelProvider.SHOW_DEFAULT);

		ElementListSelectionDialog dialog = new ElementListSelectionDialog(
				getShell(), labelProvider);

		dialog.setIgnoreCase(false);
		dialog.setTitle(Messages.NewSourceModulePage_selectScriptFolder);
		dialog.setMessage(Messages.NewSourceModulePage_selectScriptFolder);
		dialog.setEmptyListMessage(Messages.NewSourceModulePage_noFoldersAvailable);

		IProjectFragment projectFragment = getProjectFragment();
		if (projectFragment != null) {
			try {
				dialog.setElements(projectFragment.getChildren());
			} catch (ModelException e) {
				if (DLTKCore.DEBUG) {
					e.printStackTrace();
				}
			}
		}

		dialog.setHelpAvailable(false);

		if (getScriptFolder() != null) {
			dialog.setInitialSelections(new Object[] { getScriptFolder() });
		}

		if (dialog.open() == Window.OK) {
			Object element = dialog.getFirstResult();
			if (element instanceof IScriptFolder) {
				return (IScriptFolder) element;
			}
		}

		return null;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			setFocus();
		}
	}

	protected void setFocus() {
		fileDialogField.setFocus();
	}

	protected abstract String getPageTitle();

	protected abstract String getPageDescription();

	protected ICodeTemplateArea getTemplateArea() {
		return null;
	}

	protected String[] getCodeTemplateContextTypeIds() {
		return null;
	}

	protected String getDefaultCodeTemplateId() {
		return null;
	}

	protected String getFileContent(ISourceModule module) throws CoreException {
		final ICodeTemplateArea templateArea = getTemplateArea();
		if (templateArea != null) {
			final Template template = getSelectedTemplate();
			saveLastUsedTemplateName(template != null ? template.getName()
					: NO_TEMPLATE);
			if (template != null) {
				final TemplateContextType contextType = templateArea
						.getTemplateAccess().getContextTypeRegistry()
						.getContextType(template.getContextTypeId());
				// TODO introduce a way to create context by contextType
				final SourceModuleTemplateContext context = new SourceModuleTemplateContext(
						contextType,
						CodeGeneration.getLineDelimiterUsed(module));
				// String fileComment = getFileComment(file, lineDelimiter);
				// context.setVariable(CodeTemplateContextType.FILE_COMMENT,
				//					fileComment != null ? fileComment : ""); //$NON-NLS-1$
				// ICProject cproject = CoreModel.getDefault().create(
				// file.getProject());
				// String includeGuardSymbol = generateIncludeGuardSymbol(file
				// .getName(), cproject);
				// context.setVariable(CodeTemplateContextType.INCLUDE_GUARD_SYMBOL,
				//					includeGuardSymbol != null ? includeGuardSymbol : ""); //$NON-NLS-1$
				context.setSourceModuleVariables(module);
				final String[] fullLine = {};
				final String result = CodeGeneration.evaluateTemplate(context,
						template, fullLine);
				return result != null ? result : Util.EMPTY_STRING;
			}
		}
		return getFileContent();
	}

	protected String getFileContent() {
		return Util.EMPTY_STRING;
	}

	protected IEnvironment getEnvironment() {
		final IProjectFragment fragment = getProjectFragment();
		if (fragment != null) {
			return EnvironmentManager.getEnvironment(fragment
					.getScriptProject().getProject());
		}
		return null;
	}

	String getMode() {
		return getActiveMode().template.getId();
	}

	void setMode(String mode) {
		for (ModeEntry entry : modes) {
			entry.setSelection(mode != null
					&& mode.equals(entry.template.getId()));
		}
	}

	void enableMode(String mode, boolean enable) {
		boolean updateSelection = false;
		for (ModeEntry entry : modes) {
			if (mode.equals(entry.template.getId()) && entry.field != null) {
				if (entry.getSelection() && !enable) {
					updateSelection = true;
					entry.setSelection(false);
				}
				entry.field.setEnabled(enable);
			}
		}
		if (updateSelection) {
			for (ModeEntry entry : modes) {
				if (entry.field == null || entry.field.isEnabled()) {
					entry.setSelection(true);
					break;
				}
			}
		}
	}

}
