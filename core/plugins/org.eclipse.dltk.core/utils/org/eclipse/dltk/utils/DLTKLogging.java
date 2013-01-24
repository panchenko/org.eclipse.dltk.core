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
package org.eclipse.dltk.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.dltk.core.DLTKCore;
import org.osgi.service.prefs.BackingStoreException;

/**
 * @since 2.0
 */
public class DLTKLogging {

	private static DLTKDebugOptions debugOptions = null;

	@SuppressWarnings("serial")
	private static class DLTKDebugOptions extends HashSet<String> {

		public DLTKDebugOptions() {
			super();
		}

		public DLTKDebugOptions(DLTKDebugOptions source) {
			super(source);
		}

	}

	private static synchronized DLTKDebugOptions getDebugOptions() {
		if (debugOptions == null) {
			debugOptions = new DLTKDebugOptions();
			final String loggingOptions = InstanceScope.INSTANCE.getNode(
					DLTKCore.PLUGIN_ID).get(DLTKCore.LOGGING_OPTIONS, null);
			if (loggingOptions != null) {
				for (String option : TextUtils.split(loggingOptions,
						DLTKCore.LOGGING_OPTION_SEPARATOR)) {
					debugOptions.add(option);
				}
			}
		}
		return debugOptions;
	}

	private static synchronized void setDebugOptions(
			DLTKDebugOptions debugOptions) {
		DLTKLogging.debugOptions = new DLTKDebugOptions(debugOptions);
	}

	private static DLTKDebugOptions copy(DLTKDebugOptions source) {
		return new DLTKDebugOptions(source);
	}

	public static boolean isEnabled(String option) {
		final DLTKDebugOptions debugOptions = getDebugOptions();
		return debugOptions.contains(option);
	}

	public static void setEnabled(String option, boolean value) {
		final DLTKDebugOptions debugOptions = copy(getDebugOptions());
		if (value) {
			debugOptions.add(option);
		} else {
			debugOptions.remove(option);
		}
		setDebugOptions(debugOptions);
	}

	/**
	 * @param options
	 * @return
	 */
	public static Map<String, Boolean> getState(Collection<String> options) {
		final DLTKDebugOptions debugOptions = getDebugOptions();
		final Map<String, Boolean> result = new HashMap<String, Boolean>();
		for (String option : options) {
			boolean value = debugOptions.contains(option);
			result.put(option, Boolean.valueOf(value));
		}
		return result;
	}

	public static void setState(Map<String, Boolean> state) {
		final DLTKDebugOptions debugOptions = copy(getDebugOptions());
		for (Map.Entry<String, Boolean> entry : state.entrySet()) {
			if (entry.getValue().booleanValue()) {
				debugOptions.add(entry.getKey());
			} else {
				debugOptions.remove(entry.getKey());
			}
		}
		final IEclipsePreferences node = InstanceScope.INSTANCE
				.getNode(DLTKCore.PLUGIN_ID);
		if (!debugOptions.isEmpty()) {
			node.put(DLTKCore.LOGGING_OPTIONS, TextUtils.join(debugOptions,
					DLTKCore.LOGGING_OPTION_SEPARATOR));
		} else {
			node.remove(DLTKCore.LOGGING_OPTIONS);
		}
		try {
			node.flush();
		} catch (BackingStoreException e) {
			DLTKCore.error("Error Saving Logging Options", e); //$NON-NLS-1$
		}
		setDebugOptions(debugOptions);
	}

}
