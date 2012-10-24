/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.dltk.internal.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.compiler.util.Util;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IModelStatusConstants;
import org.eclipse.dltk.core.IProjectFragment;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.RuntimePerformanceMonitor;
import org.eclipse.dltk.core.RuntimePerformanceMonitor.PerformanceNode;
import org.eclipse.dltk.core.WorkingCopyOwner;
import org.eclipse.dltk.core.environment.EnvironmentPathUtils;
import org.eclipse.dltk.core.environment.IFileHandle;

/**
 * Represents an external source module.
 */
public class ExternalSourceModule extends AbstractExternalSourceModule {

	private IStorage storage;

	public ExternalSourceModule(ModelElement parent, String name,
			WorkingCopyOwner owner, IStorage storage) {
		super(parent, name, owner);
		this.storage = storage;
	}

	public IStorage getStorage() {
		return storage;
	}

	/*
	 * @see
	 * org.eclipse.dltk.internal.core.AbstractSourceModule#equals(java.lang.
	 * Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof ExternalSourceModule)) {
			return false;
		}

		return super.equals(obj);
	}

	/*
	 * @see org.eclipse.core.resources.IStorage#getContents()
	 */
	public InputStream getContents() throws CoreException {
		return storage.getContents();
	}

	/*
	 * @see org.eclipse.dltk.compiler.env.IDependent#getFileName()
	 */
	public String getFileName() {
		return getPath().toOSString();
	}

	/*
	 * @see org.eclipse.core.resources.IStorage#getFullPath()
	 */
	public IPath getFullPath() {
		if (this.storage != null) {
			return storage.getFullPath();
		} else {
			return getPath();
		}
	}

	/*
	 * @see org.eclipse.core.resources.IStorage#getName()
	 */
	public String getName() {
		return storage.getName();
	}

	public IResource getResource() {
		return null;
	}

	/*
	 * @see
	 * org.eclipse.dltk.internal.core.AbstractSourceModule#getBufferContent()
	 */
	protected char[] getBufferContent() throws ModelException {
		IPath path = getBufferPath();
		IFileHandle file = EnvironmentPathUtils.getFile(path);

		IProjectFragment projectFragment = this.getProjectFragment();
		if (file != null && file.exists() && !projectFragment.isArchive()) {
			return org.eclipse.dltk.internal.core.util.Util
					.getResourceContentsAsCharArray(file);
		} else {
			if (projectFragment.isArchive()) {
				final InputStream stream;
				PerformanceNode p = RuntimePerformanceMonitor.begin();
				try {
					stream = new BufferedInputStream(storage.getContents(),
							4096);
				} catch (CoreException e) {
					throw new ModelException(e,
							IModelStatusConstants.ELEMENT_DOES_NOT_EXIST);
				}
				try {
					char[] data = Util.getInputStreamAsCharArray(stream, -1,
							Util.UTF_8);
					p.done("#", RuntimePerformanceMonitor.IOREAD, data.length);
					return data;
				} catch (IOException e) {
					throw new ModelException(e,
							IModelStatusConstants.IO_EXCEPTION);
				} finally {
					try {
						stream.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}
		}
		throw newNotPresentException();
	}

	/**
	 * Return buffer path in full mode
	 */
	protected IPath getBufferPath() {
		return getPath();
	}

	/*
	 * @see
	 * org.eclipse.dltk.internal.core.AbstractExternalSourceModule#getModuleType
	 * ()
	 */
	protected String getModuleType() {
		return "DLTK External Source Moule"; //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.dltk.internal.core.AbstractSourceModule#getNatureId()
	 */
	protected String getNatureId() {
		IPath path = getFullPath();
		IDLTKLanguageToolkit toolkit = lookupLanguageToolkit(path);
		if (toolkit == null) {
			toolkit = DLTKLanguageManager
					.getLanguageToolkit(getScriptProject());
		}
		return (toolkit != null) ? toolkit.getNatureId() : null;
	}

	/*
	 * @see
	 * org.eclipse.dltk.internal.core.AbstractSourceModule#getOriginalSourceModule
	 * ()
	 */
	protected ISourceModule getOriginalSourceModule() {
		return new ExternalSourceModule((ModelElement) getParent(),
				getElementName(), DefaultWorkingCopyOwner.PRIMARY, storage);
	}
}
