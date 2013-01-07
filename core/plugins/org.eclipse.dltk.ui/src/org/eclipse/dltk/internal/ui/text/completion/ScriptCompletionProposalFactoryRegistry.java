/*******************************************************************************
 * Copyright (c) 2011 NumberFour AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     NumberFour AG - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.internal.ui.text.completion;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.dltk.ui.text.completion.IScriptCompletionProposalFactory;
import org.eclipse.dltk.utils.NatureExtensionManager;

public class ScriptCompletionProposalFactoryRegistry {

	public static final String EXT_POINT = DLTKUIPlugin.PLUGIN_ID
			+ ".completion";

	private static NatureExtensionManager<IScriptCompletionProposalFactory> manager = null;

	public static synchronized IScriptCompletionProposalFactory[] getFactories(
			String natureId) {
		if (manager == null) {
			manager = new NatureExtensionManager<IScriptCompletionProposalFactory>(
					EXT_POINT, IScriptCompletionProposalFactory.class) {
				@Override
				protected boolean isValidElement(IConfigurationElement element) {
					return "proposalFactory".equals(element.getName());
				}
			};
		}
		return manager.getInstances(natureId);
	}

}
