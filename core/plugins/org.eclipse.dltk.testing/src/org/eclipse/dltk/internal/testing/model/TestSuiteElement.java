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
package org.eclipse.dltk.internal.testing.model;

import org.eclipse.dltk.testing.model.ITestSuiteElement;

public class TestSuiteElement extends TestContainerElement implements
		ITestSuiteElement {

	/**
	 * @param parent
	 * @param id
	 * @param testName
	 * @param childrenCount
	 */
	public TestSuiteElement(TestContainerElement parent, String id,
			String testName, int childrenCount) {
		super(parent, id, testName, childrenCount);
	}

}
