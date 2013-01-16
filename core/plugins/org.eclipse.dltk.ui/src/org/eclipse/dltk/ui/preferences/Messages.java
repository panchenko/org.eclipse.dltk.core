package org.eclipse.dltk.ui.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.dltk.ui.preferences.messages"; //$NON-NLS-1$

	public static String FieldValidators_0;
	public static String ScriptCorePreferenceBlock_globalDLTKSettings;
	public static String ScriptCorePreferenceBlock_coreOptions;
	public static String ScriptCorePreferenceBlock_editOptions;
	public static String ScriptCorePreferenceBlock_debugOptionsOperations;
	public static String ScriptCorePreferencePage_manualReindex;
	public static String ScriptCorePreferencePage_reindex;
	public static String ScriptCorePreferenceBlock_UI_Options;
	public static String ScriptCorePreferenceBlock_CodeAssistTimeout;
	public static String ScriptCorePreferenceBlock_Builder_Options;
	public static String ScriptCorePreferenceBlock_Builder_CircularDependencies;
	public static String EditorPreferencePage_ResourceShowError_InvalidResourceName;

	public static String ScriptCorePreferenceBlock_Warning;
	public static String ScriptCorePreferenceBlock_Error;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
