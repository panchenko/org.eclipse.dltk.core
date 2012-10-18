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
package org.eclipse.dltk.internal.core.builder;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.dltk.compiler.CharOperation;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.builder.IBuildContext;
import org.eclipse.dltk.core.builder.IBuildContextExtension;
import org.eclipse.dltk.core.builder.ISourceLineTracker;
import org.eclipse.dltk.utils.TextUtils;
import org.eclipse.osgi.util.NLS;

public abstract class AbstractBuildContext implements IBuildContext,
		IBuildContextExtension {

	private final Map<String, Object> attributes = new HashMap<String, Object>();

	public Object get(String attribute) {
		return attributes.get(attribute);
	}

	public void set(String attribute, Object value) {
		if (value == null) {
			attributes.remove(attribute);
		} else {
			attributes.put(attribute, value);
		}
	}

	private final int buildType;
	protected final ISourceModule module;

	/**
	 * @param module
	 */
	protected AbstractBuildContext(ISourceModule module, int buildType) {
		this.module = module;
		this.buildType = buildType;
	}

	public int getBuildType() {
		return buildType;
	}

	private char[] contents;

	public final char[] getContents() {
		if (contents == null) {
			try {
				contents = module.getSourceAsCharArray();
			} catch (ModelException e) {
				DLTKCore.error(
						NLS.bind(
								Messages.AbstractBuildContext_errorRetrievingContentsOf,
								module.getElementName()), e);
				contents = CharOperation.NO_CHAR;
			}
		}
		return contents;
	}

	private ISourceLineTracker lineTracker = null;

	public ISourceLineTracker getLineTracker() {
		if (lineTracker == null) {
			lineTracker = TextUtils.createLineTracker(getContents());
		}
		return lineTracker;
	}

	public void setLineTracker(ISourceLineTracker tracker) {
		this.lineTracker = tracker;
	}

	/*
	 * @see org.eclipse.dltk.core.builder.IBuildContext#isLineTrackerCreated()
	 */
	public boolean isLineTrackerCreated() {
		return lineTracker != null;
	}

	public final ISourceModule getSourceModule() {
		return module;
	}

	/*
	 * @see org.eclipse.dltk.core.builder.IBuildContext#getFile()
	 */
	public final IFile getFile() {
		return (IFile) module.getResource();
	}

	private String sourceContents;

	/*
	 * @see org.eclipse.dltk.compiler.env.IModuleSource#getSourceContents()
	 */
	public String getSourceContents() {
		if (sourceContents == null) {
			sourceContents = new String(getContents());
		}
		return sourceContents;
	}

	/*
	 * @see org.eclipse.dltk.compiler.env.IModuleSource#getContentsAsCharArray()
	 */
	public char[] getContentsAsCharArray() {
		return getContents();
	}

	/*
	 * @see org.eclipse.dltk.compiler.env.IModuleSource#getModelElement()
	 */
	public IModelElement getModelElement() {
		return getSourceModule();
	}

	/*
	 * @see org.eclipse.dltk.compiler.env.IDependent#getFileName()
	 */
	public String getFileName() {
		return getSourceModule().getElementName();
	}

}
