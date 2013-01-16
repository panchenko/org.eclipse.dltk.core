/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.ui.preferences;

import org.eclipse.dltk.ui.DLTKUIPlugin;

public class ScriptCorePreferencePage extends
		AbstractConfigurationBlockPreferencePage {

	@Override
	protected IPreferenceConfigurationBlock createConfigurationBlock(
			OverlayPreferenceStore store) {
		return new ScriptCorePreferenceBlock(store, this);
	}

	@Override
	protected String getHelpId() {
		return null;
	}

	@Override
	protected void setDescription() {
		setDescription(Messages.ScriptCorePreferenceBlock_globalDLTKSettings);
	}

	@Override
	protected void setPreferenceStore() {
		setPreferenceStore(DLTKUIPlugin.getDefault().getPreferenceStore());
	}
}
