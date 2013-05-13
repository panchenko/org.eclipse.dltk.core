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
package org.eclipse.dltk.internal.testing.ui;

import org.eclipse.dltk.internal.testing.model.TestCaseElement;
import org.eclipse.dltk.internal.testing.model.TestCategoryElement;
import org.eclipse.dltk.internal.testing.model.TestSuiteElement;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

public class TestTreeComparator extends ViewerComparator {

	private int sortDirection = TestRunnerViewPart.SORT_DIRECTION_NO_SORT;

	public int category(Object element) {
		if (element instanceof TestCategoryElement) {
			return 0;
		} else if (element instanceof TestSuiteElement) {
			return 1;
		} else {
			return 2;
		}
	}

	public void setSortDirection(int sortDirection) {
		this.sortDirection = sortDirection;
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		String left = "";
		String right = "";
		if (e1 instanceof TestSuiteElement && e2 instanceof TestSuiteElement) {
			left = ((TestSuiteElement) e1).getTestName();
			right = ((TestSuiteElement) e2).getTestName();
		} else if (e1 instanceof TestCaseElement
				&& e2 instanceof TestCaseElement) {
			left = ((TestCaseElement) e1).getTestName();
			right = ((TestCaseElement) e2).getTestName();
		} else
			return super.compare(viewer, e1, e2);

		if (sortDirection == TestRunnerViewPart.SORT_DIRECTION_NO_SORT) {
			return 0;
		} else if (sortDirection == TestRunnerViewPart.SORT_DIRECTION_ASCENDING) {
			return left.compareTo(right);
		} else if (sortDirection == TestRunnerViewPart.SORT_DIRECTION_DESCENDING) {
			return -1 * left.compareTo(right);
		}
		return 0;
	}
}
