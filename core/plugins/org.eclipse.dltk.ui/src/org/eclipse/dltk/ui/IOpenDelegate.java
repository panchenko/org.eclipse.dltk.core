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
package org.eclipse.dltk.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.annotations.ExtensionPoint;
import org.eclipse.dltk.codeassist.ISelectionEngine;
import org.eclipse.dltk.codeassist.ISelectionRequestor;
import org.eclipse.dltk.internal.ui.OpenDelegateManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

/**
 * Implementations of this interface allow opening foreign elements reported by
 * {@link ISelectionEngine} thru the
 * {@link ISelectionRequestor#acceptForeignElement(Object)} method.
 * 
 * Contributed implementations are instantiated once and used as factories,
 * handling multiple reported objects.
 * 
 * @since 3.0
 */
@ExtensionPoint(point = OpenDelegateManager.EXT_POINT, element = "delegate", attribute = "class")
public interface IOpenDelegate {

	/**
	 * Checks if this factory can handle the specified element.
	 */
	boolean supports(Object object);

	/**
	 * Returns the display name of the specified element, if supported or
	 * <code>null</code> otherwise.
	 */
	String getName(Object object);

	/**
	 * Opens the specified element in the editor.
	 */
	IEditorPart openInEditor(Object object, boolean activate)
			throws PartInitException, CoreException;

}
