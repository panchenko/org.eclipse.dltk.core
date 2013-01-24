package org.eclipse.dltk.internal.ui.preferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.dltk.internal.ui.util.CoreUtility;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.dltk.ui.preferences.IPreferenceChangeRebuildPrompt;
import org.eclipse.dltk.ui.preferences.PreferenceKey;
import org.eclipse.dltk.ui.util.IStatusChangeListener;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.preferences.IWorkingCopyManager;
import org.eclipse.ui.preferences.WorkingCopyManager;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Direct port from the jdt ui, this class should not be extended by anyone but
 * the internal dltk ui.
 * 
 * @see org.eclipse.dltk.ui.preferences.AbstractOptionsBlock
 */
public abstract class OptionsConfigurationBlock {

	private static final String REBUILD_COUNT_KEY = "preferences_build_requested"; //$NON-NLS-1$

	private final IStatusChangeListener fContext;
	protected final IProject fProject; // project or null
	private PreferenceKey[] fAllKeys;

	private final IScopeContext[] fLookupOrder;

	private Shell fShell;

	private final IWorkingCopyManager fManager;
	private final IWorkbenchPreferenceContainer fContainer;
	private boolean fInitialized;

	/**
	 * null when project specific settings are turned off
	 */
	private Map<PreferenceKey, String> fDisabledProjectSettings;

	// used to prevent multiple dialogs that ask for a rebuild
	private int fRebuildCount;

	public OptionsConfigurationBlock(IStatusChangeListener context,
			IProject project, PreferenceKey[] allKeys,
			IWorkbenchPreferenceContainer container) {
		fContext = context;
		fProject = project;
		fAllKeys = allKeys;
		fContainer = container;
		if (container == null) {
			fManager = new WorkingCopyManager();
		} else {
			fManager = container.getWorkingCopyManager();
		}

		if (fProject != null) {
			fLookupOrder = new IScopeContext[] { new ProjectScope(fProject),
					InstanceScope.INSTANCE, DefaultScope.INSTANCE };
		} else {
			fLookupOrder = new IScopeContext[] { InstanceScope.INSTANCE,
					DefaultScope.INSTANCE };
		}

		testIfOptionsComplete(allKeys);

		fRebuildCount = getRebuildCount();
	}

	protected void initializeProjectSettings() {
		if (fInitialized) {
			return;
		}
		fInitialized = true;
		if (fProject == null || hasProjectSpecificOptions(fProject)) {
			fDisabledProjectSettings = null;
		} else {
			fDisabledProjectSettings = new HashMap<PreferenceKey, String>();
			for (PreferenceKey curr : getPreferenceKeys()) {
				fDisabledProjectSettings.put(curr,
						curr.getStoredValue(fLookupOrder, false, fManager));
			}
		}
	}

	protected void addKeys(List<PreferenceKey> keys) {
		Assert.isLegal(!fInitialized);
		final Set<PreferenceKey> all = new LinkedHashSet<PreferenceKey>();
		Collections.addAll(all, fAllKeys);
		all.addAll(keys);
		if (all.size() != fAllKeys.length) {
			fAllKeys = all.toArray(new PreferenceKey[all.size()]);
		}
	}

	protected final IWorkbenchPreferenceContainer getPreferenceContainer() {
		return fContainer;
	}

	protected static PreferenceKey getKey(String plugin, String key) {
		return new PreferenceKey(plugin, key);
	}

	private void testIfOptionsComplete(PreferenceKey[] allKeys) {
		for (PreferenceKey key : allKeys) {
			validateValuePresenceFor(key);
		}
	}

	protected void validateValuePresenceFor(PreferenceKey key) {
		if (key.getStoredValue(fLookupOrder, false, fManager) == null) {
			DLTKUIPlugin
					.logErrorMessage("preference option missing: " + key + " (" + this.getClass().getName() + ')'); //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	private int getRebuildCount() {
		return fManager.getWorkingCopy(
				new DefaultScope().getNode(DLTKUIPlugin.PLUGIN_ID)).getInt(
				REBUILD_COUNT_KEY, 0);
	}

	private void incrementRebuildCount() {
		fRebuildCount++;
		fManager.getWorkingCopy(
				new DefaultScope().getNode(DLTKUIPlugin.PLUGIN_ID)).putInt(
				REBUILD_COUNT_KEY, fRebuildCount);
	}

	protected PreferenceKey[] getPreferenceKeys() {
		return fAllKeys;
	}

	public boolean hasProjectSpecificOptions(IProject project) {
		if (project != null) {
			IScopeContext projectContext = new ProjectScope(project);
			PreferenceKey[] allKeys = getPreferenceKeys();
			for (int i = 0; i < allKeys.length; i++) {
				if (allKeys[i].getStoredValue(projectContext, fManager) != null) {
					return true;
				}
			}
		}
		return false;
	}

	protected Shell getShell() {
		return fShell;
	}

	protected void setShell(Shell shell) {
		fShell = shell;
	}

	protected abstract Control createContents(Composite parent);

	protected boolean checkValue(PreferenceKey key, String value) {
		return value.equals(getValue(key));
	}

	protected String getValue(PreferenceKey key) {
		if (fDisabledProjectSettings != null) {
			return fDisabledProjectSettings.get(key);
		}
		return key.getStoredValue(fLookupOrder, false, fManager);
	}

	protected boolean getBooleanValue(PreferenceKey key) {
		return Boolean.valueOf(getValue(key)).booleanValue();
	}

	protected int getIntValue(PreferenceKey key) {
		try {
			return Integer.parseInt(getValue(key));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	protected String setValue(PreferenceKey key, String value) {
		if (fDisabledProjectSettings != null) {
			return fDisabledProjectSettings.put(key, value);
		}
		String oldValue = getValue(key);
		key.setStoredValue(fLookupOrder[0], value, fManager);
		return oldValue;
	}

	protected String setValue(PreferenceKey key, boolean value) {
		return setValue(key, String.valueOf(value));
	}

	private boolean getChanges(IScopeContext currContext,
			List<PreferenceKey> changedSettings) {
		// complete when project settings are enabled
		boolean completeSettings = fProject != null
				&& fDisabledProjectSettings == null;
		boolean needsBuild = false;

		PreferenceKey[] allKeys = getPreferenceKeys();
		for (int i = 0; i < allKeys.length; i++) {
			PreferenceKey key = allKeys[i];
			String oldVal = key.getStoredValue(currContext, null);
			String val = key.getStoredValue(currContext, fManager);
			if (val == null) {
				if (oldVal != null) {
					changedSettings.add(key);
					needsBuild |= !oldVal.equals(key.getStoredValue(
							fLookupOrder, true, fManager));
				} else if (completeSettings) {
					key.setStoredValue(currContext,
							key.getStoredValue(fLookupOrder, true, fManager),
							fManager);
					changedSettings.add(key);
					// no build needed
				}
			} else if (!val.equals(oldVal)) {
				changedSettings.add(key);
				needsBuild |= oldVal != null
						|| !val.equals(key.getStoredValue(fLookupOrder, true,
								fManager));
			}
		}
		return needsBuild;
	}

	public void useProjectSpecificSettings(boolean enable) {
		boolean hasProjectSpecificOption = fDisabledProjectSettings == null;
		if (enable != hasProjectSpecificOption && fProject != null) {
			PreferenceKey[] allKeys = getPreferenceKeys();
			if (enable) {
				for (int i = 0; i < allKeys.length; i++) {
					PreferenceKey curr = allKeys[i];
					String val = fDisabledProjectSettings.get(curr);
					curr.setStoredValue(fLookupOrder[0], val, fManager);
				}
				fDisabledProjectSettings = null;
			} else {
				fDisabledProjectSettings = new HashMap<PreferenceKey, String>();
				for (int i = 0; i < allKeys.length; i++) {
					PreferenceKey curr = allKeys[i];
					String oldSetting = curr.getStoredValue(fLookupOrder,
							false, fManager);
					fDisabledProjectSettings.put(curr, oldSetting);
					// clear project settings
					curr.setStoredValue(fLookupOrder[0], null, fManager);
				}
			}
		}
	}

	public boolean areSettingsEnabled() {
		return fDisabledProjectSettings == null || fProject == null;
	}

	public boolean performOk() {
		return processChanges(fContainer);
	}

	public boolean performApply() {
		// apply directly
		return processChanges(null);
	}

	protected boolean processChanges(IWorkbenchPreferenceContainer container) {
		IScopeContext currContext = fLookupOrder[0];

		List<PreferenceKey> changedOptions = new ArrayList<PreferenceKey>();
		boolean needsBuild = getChanges(currContext, changedOptions);
		if (changedOptions.isEmpty()) {
			return true;
		}
		if (needsBuild) {
			int count = getRebuildCount();
			if (count > fRebuildCount) {
				needsBuild = false; // build already requested
				fRebuildCount = count;
			}
		}

		boolean doBuild = false;
		if (needsBuild) {
			IPreferenceChangeRebuildPrompt prompt = getPreferenceChangeRebuildPrompt(
					fProject == null, changedOptions);
			if (prompt != null) {
				MessageDialog dialog = new MessageDialog(getShell(),
						prompt.getTitle(), null, prompt.getMessage(),
						MessageDialog.QUESTION, new String[] {
								IDialogConstants.YES_LABEL,
								IDialogConstants.NO_LABEL,
								IDialogConstants.CANCEL_LABEL }, 2);
				int res = dialog.open();
				if (res == 0) {
					doBuild = true;
				} else if (res != 1) {
					return false; // cancel pressed
				}
			}
		}
		if (container != null) {
			// no need to apply the changes to the original store: will be done
			// by the page container
			if (doBuild) { // post build
				incrementRebuildCount();
				for (Job job : createBuildJobs(fProject)) {
					container.registerUpdateJob(job);
				}
			}
		} else {
			// apply changes right away
			try {
				fManager.applyChanges();
			} catch (BackingStoreException e) {
				DLTKUIPlugin.log(e);
				return false;
			}
			if (doBuild) {
				for (Job job : createBuildJobs(fProject)) {
					job.schedule();
				}
			}

		}
		return true;
	}

	protected Job[] createBuildJobs(IProject project) {
		return new Job[] { CoreUtility.getBuildJob(project) };
	}

	public void performDefaults() {
		PreferenceKey[] allKeys = getPreferenceKeys();
		for (int i = 0; i < allKeys.length; i++) {
			PreferenceKey curr = allKeys[i];
			String origValue = curr
					.getStoredValue(fLookupOrder, true, fManager);
			setValue(curr, origValue);
		}
	}

	/**
	 * Returns the prompt that should be used in the popup box that indicates a
	 * project build needs to occur.
	 * 
	 * <p>
	 * Default implementation returns <code>null</code>. Clients should override
	 * to return context appropriate message.
	 * </p>
	 * 
	 * @param workspaceSettings
	 *            <code>true</code> if workspace settings were changed,
	 *            <code>false</code> if project settings were changed
	 * @param changedOptions
	 *            options that were actually changed. Could be used to test if
	 *            particular option was changed.
	 * @return
	 */
	protected IPreferenceChangeRebuildPrompt getPreferenceChangeRebuildPrompt(
			boolean workspaceSettings, Collection<PreferenceKey> changedOptions) {
		return null;
	}

	/**
	 * @deprecated
	 * @see #getPreferenceChangeRebuildPrompt(boolean, Collection)
	 */
	protected final String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		return null;
	}

	public void dispose() {
	}

	protected void statusChanged(IStatus status) {
		fContext.statusChanged(status);
	}

	protected ExpandableComposite createStyleSection(Composite parent,
			String label, int nColumns) {
		ExpandableComposite excomposite = new ExpandableComposite(parent,
				SWT.NONE, ExpandableComposite.TWISTIE
						| ExpandableComposite.CLIENT_INDENT);
		excomposite.setText(label);
		excomposite.setExpanded(false);
		excomposite.setFont(JFaceResources.getFontRegistry().getBold(
				JFaceResources.DIALOG_FONT));
		excomposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
				true, false, nColumns, 1));
		excomposite.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				expandedStateChanged((ExpandableComposite) e.getSource());
			}
		});
		fExpandedComposites.add(excomposite);
		makeScrollableCompositeAware(excomposite);
		return excomposite;
	}

	private List<ExpandableComposite> fExpandedComposites = new ArrayList<ExpandableComposite>();

	private static final String SETTINGS_EXPANDED = "expanded"; //$NON-NLS-1$

	protected void restoreSectionExpansionStates(IDialogSettings settings) {
		for (int i = 0; i < fExpandedComposites.size(); i++) {
			ExpandableComposite excomposite = fExpandedComposites.get(i);
			if (settings == null) {
				excomposite.setExpanded(i == 0); // only expand the first node
													// by default
			} else {
				excomposite.setExpanded(settings.getBoolean(SETTINGS_EXPANDED
						+ String.valueOf(i)));
			}
		}
	}

	protected void storeSectionExpansionStates(IDialogSettings settings) {
		for (int i = 0; i < fExpandedComposites.size(); i++) {
			ExpandableComposite curr = fExpandedComposites.get(i);
			settings.put(SETTINGS_EXPANDED + String.valueOf(i),
					curr.isExpanded());
		}
	}

	private void makeScrollableCompositeAware(Control control) {
		ScrolledPageContent parentScrolledComposite = getParentScrolledComposite(control);
		if (parentScrolledComposite != null) {
			parentScrolledComposite.adaptChild(control);
		}
	}

	protected ScrolledPageContent getParentScrolledComposite(Control control) {
		Control parent = control.getParent();
		while (!(parent instanceof ScrolledPageContent) && parent != null) {
			parent = parent.getParent();
		}
		if (parent instanceof ScrolledPageContent) {
			return (ScrolledPageContent) parent;
		}
		return null;
	}

	protected final void expandedStateChanged(ExpandableComposite expandable) {
		ScrolledPageContent parentScrolledComposite = getParentScrolledComposite(expandable);
		if (parentScrolledComposite != null) {
			parentScrolledComposite.reflow(true);
		}
	}

}