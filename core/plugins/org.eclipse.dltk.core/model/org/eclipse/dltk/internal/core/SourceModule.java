/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.dltk.internal.core;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.IBuffer;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IModelStatusConstants;
import org.eclipse.dltk.core.IProblemRequestor;
import org.eclipse.dltk.core.IProjectFragment;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.WorkingCopyOwner;
import org.eclipse.dltk.internal.core.util.Messages;
import org.eclipse.dltk.internal.core.util.Util;

public class SourceModule extends AbstractSourceModule implements ISourceModule {

	// ~ Constructors

	public SourceModule(ModelElement parent, String name, WorkingCopyOwner owner) {
		super(parent, name, owner);
	}

	// ~ Methods

	/*
	 * @see
	 * org.eclipse.dltk.core.ISourceModule#becomeWorkingCopy(org.eclipse.dltk
	 * .core.IProblemRequestor, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void becomeWorkingCopy(IProblemRequestor problemRequestor,
			IProgressMonitor monitor) throws ModelException {
		ModelManager manager = ModelManager.getModelManager();

		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=267008
		// not null project requester passed here.
		ModelManager.PerWorkingCopyInfo perWorkingCopyInfo = manager
				.getPerWorkingCopyInfo(this, false /* don't create */,
						true /* record usage */, problemRequestor);
		if (perWorkingCopyInfo == null) {
			// close cu and its children
			close();

			BecomeWorkingCopyOperation operation = new BecomeWorkingCopyOperation(
					this, problemRequestor);
			operation.runOperation(monitor);
		}
	}

	@Override
	public boolean canBeRemovedFromCache() {
		if (getPerWorkingCopyInfo() != null) {
			return false; // working copies should remain in the cache until
		}
		// they are destroyed
		return super.canBeRemovedFromCache();
	}

	@Override
	public boolean canBufferBeRemovedFromCache(IBuffer buffer) {
		if (getPerWorkingCopyInfo() != null) {
			return false; // working copy buffers should remain in the cache
		}
		// until working copy is destroyed
		return super.canBufferBeRemovedFromCache(buffer);
	}

	@Override
	public void close() throws ModelException {
		if (getPerWorkingCopyInfo() != null) {
			return; // a working copy must remain opened until it is discarded
		}

		super.close();
	}

	/*
	 * @see org.eclipse.dltk.core.ISourceModule#commitWorkingCopy(boolean,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void commitWorkingCopy(boolean force, IProgressMonitor monitor)
			throws ModelException {
		CommitWorkingCopyOperation op = new CommitWorkingCopyOperation(this,
				force);
		op.runOperation(monitor);
	}

	/*
	 * @see org.eclipse.dltk.core.ISourceManipulation#delete(boolean,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void delete(boolean force, IProgressMonitor monitor)
			throws ModelException {
		IModelElement[] elements = new IModelElement[] { this };
		getModel().delete(elements, force, monitor);
	}

	/*
	 * @see org.eclipse.dltk.core.ISourceModule#discardWorkingCopy()
	 */
	public void discardWorkingCopy() throws ModelException {
		// discard working copy and its children
		DiscardWorkingCopyOperation op = new DiscardWorkingCopyOperation(this);
		op.runOperation(null);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SourceModule)) {
			return false;
		}

		return super.equals(obj);
	}

	@Override
	public boolean exists() {
		// working copy always exists in the model until it is gotten rid of
		// (even if not on buildpath)
		if (getPerWorkingCopyInfo() != null) {
			return true;
		}

		return super.exists();
	}

	/*
	 * @see org.eclipse.dltk.compiler.env.IDependent#getFileName()
	 */
	public String getFileName() {
		return this.getPath().toString();
	}

	/**
	 * Returns the per working copy info for the receiver, or null if none
	 * exist. Note: the use count of the per working copy info is NOT
	 * incremented.
	 */
	@Override
	public ModelManager.PerWorkingCopyInfo getPerWorkingCopyInfo() {
		// XXX: should be an interface method that allows a null
		// don't create or record usage - no problem requestor required
		return ModelManager.getModelManager().getPerWorkingCopyInfo(this,
				false, false, null);
	}

	/*
	 * @see org.eclipse.dltk.core.IModelElement#getResource()
	 */
	public IResource getResource() {
		IProjectFragment root = this.getProjectFragment();
		if (root.isArchive()) {
			return root.getResource();
		}

		return ((IContainer) this.getParent().getResource()).getFile(new Path(
				this.getElementName()));
	}

	/*
	 * @see
	 * org.eclipse.dltk.core.ISourceModule#getWorkingCopy(org.eclipse.dltk.core
	 * .WorkingCopyOwner, org.eclipse.dltk.core.IProblemRequestor,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ISourceModule getWorkingCopy(WorkingCopyOwner workingCopyOwner,
			IProblemRequestor problemRequestor, IProgressMonitor monitor)
			throws ModelException {
		if (!isPrimary()) {
			return this;
		}

		ModelManager manager = ModelManager.getModelManager();

		SourceModule workingCopy = new SourceModule((ModelElement) getParent(),
				getElementName(), workingCopyOwner);
		ModelManager.PerWorkingCopyInfo perWorkingCopyInfo = manager
				.getPerWorkingCopyInfo(workingCopy, false /* don't create */,
						true /* record usage */, null /*
													 * not used since don't
													 * create
													 */);
		if (perWorkingCopyInfo != null) {
			return perWorkingCopyInfo.getWorkingCopy(); // return existing
			// handle instead of the
			// one
			// created above
		}

		BecomeWorkingCopyOperation op = new BecomeWorkingCopyOperation(
				workingCopy, problemRequestor);
		op.runOperation(monitor);
		return workingCopy;
	}

	public boolean hasResourceChanged() {
		// XXX: should be an interface method
		if (!isWorkingCopy()) {
			return false;
		}

		// if resource got deleted, then #getModificationStamp() will answer
		// IResource.NULL_STAMP, which is always different from the cached
		// timestamp
		Object info = ModelManager.getModelManager().getInfo(this);
		if (info == null) {
			return false;
		}

		return ((SourceModuleElementInfo) info).timestamp != getResource()
				.getModificationStamp();
	}

	/*
	 * @see org.eclipse.dltk.core.ISourceModule#isWorkingCopy()
	 */
	public boolean isWorkingCopy() {
		// For backward compatibility, non primary working copies are always
		// returning true; in removal
		// delta, clients can still check that element was a working copy before
		// being discarded.
		return !isPrimary() || (getPerWorkingCopyInfo() != null);
	}

	private static final Set<SourceModule> locks = new HashSet<SourceModule>();

	private boolean acquire() {
		final long stop = System.currentTimeMillis() + 10000;
		synchronized (locks) {
			while (locks.contains(this)) {
				final long now = System.currentTimeMillis();
				if (now >= stop) {
					return false;
				}
				try {
					locks.wait(stop - now);
				} catch (InterruptedException e) {
					return false;
				}
			}
			locks.add(this);
			return true;
		}
	}

	private void release() {
		synchronized (locks) {
			locks.remove(this);
			locks.notifyAll();
		}
	}

	@Override
	public void makeConsistent(IProgressMonitor monitor) throws ModelException {
		if (acquire()) {
			try {
				if (isConsistent())
					return;
				openWhenClosed(createElementInfo(), monitor);
			} finally {
				release();
			}
		}
	}

	/*
	 * @see
	 * org.eclipse.dltk.core.ISourceManipulation#move(org.eclipse.dltk.core.
	 * IModelElement, org.eclipse.dltk.core.IModelElement, java.lang.String,
	 * boolean, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void move(IModelElement container, IModelElement sibling,
			String rename, boolean replace, IProgressMonitor monitor)
			throws ModelException {
		if (container == null) {
			throw new IllegalArgumentException(Messages.operation_nullContainer);
		}

		IModelElement[] elements = new IModelElement[] { this };
		IModelElement[] containers = new IModelElement[] { container };

		String[] renamings = null;
		if (rename != null) {
			renamings = new String[] { rename };
		}

		getModel()
				.move(elements, containers, null, renamings, replace, monitor);
	}

	/*
	 * @see org.eclipse.dltk.core.ISourceModule#reconcile(boolean,
	 * org.eclipse.dltk.core.WorkingCopyOwner,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void reconcile(boolean forceProblemDetection,
			WorkingCopyOwner workingCopyOwner, IProgressMonitor monitor)
			throws ModelException {
		if (!isWorkingCopy()) {
			return; // Reconciling is not supported on non working copies
		}

		if (workingCopyOwner == null) {
			workingCopyOwner = DefaultWorkingCopyOwner.PRIMARY;
		}

		ReconcileWorkingCopyOperation op = new ReconcileWorkingCopyOperation(
				this, forceProblemDetection, workingCopyOwner);
		// op.runOperation(monitor);
		ModelManager manager = ModelManager.getModelManager();
		try {
			// cache zip files for performance (see
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=134172)
			manager.cacheZipFiles();
			op.runOperation(monitor);
		} finally {
			manager.flushZipFiles();
		}
	}

	/*
	 * @see org.eclipse.dltk.core.ISourceManipulation#rename(java.lang.String,
	 * boolean, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void rename(String newName, boolean replace, IProgressMonitor monitor)
			throws ModelException {
		if (newName == null) {
			throw new IllegalArgumentException(Messages.operation_nullName);
		}

		IModelElement[] elements = new IModelElement[] { this };
		IModelElement[] dests = new IModelElement[] { this.getParent() };
		String[] renamings = new String[] { newName };
		getModel().rename(elements, dests, renamings, replace, monitor);
	}

	@Override
	public void save(IProgressMonitor pm, boolean force) throws ModelException {
		if (isWorkingCopy()) {
			// no need to save the buffer for a working copy (this is a noop)
			throw new RuntimeException("not implemented"); //$NON-NLS-1$ // not simply
			// makeConsistent,
			// also computes
			// fine-grain deltas
			// in case the working copy is being reconciled already (if not it
			// would miss
			// one iteration of deltas).
		}

		super.save(pm, force);
	}

	@Override
	protected boolean preventReopen() {
		return super.preventReopen() && (getPerWorkingCopyInfo() == null);
	}

	@Override
	protected String getNatureId() {
		IResource resource = this.getResource();
		Object lookup = (resource == null) ? (Object) getPath() : resource;

		IDLTKLanguageToolkit lookupLanguageToolkit = lookupLanguageToolkit(lookup);
		if (lookupLanguageToolkit == null) {
			return null;
		}
		return lookupLanguageToolkit.getNatureId();
	}

	@Override
	protected void closing(Object info) {
		if (getPerWorkingCopyInfo() == null) {
			super.closing(info);
		}
		// else the buffer of a working copy must remain open for the
		// lifetime of the working copy
	}

	/*
	 * @see
	 * org.eclipse.dltk.internal.core.AbstractSourceModule#getBufferContent()
	 */
	@Override
	protected char[] getBufferContent() throws ModelException {
		IFile file = (IFile) this.getResource();
		if (file == null || !file.exists()) {
			throw newNotPresentException();
		}

		return Util.getResourceContentsAsCharArray(file);
	}

	@Override
	protected String getModuleType() {
		return "DLTK Source Module"; //$NON-NLS-1$
	}

	@Override
	protected ISourceModule getOriginalSourceModule() {
		return new SourceModule((ModelElement) getParent(), getElementName(),
				DefaultWorkingCopyOwner.PRIMARY);
	}

	/*
	 * Assume that this is a working copy
	 */
	protected void updateTimeStamp(SourceModule original) throws ModelException {
		// XXX: should be an interface method
		long timeStamp = ((IFile) original.getResource())
				.getModificationStamp();
		if (timeStamp == IResource.NULL_STAMP) {
			throw new ModelException(new ModelStatus(
					IModelStatusConstants.INVALID_RESOURCE));
		}

		((SourceModuleElementInfo) getElementInfo()).timestamp = timeStamp;
	}
}
