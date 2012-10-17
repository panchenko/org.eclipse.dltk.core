/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.dltk.internal.core;

import java.util.List;

import org.eclipse.dltk.core.IImportContainer;
import org.eclipse.dltk.core.IImportDeclaration;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ISourceRange;
import org.eclipse.dltk.core.ISourceReference;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.SourceRange;
import org.eclipse.dltk.core.WorkingCopyOwner;
import org.eclipse.dltk.internal.core.util.MementoTokenizer;
import org.eclipse.dltk.utils.CorePrinter;

/**
 * @see IImportContainer
 */
public class ImportContainer extends SourceRefElement implements
		IImportContainer {
	private final String containerName;

	protected ImportContainer(AbstractSourceModule parent, String containerName) {
		super(parent);
		this.containerName = containerName;
	}

	public String getContainerName() {
		return containerName;
	}

	@Override
	public String getElementName() {
		return getContainerName();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ImportContainer))
			return false;
		final ImportContainer other = (ImportContainer) o;
		if (!containerName.equals(other.containerName)) {
			return false;
		}
		return super.equals(o);
	}

	/**
	 * @see IJavaElement
	 */
	public int getElementType() {
		return IMPORT_CONTAINER;
	}

	@Override
	public IModelElement getHandleFromMemento(String token,
			MementoTokenizer memento, WorkingCopyOwner workingCopyOwner) {
		switch (token.charAt(0)) {
		case JEM_COUNT:
			return getHandleUpdatingCountFromMemento(memento, workingCopyOwner);
		case JEM_IMPORTDECLARATION:
			if (memento.hasMoreTokens()) {
				String importName = memento.nextToken();
				String version = memento.nextToken();
				if (version.length() == 0) {
					version = null;
				}
				ModelElement importDecl = (ModelElement) getImport(importName,
						version);
				return importDecl.getHandleFromMemento(memento,
						workingCopyOwner);
			} else {
				return this;
			}
		}
		return null;
	}

	/**
	 * @see ModelElement#getHandleMemento()
	 */
	@Override
	protected char getHandleMementoDelimiter() {
		return ModelElement.JEM_IMPORTDECLARATION;
	}

	/**
	 * @see IImportContainer
	 */
	public IImportDeclaration getImport(String importName, String version) {
		return new ImportDeclaration(this, importName, version);
	}

	public IImportDeclaration[] getImports() throws ModelException {
		List<IModelElement> list = getChildrenOfType(IMPORT_DECLARATION);
		return list.toArray(new IImportDeclaration[list.size()]);
	}

	/*
	 * @see JavaElement#getPrimaryElement(boolean)
	 */
	@Override
	public IModelElement getPrimaryElement(boolean checkOwner) {
		AbstractSourceModule cu = (AbstractSourceModule) this.parent;
		if (checkOwner && cu.isPrimary())
			return this;
		return new ImportContainer(cu, containerName);
	}

	/**
	 * @see ISourceReference
	 */
	@Override
	public ISourceRange getSourceRange() throws ModelException {
		IModelElement[] imports = getChildren();
		if (imports.length != 0) {
			ISourceRange firstRange = ((ISourceReference) imports[0])
					.getSourceRange();
			ISourceRange lastRange = ((ISourceReference) imports[imports.length - 1])
					.getSourceRange();
			return new SourceRange(firstRange.getOffset(),
					lastRange.getOffset() + lastRange.getLength()
							- firstRange.getOffset());
		} else {
			return null;
		}
	}

	/**
	 * @private Debugging purposes
	 */
	@Override
	protected void toString(int tab, StringBuffer buffer) {
		Object info = ModelManager.getModelManager().peekAtInfo(this);
		if (info == null || !(info instanceof ModelElementInfo))
			return;
		IModelElement[] children = ((ModelElementInfo) info).getChildren();
		for (int i = 0; i < children.length; i++) {
			if (i > 0)
				buffer.append("\n"); //$NON-NLS-1$
			((ModelElement) children[i]).toString(tab, buffer);
		}
	}

	/**
	 * Debugging purposes
	 */
	@Override
	protected void toStringInfo(int tab, StringBuffer buffer, Object info,
			boolean showResolvedInfo) {
		buffer.append(tabString(tab));
		buffer.append("<import container>"); //$NON-NLS-1$
		if (info == null) {
			buffer.append(" (not open)"); //$NON-NLS-1$
		}
	}

	@Override
	protected void closing(Object info) throws ModelException {
		// NOP
	}

	@Override
	public void printNode(CorePrinter output) {
		output.formatPrint("<import container>" + getElementName()); //$NON-NLS-1$
		output.indent();
		try {
			IModelElement[] modelElements = this.getChildren();
			for (int i = 0; i < modelElements.length; ++i) {
				IModelElement element = modelElements[i];
				if (element instanceof ModelElement) {
					((ModelElement) element).printNode(output);
				} else {
					output.print("Unknown element:" + element); //$NON-NLS-1$
				}
			}
		} catch (ModelException ex) {
			output.formatPrint(ex.getLocalizedMessage());
		}
		output.dedent();
	}

	public ISourceRange getNameRange() throws ModelException {
		return null;
	}
}
