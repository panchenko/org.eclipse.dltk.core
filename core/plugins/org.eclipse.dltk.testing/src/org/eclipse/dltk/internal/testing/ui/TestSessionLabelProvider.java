/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.dltk.internal.testing.ui;

import org.eclipse.dltk.internal.testing.Messages;
import org.eclipse.dltk.internal.testing.model.TestCaseElement;
import org.eclipse.dltk.internal.testing.model.TestCategoryElement;
import org.eclipse.dltk.internal.testing.model.TestSuiteElement;
import org.eclipse.dltk.internal.testing.model.TestElement.Status;
import org.eclipse.dltk.testing.DLTKTestingMessages;
import org.eclipse.dltk.testing.ITestRunnerUI;
import org.eclipse.dltk.testing.ITestingClient;
import org.eclipse.dltk.testing.model.ITestCaseElement;
import org.eclipse.dltk.testing.model.ITestCategoryElement;
import org.eclipse.dltk.testing.model.ITestElement;
import org.eclipse.dltk.testing.model.ITestRunSession;
import org.eclipse.dltk.testing.model.ITestSuiteElement;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import java.text.NumberFormat;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

public class TestSessionLabelProvider extends LabelProvider  implements IStyledLabelProvider {

	private final TestRunnerViewPart fTestRunnerPart;
	private final int fLayoutMode;
	private final NumberFormat timeFormat;
	private boolean fShowTime;
	
	public TestSessionLabelProvider(TestRunnerViewPart testRunnerPart,
			int layoutMode) {
		fTestRunnerPart = testRunnerPart;
		fLayoutMode = layoutMode;
		fShowTime= true;
					
		timeFormat= NumberFormat.getNumberInstance();
		timeFormat.setGroupingUsed(true);
		timeFormat.setMinimumFractionDigits(3);
		timeFormat.setMaximumFractionDigits(3);
		timeFormat.setMinimumIntegerDigits(1);
	}

	private String getSimpleLabel(Object element) {
		if (element instanceof ITestCaseElement) {
			final ITestCaseElement caseElement = (ITestCaseElement) element;
			return getTestRunnerUI().getTestCaseLabel(caseElement, false);
		} else if (element instanceof ITestSuiteElement) {
			return ((ITestSuiteElement) element).getSuiteTypeName();
		} else if (element instanceof ITestCategoryElement) {
			return ((ITestCategoryElement) element).getCategoryName();
		}
		return element.toString();
	}
	
	private String addElapsedTime(String string, double time) {
		if (!fShowTime || Double.isNaN(time)) {
			return string;
		}
		String formattedTime= timeFormat.format(time);
		return Messages.format(DLTKTestingMessages.TestSessionLabelProvider_testName_elapsedTimeInSeconds, new String[] { string, formattedTime});
	}
	
	private StyledString addElapsedTime(StyledString styledString, double time) {
		String string= styledString.getString();
		String decorated= addElapsedTime(string, time);
		return StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.COUNTER_STYLER, styledString);
	}
	
	
	public StyledString getStyledText(Object element) {
		
		String label= getSimpleLabel(element);
		StyledString text= new StyledString(label);
		if ( element instanceof ITestCaseElement) {
			text =  StyledCellLabelProvider.styleDecoratedString(label, StyledString.QUALIFIER_STYLER, text);
		}

		ITestElement testElement= (ITestElement) element;
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL
				&& element instanceof ITestElement
				&& !(element instanceof ITestCategoryElement)) {
			final ITestElement parent = ((ITestElement) element)
					.getParentContainer();
			if (parent instanceof ITestRunSession
					|| parent instanceof ITestCategoryElement) {
				final String runnerDisplayName = getTestRunnerUI()
						.getDisplayName();
				if (runnerDisplayName != null) {
					String decorated =  Messages
							.format(
									DLTKTestingMessages.TestSessionLabelProvider_testName_JUnitVersion,
									new Object[] { label, runnerDisplayName });
					text= StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.QUALIFIER_STYLER, text);
				}
			}
		}
		return addElapsedTime(text, testElement.getElapsedTimeInSeconds());
	}

	public String getText(Object element) {
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_FLAT
				&& element instanceof ITestCaseElement) {
			return getTestRunnerUI().getTestCaseLabel(
					(ITestCaseElement) element, true);
		}
		final String label = getSimpleLabel(element);
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL
				&& element instanceof ITestElement
				&& !(element instanceof ITestCategoryElement)) {
			final ITestElement parent = ((ITestElement) element)
					.getParentContainer();
			if (parent instanceof ITestRunSession
					|| parent instanceof ITestCategoryElement) {
				final String runnerDisplayName = getTestRunnerUI()
						.getDisplayName();
				if (runnerDisplayName != null) {
					return Messages
							.format(
									DLTKTestingMessages.TestSessionLabelProvider_testName_JUnitVersion,
									new Object[] { label, runnerDisplayName });
				}
			}
		}
		return label;
	}

	private final ITestRunnerUI getTestRunnerUI() {
		return fTestRunnerPart.getTestRunnerUI();
	}

	public Image getImage(Object element) {
		if (element instanceof TestCaseElement) {
			TestCaseElement testCaseElement = ((TestCaseElement) element);
			if (testCaseElement.isIgnored())
				return fTestRunnerPart.fTestIgnoredIcon;

			Status status = testCaseElement.getStatus();
			if (status.isNotRun())
				return fTestRunnerPart.fTestIcon;
			else if (status.isRunning())
				return fTestRunnerPart.fTestRunningIcon;
			else if (status.isError())
				return fTestRunnerPart.fTestErrorIcon;
			else if (status.isFailure()) {
				switch (status.getFailedCode()) {
				case ITestingClient.ABORTED:
					return fTestRunnerPart.fTestAbortedIcon;
				case ITestingClient.BLOCKED:
					return fTestRunnerPart.fTestBlockedIcon;
				case ITestingClient.SKIPPED:
					return fTestRunnerPart.fTestSkippedIcon;
				case ITestingClient.UNKNOWN:
					return fTestRunnerPart.fTestUnknownIcon;
				default:
					return fTestRunnerPart.fTestFailIcon;
				}
			} else if (status.isOK())
				return fTestRunnerPart.fTestOkIcon;
			else
				throw new IllegalStateException(element.toString());

		} else if (element instanceof TestSuiteElement) {
			Status status = ((TestSuiteElement) element).getStatus();
			if (status.isNotRun())
				return fTestRunnerPart.fSuiteIcon;
			else if (status.isRunning())
				return fTestRunnerPart.fSuiteRunningIcon;
			else if (status.isError())
				return fTestRunnerPart.fSuiteErrorIcon;
			else if (status.isFailure())
				switch (status.getFailedCode()) {
				case ITestingClient.ABORTED:
					return fTestRunnerPart.fSuiteAbortedIcon;
				case ITestingClient.BLOCKED:
					return fTestRunnerPart.fSuiteBlockedIcon;
				case ITestingClient.SKIPPED:
					return fTestRunnerPart.fSuiteSkippedIcon;
				case ITestingClient.UNKNOWN:
					return fTestRunnerPart.fSuiteUnknownIcon;
				default:
					return fTestRunnerPart.fSuiteFailIcon;
				}
			else if (status.isOK())
				return fTestRunnerPart.fSuiteOkIcon;
			else
				throw new IllegalStateException(element.toString());

		} else if (element instanceof TestCategoryElement) {
			Status status = ((TestCategoryElement) element).getStatus();
			if (status.isNotRun())
				return fTestRunnerPart.fCategoryIcon;
			else if (status.isRunning())
				return fTestRunnerPart.fCategoryRunningIcon;
			else if (status.isError())
				return fTestRunnerPart.fCategoryErrorIcon;
			else if (status.isFailure())
				switch (status.getFailedCode()) {
				case ITestingClient.ABORTED:
					return fTestRunnerPart.fCategoryAbortedIcon;
				case ITestingClient.BLOCKED:
					return fTestRunnerPart.fCategoryBlockedIcon;
				case ITestingClient.SKIPPED:
					return fTestRunnerPart.fCategorySkippedIcon;
				case ITestingClient.UNKNOWN:
					return fTestRunnerPart.fCategoryUnknownIcon;
				default:
					return fTestRunnerPart.fCategoryFailIcon;
				}
			else if (status.isOK())
				return fTestRunnerPart.fCategoryOkIcon;
			else
				throw new IllegalStateException(element.toString());

		} else {
			throw new IllegalArgumentException(String.valueOf(element));
		}
	}
	
	public void setShowTime(boolean showTime) {
		fShowTime= showTime;
		fireLabelProviderChanged(new LabelProviderChangedEvent(this));
	}

}
