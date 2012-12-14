/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.internal.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.dltk.codeassist.ICompletionEngine;
import org.eclipse.dltk.codeassist.ISelectionEngine;
import org.eclipse.dltk.codeassist.ISelectionRequestor;
import org.eclipse.dltk.compiler.env.IModuleSource;
import org.eclipse.dltk.compiler.problem.DefaultProblem;
import org.eclipse.dltk.compiler.problem.IProblemIdentifier;
import org.eclipse.dltk.compiler.problem.ProblemSeverity;
import org.eclipse.dltk.core.BufferChangedEvent;
import org.eclipse.dltk.core.CompletionRequestor;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IBuffer;
import org.eclipse.dltk.core.IBufferChangedListener;
import org.eclipse.dltk.core.ICompletionRequestorExtension;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IModelStatusConstants;
import org.eclipse.dltk.core.IOpenable;
import org.eclipse.dltk.core.IProjectFragment;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceRange;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.ScriptModelUtil;
import org.eclipse.dltk.core.WorkingCopyOwner;

/**
 * Abstract class for implementations of model elements which are IOpenable.
 * 
 * @see IModelElement
 * @see IOpenable
 */
public abstract class Openable extends ModelElement implements IOpenable,
		IBufferChangedListener {

	protected Openable(ModelElement parent) {
		super(parent);
	}

	/**
	 * The buffer associated with this element has changed. Registers this
	 * element as being out of synch with its buffer's contents. If the buffer
	 * has been closed, this element is set as NOT out of synch with the
	 * contents.
	 * 
	 * @see IBufferChangedListener
	 */
	public void bufferChanged(BufferChangedEvent event) {
		if (event.getBuffer().isClosed()) {
			ModelManager.getModelManager().getElementsOutOfSynchWithBuffers()
					.remove(this);
			getBufferManager().removeBuffer(event.getBuffer());
		} else {
			ModelManager.getModelManager().getElementsOutOfSynchWithBuffers()
					.add(this);
		}
	}

	/**
	 * Builds this element's structure and properties in the given info object,
	 * based on this element's current contents (reuse buffer contents if this
	 * element has an open buffer, or resource contents if this element does not
	 * have an open buffer). Children are placed in the given newElements table
	 * (note, this element has already been placed in the newElements table).
	 * Returns true if successful, or false if an error is encountered while
	 * determining the structure of this element.
	 */
	protected abstract boolean buildStructure(OpenableElementInfo info,
			IProgressMonitor pm, Map newElements, IResource underlyingResource)
			throws ModelException;

	/*
	 * Returns whether this element can be removed from the model cache to make
	 * space.
	 */
	public boolean canBeRemovedFromCache() {
		try {
			return !hasUnsavedChanges();
		} catch (ModelException e) {
			return false;
		}
	}

	/*
	 * Returns whether the buffer of this element can be removed from the Script
	 * model cache to make space.
	 */
	public boolean canBufferBeRemovedFromCache(IBuffer buffer) {
		return !buffer.hasUnsavedChanges();
	}

	/**
	 * Close the buffer associated with this element, if any.
	 */
	protected void closeBuffer() {
		if (!hasBuffer())
			return; // nothing to do
		IBuffer buffer = getBufferManager().getBuffer(this);
		if (buffer != null) {
			buffer.close();
			buffer.removeBufferChangedListener(this);
		}
	}

	/**
	 * This element is being closed. Do any necessary cleanup.
	 */
	protected void closing(Object info) {
		closeBuffer();
	}

	/**
	 * @see IModelElement
	 */
	public boolean exists() {
		ModelManager manager = ModelManager.getModelManager();
		if (manager.getInfo(this) != null)
			return true;
		if (!parentExists())
			return false;
		IProjectFragment root = getProjectFragment();
		if (root != null && (root == this || !root.isArchive())) {
			return resourceExists();
		}
		return super.exists();
	}

	protected void generateInfos(Object info, HashMap newElements,
			IProgressMonitor monitor) throws ModelException {

		if (ModelManager.VERBOSE) {
			String element;
			switch (getElementType()) {
			case SCRIPT_PROJECT:
				element = "project"; //$NON-NLS-1$
				break;
			case PROJECT_FRAGMENT:
				element = "fragment"; //$NON-NLS-1$
				break;
			case SCRIPT_FOLDER:
				element = "folder"; //$NON-NLS-1$
				break;
			case BINARY_MODULE:
				element = "binary module"; //$NON-NLS-1$
				break;
			case SOURCE_MODULE:
				element = "source module"; //$NON-NLS-1$
				break;
			default:
				element = "element"; //$NON-NLS-1$
			}
			System.out
					.println(Thread.currentThread()
							+ " OPENING " + element + " " + this.toStringWithAncestors()); //$NON-NLS-1$//$NON-NLS-2$
		}

		// open the parent if necessary
		openParent(info, newElements, monitor);
		if (monitor != null && monitor.isCanceled())
			throw new OperationCanceledException();

		// puts the info before building the structure so that questions to the
		// handle behave as if the element existed
		// (case of compilation units becoming working copies)
		newElements.put(this, info);

		// build the structure of the openable (this will open the buffer if
		// needed)
		try {
			OpenableElementInfo openableElementInfo = (OpenableElementInfo) info;
			boolean isStructureKnown = buildStructure(openableElementInfo,
					monitor, newElements, getResource());
			openableElementInfo.setIsStructureKnown(isStructureKnown);
		} catch (ModelException e) {
			newElements.remove(this);
			throw e;
		}

		// remove out of sync buffer for this element
		ModelManager.getModelManager().getElementsOutOfSynchWithBuffers()
				.remove(this);

		if (ModelManager.VERBOSE) {
			System.out.println(ModelManager.getModelManager().cache
					.toStringFillingRation("-> ")); //$NON-NLS-1$
		}
	}

	/**
	 * Note: a buffer with no unsaved changes can be closed by the Model since
	 * it has a finite number of buffers allowed open at one time. If this is
	 * the first time a request is being made for the buffer, an attempt is made
	 * to create and fill this element's buffer. If the buffer has been closed
	 * since it was first opened, the buffer is re-created.
	 * 
	 * @see IOpenable
	 */
	public IBuffer getBuffer() throws ModelException {
		if (hasBuffer()) {
			// ensure element is open
			Object info = getElementInfo();
			IBuffer buffer = getBufferManager().getBuffer(this);
			if (buffer == null) {
				// try to (re)open a buffer
				buffer = openBuffer(null, info);
			}
			return buffer;
		} else {
			return null;
		}
	}

	/**
	 * Returns {@link IBuffer} or <code>null</code> if this element is not
	 * opened yet.
	 */
	public IBuffer getBufferNotOpen() throws ModelException {
		if (hasBuffer()) {
			return getBufferManager().getBuffer(this);
		}
		return null;
	}

	/**
	 * Returns the buffer manager for this element.
	 */
	protected BufferManager getBufferManager() {
		return BufferManager.getDefaultBufferManager();
	}

	/**
	 * Return my underlying resource. Elements that may not have a corresponding
	 * resource must override this method.
	 * 
	 * @see IScriptElement
	 */
	public IResource getCorrespondingResource() throws ModelException {
		return getUnderlyingResource();
	}

	/*
	 * @see IModelElement
	 */
	public IOpenable getOpenable() {
		return this;
	}

	public IResource getUnderlyingResource() throws ModelException {
		IResource parentResource = this.parent.getUnderlyingResource();
		if (parentResource == null) {
			return null;
		}
		int type = parentResource.getType();
		if (type == IResource.FOLDER || type == IResource.PROJECT) {
			IContainer folder = (IContainer) parentResource;
			IResource resource = folder.findMember(getElementName());
			if (resource == null) {
				throw newNotPresentException();
			} else {
				return resource;
			}
		} else {
			return parentResource;
		}
	}

	/**
	 * Returns true if this element may have an associated source buffer,
	 * otherwise false. Subclasses must override as required.
	 */
	protected boolean hasBuffer() {
		return false;
	}

	/**
	 * @see IOpenable
	 */
	public boolean hasUnsavedChanges() throws ModelException {

		if (isReadOnly() || !isOpen()) {
			return false;
		}
		if (hasBuffer()) {
			IBuffer buf = this.getBufferNotOpen();
			if (buf != null && buf.hasUnsavedChanges()) {
				return true;
			}
		}
		// for package fragments, package fragment roots, and projects must
		// check open buffers
		// to see if they have an child with unsaved changes
		int elementType = getElementType();
		if (elementType == SCRIPT_FOLDER || elementType == PROJECT_FRAGMENT
				|| elementType == SCRIPT_PROJECT || elementType == SCRIPT_MODEL) { // fix
			// for
			// 1FWNMHH
			Enumeration openBuffers = getBufferManager().getOpenBuffers();
			while (openBuffers.hasMoreElements()) {
				IBuffer buffer = (IBuffer) openBuffers.nextElement();
				if (buffer.hasUnsavedChanges()) {
					IModelElement owner = buffer.getOwner();
					if (isAncestorOf(owner)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Subclasses must override as required.
	 * 
	 * @see IOpenable
	 */
	public boolean isConsistent() {
		return true;
	}

	/**
	 * 
	 * @see IOpenable
	 */
	public boolean isOpen() {
		return ModelManager.getModelManager().getInfo(this) != null;
	}

	/**
	 * Returns true if this represents a source element. Openable source
	 * elements have an associated buffer created when they are opened.
	 */
	protected boolean isSourceElement() {
		return false;
	}

	/**
	 * @see IModelElement
	 */
	public boolean isStructureKnown() throws ModelException {
		return ((OpenableElementInfo) getElementInfo()).isStructureKnown();
	}

	/**
	 * @see IOpenable
	 */
	public void makeConsistent(IProgressMonitor monitor) throws ModelException {
		// only source modules can be inconsistent
		// other openables cannot be inconsistent so default is to do nothing
	}

	/**
	 * @see IOpenable
	 */
	public void open(IProgressMonitor pm) throws ModelException {
		getElementInfo(pm);
	}

	/**
	 * Opens a buffer on the contents of this element, and returns the buffer,
	 * or returns <code>null</code> if opening fails. By default, do nothing -
	 * subclasses that have buffers must override as required.
	 */
	protected IBuffer openBuffer(IProgressMonitor pm, Object info)
			throws ModelException {
		return null;
	}

	/**
	 * Open the parent element if necessary.
	 */
	protected void openParent(Object childInfo, HashMap newElements,
			IProgressMonitor pm) throws ModelException {

		Openable openableParent = (Openable) getOpenableParent();
		if (openableParent != null && !openableParent.isOpen()) {
			openableParent.generateInfos(openableParent.createElementInfo(),
					newElements, pm);
		}
	}

	/**
	 * Answers true if the parent exists (null parent is answering true)
	 * 
	 */
	protected boolean parentExists() {
		IModelElement parentElement = getParent();
		if (parentElement == null)
			return true;
		return parentElement.exists();
	}

	/**
	 * Returns whether the corresponding resource or associated file exists
	 */
	protected boolean resourceExists() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		if (workspace == null)
			return false; // workaround for
		// http://bugs.eclipse.org/bugs/show_bug.cgi?id=34069
		return Model.getTarget(workspace.getRoot(), this.getPath()
				.makeRelative(), // ensure path is relative (see
				// http://dev.eclipse.org/bugs/show_bug.cgi?id=22517)
				true) != null;
	}

	/**
	 * @see IOpenable
	 */
	public void save(IProgressMonitor pm, boolean force) throws ModelException {
		if (isReadOnly()) {
			throw new ModelException(new ModelStatus(
					IModelStatusConstants.READ_ONLY, this));
		}
		IBuffer buf = getBuffer();
		if (buf != null) { // some Openables (like a ScriptProject) don't have
			// a
			// buffer
			buf.save(pm, force);
			this.makeConsistent(pm); // update the element info of this
			// element
		}
	}

	/**
	 * Find enclosing project fragment if any
	 */
	public IProjectFragment getProjectFragment() {
		return (IProjectFragment) getAncestor(IModelElement.PROJECT_FRAGMENT);
	}

	static class CompletionThread extends Thread {
		final IDLTKLanguageToolkit toolkit;
		final IScriptProject project;
		final IModuleSource cu;
		final int position;
		final CompletionRequestor requestor;
		final NullProgressMonitor monitor = new NullProgressMonitor();

		public CompletionThread(IDLTKLanguageToolkit toolkit,
				IScriptProject project, IModuleSource cu, int position,
				CompletionRequestor requestor) {
			super("CompletionThread-" + toolkit.getLanguageName());
			this.toolkit = toolkit;
			this.project = project;
			this.cu = cu;
			this.position = position;
			this.requestor = requestor;
		}

		private boolean done = false;

		@Override
		public void run() {
			// code complete
			final ICompletionEngine[] engines = DLTKLanguageManager
					.getCompletionEngines(toolkit.getNatureId());
			if (engines != null) {
				for (ICompletionEngine engine : engines) {
					run(engine);
					if (requestor.isIgnored(CompletionRequestor.ALL)) {
						break;
					}
					if (monitor.isCanceled())
						break;
				}
			}
			done = true;
		}

		private void run(ICompletionEngine engine) {
			// engine.setEnvironment(environment);
			engine.setRequestor(requestor);

			engine.setOptions(project.getOptions(true));
			engine.setProject(project);

			engine.setProgressMonitor(monitor);
			engine.complete(cu, position, 0);
		}

		boolean execute(long timeout) {
			start();
			try {
				join(timeout);
				if (!done) {
					monitor.setCanceled(true);
					interrupt();
				}
				return done;
			} catch (InterruptedException e) {
				if (DLTKCore.DEBUG) {
					e.printStackTrace();
				}
				return false;
			}
		}
	}

	/** Code Completion */
	protected void codeComplete(final IModuleSource cu, final int position,
			CompletionRequestor requestor, WorkingCopyOwner owner, long timeout)
			throws ModelException {
		if (requestor == null) {
			throw new IllegalArgumentException(
					Messages.Openable_completionRequesterCannotBeNull);
		}

		IBuffer buffer = getBuffer();
		if (buffer == null) {
			return;
		}
		if (position < -1 || position > buffer.getLength()) {
			if (DLTKCore.DEBUG) {
				throw new ModelException(new ModelStatus(
						IModelStatusConstants.INDEX_OUT_OF_BOUNDS));
			}
			return;
		}

		IDLTKLanguageToolkit toolkit = DLTKLanguageManager
				.getLanguageToolkit(this);
		if (toolkit == null) {
			return;
		}

		CompletionThread thread = new CompletionThread(toolkit,
				getScriptProject(), cu, position, requestor);
		if (!thread.execute(timeout)) {
			Thread.interrupted();
			requestor
					.completionFailure(new DefaultProblem(
							"Computation of proposals takes too long. Please try again.",
							IProblemIdentifier.NULL, null,
							ProblemSeverity.WARNING, 0, 0, 0));
			if (requestor instanceof ICompletionRequestorExtension) {
				((ICompletionRequestorExtension) requestor).reset();
			}
		}
	}

	static class ModelElementSelectionRequestor implements ISelectionRequestor {
		private final List<IModelElement> elements = new ArrayList<IModelElement>();
		private List<Object> foreignElements;
		private Map<Object, ISourceRange> ranges;

		public void acceptForeignElement(Object element) {
			if (element instanceof IModelElement) {
				acceptModelElement((IModelElement) element);
			} else {
				if (foreignElements == null) {
					foreignElements = new ArrayList<Object>();
				}
				foreignElements.add(element);
			}
		}

		public void acceptModelElement(IModelElement element) {
			elements.add(element);
		}

		public void acceptElement(Object element, ISourceRange range) {
			acceptForeignElement(element);
			if (range != null) {
				if (ranges == null) {
					ranges = new HashMap<Object, ISourceRange>();
				}
				ranges.put(element, range);
			}
		}

		void addModelElements(IModelElement[] elements) {
			Collections.addAll(this.elements, elements);
		}

		IModelElement[] toModelElementArray() {
			if (elements.isEmpty()) {
				return ScriptModelUtil.NO_ELEMENTS;
			} else {
				return elements.toArray(new IModelElement[elements.size()]);
			}
		}

		boolean isEmpty() {
			return elements.isEmpty()
					&& (foreignElements == null || foreignElements.isEmpty());
		}

		CodeSelection asResponse() {
			if (isEmpty()) {
				return null;
			} else {
				final IModelElement[] elementArray = elements != null
						&& !elements.isEmpty() ? elements
						.toArray(new IModelElement[elements.size()]) : null;
				final Object[] foreignElementArray = foreignElements != null
						&& !foreignElements.isEmpty() ? foreignElements
						.toArray() : null;
				return new CodeSelection(elementArray, foreignElementArray,
						ranges);
			}
		}
	}

	private void codeSelect(org.eclipse.dltk.compiler.env.IModuleSource cu,
			int offset, int length, WorkingCopyOwner owner,
			ModelElementSelectionRequestor requestor) throws ModelException {

		ScriptProject project = (ScriptProject) getScriptProject();

		IBuffer buffer = getBuffer();
		int end = -1;
		if (buffer != null) {
			end = buffer.getLength();
		}
		IDLTKLanguageToolkit toolkit = DLTKLanguageManager
				.getLanguageToolkit(this);
		if (toolkit == null) {
			if (DLTKCore.VERBOSE) {
				System.out
						.println("DLTK.Openable.VERBOSE: Failed to detect language toolkit... for module:" //$NON-NLS-1$
								+ this.getResource().getName());
			}
			return;
		}

		if (offset < 0 || length < 0 || (end != -1 && (offset + length > end))) {
			throw new ModelException(new ModelStatus(
					IModelStatusConstants.INDEX_OUT_OF_BOUNDS));
		}

		final ISelectionEngine[] engines = DLTKLanguageManager
				.getSelectionEngines(toolkit.getNatureId());
		if (engines != null) {
			for (ISelectionEngine engine : engines) {
				engine.setOptions(project.getOptions(true));
				engine.setRequestor(requestor);
				final IModelElement[] result = engine.select(cu, offset, offset
						+ length - 1);
				if (result != null) {
					requestor.addModelElements(result);
				}
				if (!requestor.isEmpty()) {
					return;
				}
			}
		}
	}

	protected IModelElement[] codeSelect(
			org.eclipse.dltk.compiler.env.IModuleSource cu, int offset,
			int length, WorkingCopyOwner owner) throws ModelException {
		final ModelElementSelectionRequestor requestor = new ModelElementSelectionRequestor();
		codeSelect(cu, offset, length, owner, requestor);
		return requestor.toModelElementArray();
	}

	protected CodeSelection codeSelectAll(
			org.eclipse.dltk.compiler.env.IModuleSource cu, int offset,
			int length, WorkingCopyOwner owner) throws ModelException {
		final ModelElementSelectionRequestor requestor = new ModelElementSelectionRequestor();
		codeSelect(cu, offset, length, owner, requestor);
		return requestor.asResponse();
	}

}
