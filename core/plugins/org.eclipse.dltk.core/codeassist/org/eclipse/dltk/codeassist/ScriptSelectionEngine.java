/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.codeassist;

import java.util.Collection;
import java.util.Map;

import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ISourceRange;
import org.eclipse.dltk.internal.codeassist.impl.AssistOptions;
import org.eclipse.dltk.internal.codeassist.impl.Engine;
import org.eclipse.dltk.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.dltk.internal.core.SearchableEnvironment;

public abstract class ScriptSelectionEngine extends Engine implements
		ISelectionEngine {

	/**
	 * @since 3.0
	 */
	protected ISelectionRequestor requestor;

	public ScriptSelectionEngine() {
		super(null);
	}

	public void setEnvironment(SearchableEnvironment environment) {
		this.nameEnvironment = environment;
		this.lookupEnvironment = new LookupEnvironment(this, nameEnvironment);
	}

	/**
	 * @since 3.0
	 */
	public void setRequestor(ISelectionRequestor requestor) {
		this.requestor = requestor;
	}

	/**
	 * @since 3.0
	 */
	protected void reportModelElement(IModelElement element) {
		requestor.acceptModelElement(element);
	}

	/**
	 * @since 3.0
	 */
	protected void reportModelElements(IModelElement[] elements) {
		for (IModelElement element : elements) {
			requestor.acceptModelElement(element);
		}
	}

	/**
	 * @since 3.0
	 */
	protected void reportModelElements(Collection<IModelElement> elements) {
		for (IModelElement element : elements) {
			requestor.acceptModelElement(element);
		}
	}

	/**
	 * @since 3.0
	 */
	protected void reportForeignElement(Object object) {
		requestor.acceptForeignElement(object);
	}

	/**
	 * @since 5.0
	 */
	protected void reportElement(Object element) {
		requestor.acceptElement(element, null);
	}

	/**
	 * @since 5.0
	 */
	protected void reportElement(Object element, ISourceRange range) {
		requestor.acceptElement(element, range);
	}

	public void setOptions(Map options) {
		this.options = new AssistOptions(options);
	}

}
