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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Source workspace - default implementation of {@link IWorkspaceSetup}.
 */
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

	/**
	 * Returns the relative path to this source workspace.
	 */
	protected String getLocalWorkspacePath() {
		return "workspace";
	}

	private File sourceWorkspaceDirectory;

	public synchronized File getSourceWorkspaceDirectory() {
		if (sourceWorkspaceDirectory != null) {
			return sourceWorkspaceDirectory;
		}
		final Bundle bundle = Platform.getBundle(bundleName);
		if (bundle == null) {
			throw new IllegalStateException(NLS.bind(
					"Bundle \"{0}\" with test data not found", bundleName));
		}
		final URL bundleURL = bundle.getEntry("/");
		final File bundleDirectory;
		try {
			bundleDirectory = new File(FileLocator.toFileURL(bundleURL).toURI());
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		final File workspace = new File(bundleDirectory,
				getLocalWorkspacePath());
		if (!workspace.isDirectory()) {
			throw new IllegalStateException(NLS.bind(
					"Source workspace directory {0} doesn't exist", workspace));
		}
		sourceWorkspaceDirectory = workspace;
		return workspace;
	}

}
