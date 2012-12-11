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
package org.eclipse.dltk.internal.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.codeassist.ISelectionEngine;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.ui.IOpenDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

/**
 * Instances of this class are used to represent foreign elements returned from
 * the {@link ISelectionEngine} after the corresponding {@link IOpenDelegate}
 * factory was found.
 * 
 * @see IOpenDelegate
 */
public class DelegatedOpen {
	private final IOpenDelegate adapter;
	private final Object object;

	public DelegatedOpen(IOpenDelegate adapter, Object object) {
		this.adapter = adapter;
		this.object = object;
	}

	public IEditorPart openInEditor(boolean activate) throws PartInitException,
			ModelException {
		try {
			return adapter.openInEditor(object, activate);
		} catch (PartInitException e) {
			throw e;
		} catch (ModelException e) {
			throw e;
		} catch (CoreException e) {
			throw new ModelException(e);
		}
	}

	public String getName() {
		return adapter.getName(object);
	}

}
