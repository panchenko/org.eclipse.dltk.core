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
package org.eclipse.dltk.core.tests;

import static org.eclipse.dltk.core.tests.model.AbstractModelTests.getProject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.dltk.core.tests.model.AbstractModelTests;
import org.eclipse.dltk.core.tests.model.SuiteOfTestCases;
import org.eclipse.dltk.core.tests.model.SuiteOfTestCases.Suite;
import org.eclipse.dltk.internal.core.ModelManager;

/**
 * A decorator for tests to initialize workspace project before executing the
 * tests. Use as follows:
 * 
 * <pre>
 * suite.addTest(new BundledProjectSetup(MyPluginActivator.PLUGIN_ID, &quot;myProject&quot;,
 * 		new TestSuite(MyTests.class)));
 * </pre>
 * 
 * where <code>myProject</code> is the folder with the preconfigured project
 * located at <code>bundleRoot/workspace/myProject</code>
 */
public class BundledProjectSetup extends TestSetup {

	public static class Builder {

		private String bundleName;
		private List<String> projectNames = new ArrayList<String>();
		private boolean build;
		private boolean disableIndexer;

		public Builder(String bundleName, String[] projectNames) {
			this.bundleName = bundleName;
			Collections.addAll(this.projectNames, projectNames);
		}

		public Builder build(boolean value) {
			this.build = value;
			return this;
		}

		public BundledProjectSetup suite(Class<?>... testClasses) {
			return new BundledProjectSetup(bundleName,
					projectNames.toArray(new String[projectNames.size()]),
					createTests(testClasses), build, disableIndexer);
		}

		private Test createTests(Class<?>[] testClasses) {
			if (testClasses.length == 1) {
				return createSuite(testClasses[0]);
			} else {
				TestSuite result = new TestSuite();
				for (Class<?> clazz : testClasses) {
					result.addTest(createSuite(clazz));
				}
				return result;
			}
		}

		@SuppressWarnings("unchecked")
		private Test createSuite(final Class<?> cls) {
			if (SuiteOfTestCases.class.isAssignableFrom(cls)) {
				return new Suite((Class<? extends SuiteOfTestCases>) cls);
			} else {
				return new TestSuite(cls);
			}
		}

		public Builder disableIndexer() {
			this.disableIndexer = true;
			return this;
		}

	}

	public static Builder create(String bundleName, String... projectNames) {
		return new Builder(bundleName, projectNames);
	}

	static class Helper extends AbstractModelTests {

		public Helper(String bundleName) {
			super(bundleName, BundledProjectSetup.class.getName());
		}

	}

	private final Helper helper;
	private final String[] projectNames;
	private final boolean build;
	private final boolean disableIndexer;

	public BundledProjectSetup(String bundleName, String projectName, Test test) {
		this(bundleName, projectName, test, false);
	}

	public BundledProjectSetup(String bundleName, String projectName,
			Test test, boolean build) {
		this(bundleName, new String[] { projectName }, test, build);
	}

	public BundledProjectSetup(String bundleName, String[] projectNames,
			Test test, boolean build) {
		this(bundleName, projectNames, test, build, false);
	}

	BundledProjectSetup(String bundleName, String[] projectNames, Test test,
			boolean build, boolean disableIndexer) {
		super(test);
		this.helper = new Helper(bundleName);
		this.projectNames = projectNames;
		this.build = build;
		this.disableIndexer = disableIndexer;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		WorkspaceAutoBuild.disable();
		if (disableIndexer) {
			ModelManager.getModelManager().getIndexManager().disable();
		}
		for (String projectName : projectNames) {
			helper.setUpProject(projectName);
		}
		if (build) {
			final long start = System.currentTimeMillis();
			for (String projectName : projectNames) {
				getProject(projectName).build(
						IncrementalProjectBuilder.FULL_BUILD, null);
			}
			System.out.println((System.currentTimeMillis() - start)
					+ " ms to build " + Arrays.asList(projectNames)
					+ " project(s)");
		}
	}

	@Override
	protected void tearDown() throws Exception {
		for (String projectName : projectNames) {
			helper.deleteProject(projectName);
		}
		if (disableIndexer) {
			ModelManager.getModelManager().getIndexManager().enable();
		}
		super.tearDown();
	}

}
