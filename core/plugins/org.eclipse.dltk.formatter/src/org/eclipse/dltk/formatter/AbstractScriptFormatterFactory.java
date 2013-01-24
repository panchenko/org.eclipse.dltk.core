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
package org.eclipse.dltk.formatter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.dltk.core.DLTKContributedExtension;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IPreferencesLookupDelegate;
import org.eclipse.dltk.core.IPreferencesSaveDelegate;
import org.eclipse.dltk.formatter.profile.BuiltInProfile;
import org.eclipse.dltk.formatter.profile.GeneralProfileVersioner;
import org.eclipse.dltk.formatter.profile.ProfileManager;
import org.eclipse.dltk.formatter.profile.ProfileStore;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.dltk.ui.formatter.FormatterMessages;
import org.eclipse.dltk.ui.formatter.IFormatterModifyDialog;
import org.eclipse.dltk.ui.formatter.IFormatterModifyDialogOwner;
import org.eclipse.dltk.ui.formatter.IProfile;
import org.eclipse.dltk.ui.formatter.IProfileManager;
import org.eclipse.dltk.ui.formatter.IProfileStore;
import org.eclipse.dltk.ui.formatter.IProfileVersioner;
import org.eclipse.dltk.ui.formatter.IScriptFormatterFactory;
import org.eclipse.dltk.ui.formatter.ScriptFormatterManager;
import org.eclipse.dltk.ui.preferences.PreferenceKey;

/**
 * Abstract base class for the {@link IScriptFormatterFactory} implementations.
 */
public abstract class AbstractScriptFormatterFactory extends
		DLTKContributedExtension implements IScriptFormatterFactory {

	protected IProfileVersioner versioner;

	protected String getDefaultProfileID() {
		StringBuffer buffer = new StringBuffer();
		String lang = getLanguage();
		if (lang != null && lang.length() > 0) {
			buffer.append("org.eclipse.dltk."); //$NON-NLS-1$
			buffer.append(lang.toLowerCase());
		} else {
			buffer.append(getClass().getName());
		}

		buffer.append(".formatter.profiles.default"); //$NON-NLS-1$
		return buffer.toString();
	}

	protected String getDefaultProfileName() {
		return FormatterMessages.AbstractScriptFormatterFactory_defaultProfileName;
	}

	public List<IProfile> getBuiltInProfiles() {
		final List<IProfile> profiles = new ArrayList<IProfile>();

		final IProfileVersioner versioner = getProfileVersioner();
		final Map<String, String> defaults = loadDefaultSettings();
		profiles.addAll(loadContributedProfiles(versioner, defaults));
		final BuiltInProfile profile = new BuiltInProfile(
				getDefaultProfileID(), getDefaultProfileName(), defaults, 1000,
				getId(), versioner.getCurrentVersion());
		profiles.add(profile);
		return profiles;
	}

	private Collection<? extends IProfile> loadContributedProfiles(
			IProfileVersioner versioner, Map<String, String> defaults) {
		final IConfigurationElement[] elements = Platform
				.getExtensionRegistry().getConfigurationElementsFor(
						ScriptFormatterManager.EXTPOINT);
		final ProfileStore profileStore = new ProfileStore(versioner, defaults);
		final List<IProfile> profiles = new ArrayList<IProfile>();
		for (IConfigurationElement element : elements) {
			if ("profiles".equals(element.getName())) {
				final URL url = Platform.getBundle(
						element.getContributor().getName()).getEntry(
						element.getAttribute("resource"));
				if (url == null) {
					// TODO log
					continue;
				}
				int priority;
				try {
					priority = Integer.parseInt(element
							.getAttribute("priority"));
				} catch (NumberFormatException e) {
					priority = 0;
				}
				try {
					final InputStream stream = url.openStream();
					try {
						final List<IProfile> loaded = profileStore
								.readProfilesFromStream(stream);
						for (IProfile profile : loaded) {
							if (getId().equals(profile.getFormatterId()))
								profiles.add(new BuiltInProfile(
										profile.getID(), profile.getName(),
										profile.getSettings(), priority,
										profile.getFormatterId(), profile
												.getVersion()));
						}
					} finally {
						try {
							stream.close();
						} catch (IOException e) {
							// ignore
						}
					}
				} catch (IOException e) {
					DLTKUIPlugin.log(e);
				} catch (CoreException e) {
					DLTKUIPlugin.log(e);
				}
			}
		}
		return profiles;
	}

	protected PreferenceKey getProfilesKey() {
		return null;
	}

	public List<IProfile> getCustomProfiles() {
		final PreferenceKey profilesKey = getProfilesKey();
		if (profilesKey != null) {
			final String profilesSource = profilesKey
					.getStoredValue(InstanceScope.INSTANCE);
			if (profilesSource != null && profilesSource.length() > 0) {
				final IProfileStore store = getProfileStore();
				try {
					return ((ProfileStore) store)
							.readProfilesFromString(profilesSource);
				} catch (CoreException e) {
					DLTKFormatterPlugin.error(e);
				}
			}
		}
		return Collections.emptyList();
	}

	public void saveCustomProfiles(List<IProfile> profiles) {
		final PreferenceKey profilesKey = getProfilesKey();
		if (profilesKey != null) {
			final IProfileStore store = getProfileStore();
			try {
				String value = ((ProfileStore) store).writeProfiles(profiles);
				profilesKey.setStoredValue(InstanceScope.INSTANCE, value);
			} catch (CoreException e) {
				DLTKFormatterPlugin.error(e);
			}
		}
	}

	public Map<String, String> loadDefaultSettings() {
		Map<String, String> settings = new HashMap<String, String>();
		PreferenceKey[] keys = getPreferenceKeys();
		if (keys != null) {
			for (int i = 0; i < keys.length; i++) {
				PreferenceKey key = keys[i];
				String name = key.getName();
				IEclipsePreferences preferences = DefaultScope.INSTANCE
						.getNode(key.getQualifier());
				String value = preferences.get(name, null);
				if (value != null)
					settings.put(name, value);
			}
		}
		return settings;
	}

	public Map<String, String> retrievePreferences(
			IPreferencesLookupDelegate delegate) {
		final PreferenceKey activeProfileKey = getActiveProfileKey();
		if (activeProfileKey != null) {
			final String profileId = delegate
					.getString(activeProfileKey.getQualifier(),
							activeProfileKey.getName());
			if (profileId != null && profileId.length() != 0) {
				for (IProfile profile : getBuiltInProfiles()) {
					if (profileId.equals(profile.getID())) {
						return profile.getSettings();
					}
				}
				for (IProfile profile : getCustomProfiles()) {
					if (profileId.equals(profile.getID())) {
						return profile.getSettings();
					}
				}
			}
		}
		final Map<String, String> result = new HashMap<String, String>();
		final PreferenceKey[] keys = getPreferenceKeys();
		if (keys != null) {
			for (int i = 0; i < keys.length; ++i) {
				final PreferenceKey prefKey = keys[i];
				final String key = prefKey.getName();
				result.put(key, delegate.getString(prefKey.getQualifier(), key));
			}
		}
		return result;
	}

	/**
	 * @since 2.0
	 */
	public Map<String, String> changeToIndentingOnly(
			Map<String, String> preferences) {
		return preferences;
	}

	public void savePreferences(Map<String, String> preferences,
			IPreferencesSaveDelegate delegate) {
		final PreferenceKey[] keys = getPreferenceKeys();
		if (keys != null) {
			for (int i = 0; i < keys.length; ++i) {
				final PreferenceKey prefKey = keys[i];
				final String key = prefKey.getName();
				if (preferences.containsKey(key)) {
					final String value = preferences.get(key);
					delegate.setString(prefKey.getQualifier(), key, value);
				}
			}
		}

		final PreferenceKey activeProfileKey = getActiveProfileKey();
		if (activeProfileKey != null
				&& preferences.containsKey(activeProfileKey.getName())) {
			final String value = preferences.get(activeProfileKey.getName());
			delegate.setString(activeProfileKey.getQualifier(),
					activeProfileKey.getName(), value);
		}
	}

	public IProfileVersioner getProfileVersioner() {
		if (versioner == null)
			versioner = createProfileVersioner();
		return versioner;
	}

	public IProfileStore getProfileStore() {
		return new ProfileStore(getProfileVersioner(), loadDefaultSettings());
	}

	protected IProfileVersioner createProfileVersioner() {
		return new GeneralProfileVersioner(getId());
	}

	/*
	 * @see IScriptFormatterFactory#createProfileManager(java.util.List)
	 */
	public IProfileManager createProfileManager(List<IProfile> profiles) {
		return new ProfileManager(profiles);
	}

	public boolean isValid() {
		return true;
	}

	public URL getPreviewContent() {
		return null;
	}

	public IFormatterModifyDialog createDialog(
			IFormatterModifyDialogOwner dialogOwner, IProfileManager manager) {
		return null;
	}

	private String getLanguage() {
		IDLTKLanguageToolkit toolkit = DLTKLanguageManager
				.getLanguageToolkit(getNatureId());
		if (toolkit != null)
			return toolkit.getLanguageName();
		return null;
	}
}
