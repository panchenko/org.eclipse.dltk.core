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
package org.eclipse.dltk.internal.debug.ui.interpreters;

import org.eclipse.dltk.core.environment.IEnvironment;

/**
 * @since 2.0
 */
public interface IInterpreterComboBlockContext {

	int M_BUILDPATH = 0;
	int M_LAUNCH_CONFIGURATION = 1;

	int getMode();

	String getNatureId();

	IEnvironment getEnvironment();

}
