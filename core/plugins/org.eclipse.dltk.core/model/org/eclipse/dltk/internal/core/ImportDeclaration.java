/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.dltk.internal.core;

import org.eclipse.core.runtime.Assert;
import org.eclipse.dltk.core.IImportDeclaration;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ISourceRange;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.utils.CorePrinter;

/**
 * Handle for an import declaration. Info object is a
 * ImportDeclarationElementInfo.
 * 
 * @see IImportDeclaration
 */
public class ImportDeclaration extends SourceRefElement implements
		IImportDeclaration {

	private final String name;
	private final String version;

	/**
	 * Constructs an ImportDeclaration in the given import container with the
	 * given name.
	 */
	protected ImportDeclaration(ImportContainer parent, String name,
			String version) {
		super(parent);
		this.name = name;
		this.version = version;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ImportDeclaration))
			return false;
		final ImportDeclaration other = (ImportDeclaration) o;
		if (!name.equals(other.name)) {
			return false;
		}
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return super.equals(o);
	}

	@Override
	public String getElementName() {
		return this.name;
	}

	public String getVersion() {
		return version;
	}

	/**
	 * @see IJavaElement
	 */
	public int getElementType() {
		return IMPORT_DECLARATION;
	}

	/**
	 * @see JavaElement#getHandleMemento(StringBuffer) For import declarations,
	 *      the handle delimiter is associated to the import container already
	 */
	@Override
	public void getHandleMemento(StringBuffer buff) {
		((ModelElement) getParent()).getHandleMemento(buff);
		escapeMementoName(buff, getElementName());
		if (this.occurrenceCount > 1) {
			buff.append(JEM_COUNT);
			buff.append(this.occurrenceCount);
		}
	}

	/**
	 * @see JavaElement#getHandleMemento()
	 */
	@Override
	protected char getHandleMementoDelimiter() {
		// For import declarations, the handle delimiter is associated to the
		// import container already
		Assert.isTrue(false, "Should not be called"); //$NON-NLS-1$
		return 0;
	}

	/*
	 * @see JavaElement#getPrimaryElement(boolean)
	 */
	@Override
	public IModelElement getPrimaryElement(boolean checkOwner) {
		AbstractSourceModule cu = (AbstractSourceModule) this.parent
				.getParent();
		if (checkOwner && cu.isPrimary())
			return this;
		return new ImportDeclaration(new ImportContainer(cu,
				((ImportContainer) this.parent).getContainerName()), name,
				version);
	}

	/**
	 * @private Debugging purposes
	 */
	@Override
	protected void toStringInfo(int tab, StringBuffer buffer, Object info,
			boolean showResolvedInfo) {
		buffer.append(tabString(tab));
		buffer.append("import "); //$NON-NLS-1$
		toStringName(buffer);
		if (info == null) {
			buffer.append(" (not open)"); //$NON-NLS-1$
		}
	}

	@Override
	protected void closing(Object info) throws ModelException {
		// EMPTY
	}

	@Override
	public void printNode(CorePrinter output) {
		output.formatPrint(getElementName());
	}

	public ISourceRange getNameRange() throws ModelException {
		return getSourceRange();
	}
}
