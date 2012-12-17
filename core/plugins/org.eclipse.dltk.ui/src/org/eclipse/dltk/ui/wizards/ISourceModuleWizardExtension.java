/*******************************************************************************
 * Copyright (c) 2010 xored software, Inc.  
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html  
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.ui.wizards;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.dltk.annotations.ExtensionPoint;
import org.eclipse.dltk.ui.wizards.ISourceModuleWizard.ICreateContext;

/**
 * Extensions for source module wizard should implement this interface.
 * 
 * @since 2.0
 */
@ExtensionPoint(point = NewSourceModuleWizard.WizardExtensionManager.EXTENSION_POINT, element = "wizardExtension", attribute = "class")
public interface ISourceModuleWizardExtension {

	/**
	 * Initializes the extension after creation. It gives the extension a way to
	 * give up by returning <code>false</code>.
	 * 
	 * @return
	 */
	boolean start(ISourceModuleWizard wizard);

	/**
	 * Returns the list of different wizard modes
	 * 
	 * @return
	 */
	List<ISourceModuleWizardMode> getModes();

	/**
	 * Initializes this extension and allows to specify initial values for the
	 * wizard
	 * 
	 * @param wizard
	 */
	void initialize();

	/**
	 * Validates the status of this extension
	 * 
	 * @return
	 */
	IStatus validate();

	/**
	 * @param context
	 */
	void prepare(ICreateContext context);

}
