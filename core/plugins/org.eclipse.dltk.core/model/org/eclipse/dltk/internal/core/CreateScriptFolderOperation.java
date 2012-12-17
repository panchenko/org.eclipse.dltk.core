/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.internal.core;

import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IModelStatus;
import org.eclipse.dltk.core.IModelStatusConstants;
import org.eclipse.dltk.core.IProjectFragment;
import org.eclipse.dltk.core.IScriptFolder;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.core.util.Messages;
import org.eclipse.dltk.internal.core.util.Util;


/**
 * This operation creates a new script folder under a given project fragment.
 * The following must be specified:
 * <ul>
 * <li>the project fragment
 * <li>the folder name
 * </ul>
 * <p>
 * Any needed folders are created. If the script folder already exists, this
 * operation has no effect. The result elements include the
 * <code>IScriptFolder</code> created and any side effect script folders that
 * were created.
 * 
 * <p>
 * NOTE: A default script folder exists by default for a given project fragment.
 * 
 * <p>
 * Possible exception conditions:
 * <ul>
 * <li>project fragment is read-only
 * <li>script folder's name is taken by a simple (non-folder) resource
 * </ul>
 */
public class CreateScriptFolderOperation extends ModelOperation {
	/**
	 * The fully qualified, folder name.
	 */
	protected IPath pkgName;

	/**
	 * When executed, this operation will create a script folder with the given
	 * name under the given project fragment. The slash-separated name is broken
	 * into segments. Intermediate folders are created as required for each
	 * segment. If the folders already exist, this operation has no effect.
	 */
	public CreateScriptFolderOperation(IProjectFragment parentElement, String packageName, boolean force) {
		super(null, new IModelElement[] {
			parentElement
		}, force);
		this.pkgName = packageName == null ? null : new Path(packageName);
	}

	/**
	 * Execute the operation - creates the new script folder and any side effect
	 * folders.
	 * 
	 * @exception ModelException
	 *                if the operation is unable to complete
	 */
	protected void executeOperation() throws ModelException {
		ModelElementDelta delta = null;
		IProjectFragment root = (IProjectFragment) getParentElement();
		beginTask(Messages.operation_createScriptFolderProgress, this.pkgName.segmentCount());
		IContainer parentFolder = (IContainer) root.getResource();
		IPath sideEffectPackageName = Path.EMPTY;
		ArrayList<IScriptFolder> results = new ArrayList<IScriptFolder>(
				this.pkgName.segmentCount());
		int i;
		for (i = 0; i < this.pkgName.segmentCount(); i++) {
			String subFolderName = this.pkgName.segment(i);
			sideEffectPackageName = sideEffectPackageName.append(subFolderName);
			IResource subFolder = parentFolder.findMember(subFolderName);
			if (subFolder == null) {
				createFolder(parentFolder, subFolderName, force);
				parentFolder = parentFolder.getFolder(new Path(subFolderName));
				IScriptFolder addedFrag = root.getScriptFolder(sideEffectPackageName);
				if (!Util.isExcluded(parentFolder, root)) {
					if (delta == null) {
						delta = newModelElementDelta();
					}
					delta.added(addedFrag);
				}
				results.add(addedFrag);
			} else {
				parentFolder = (IContainer) subFolder;
			}
			worked(1);
		}
		if (results.size() > 0) {
			this.resultElements = new IModelElement[results.size()];
			results.toArray(this.resultElements);
			if (delta != null) {
				addDelta(delta);
			}
		}
		done();
	}

	/**
	 * Possible failures:
	 * <ul>
	 * <li>NO_ELEMENTS_TO_PROCESS - the root supplied to the operation is
	 * <code>null</code>.
	 * <li>INVALID_NAME - the name provided to the operation is
	 * <code>null</code> or is not a valid script folder name.
	 * <li>READ_ONLY - the root provided to this operation is read only.
	 * <li>NAME_COLLISION - there is a pre-existing resource (file) with the
	 * same name as a folder in the script folder's hierarchy.
	 * <li>ELEMENT_NOT_PRESENT - the underlying resource for the root is missing
	 * </ul>
	 * 
	 * @see IScriptModelStatus
	 * @see ScriptConventions
	 */
	public IModelStatus verify() {
		if (getParentElement() == null) {
			return new ModelStatus(IModelStatusConstants.NO_ELEMENTS_TO_PROCESS);
		}
		IPath packageName = this.pkgName == null ? null : this.pkgName.append("."); //$NON-NLS-1$
		String packageNameValue = null;
		if (packageName != null) {
			packageNameValue = packageName.toOSString();
		}
		if (this.pkgName == null
				|| (this.pkgName.segmentCount() > 0 && !Util
						.isValidFolderNameForPackage(
								(IContainer) getParentElement().getResource(),
								packageName.toString()))) {
			return new ModelStatus(IModelStatusConstants.INVALID_NAME,
					packageNameValue);
		}
		IProjectFragment root = (IProjectFragment) getParentElement();
		if (root.isReadOnly()) {
			return new ModelStatus(IModelStatusConstants.READ_ONLY, root);
		}
		IContainer parentFolder = (IContainer) root.getResource();
		int i;
		for (i = 0; i < this.pkgName.segmentCount(); i++) {
			IResource subFolder = parentFolder.findMember(this.pkgName.segment(i));
			if (subFolder != null) {
				if (subFolder.getType() != IResource.FOLDER) {
					return new ModelStatus(IModelStatusConstants.NAME_COLLISION, Messages.bind(Messages.status_nameCollision,
							subFolder.getFullPath().toString()));
				}
				parentFolder = (IContainer) subFolder;
			}
		}
		return ModelStatus.VERIFIED_OK;
	}
}
