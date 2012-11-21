/*******************************************************************************
 * Copyright (c) 2012 NumberFour AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     NumberFour AG - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.core.tests;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class WorkspaceSetup implements IWorkspaceSetup {

	private final String bundleName;

	public WorkspaceSetup(String bundleName) {
		this.bundleName = bundleName;
	}

	public WorkspaceSetup(Bundle bundle) {
		this.bundleName = bundle.getSymbolicName();
	}

	public WorkspaceSetup(Class<?> classFromBundle) {
		this(FrameworkUtil.getBundle(classFromBundle));
	}

	public String getBundleName() {
		return bundleName;
	}

	public void before() throws Throwable {
		WorkspaceAutoBuild.disable();
	}

	public void after() {
	}

}
