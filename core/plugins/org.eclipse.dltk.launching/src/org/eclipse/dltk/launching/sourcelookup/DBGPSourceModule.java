/**
 * 
 */
package org.eclipse.dltk.launching.sourcelookup;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.dltk.compiler.util.Util;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IModelStatus;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.WorkingCopyOwner;
import org.eclipse.dltk.dbgp.exceptions.DbgpException;
import org.eclipse.dltk.internal.core.AbstractExternalSourceModule;
import org.eclipse.dltk.internal.core.DefaultWorkingCopyOwner;
import org.eclipse.dltk.internal.core.ScriptProject;
import org.eclipse.dltk.internal.debug.core.model.ScriptStackFrame;

/**
 * This is DBGP source module.
 * 
 */
public class DBGPSourceModule extends AbstractExternalSourceModule {

	private ScriptStackFrame frame;

	public DBGPSourceModule(ScriptProject parent, String name,
			WorkingCopyOwner owner, ScriptStackFrame frame) {
		super(parent, name, owner);

		this.frame = frame;
	}

	/*
	 * @see AbstractSourceModule#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof DBGPSourceModule)) {
			return false;
		}

		return super.equals(obj);
	}

	protected IStatus validateSourceModule(IResource resource) {
		/*
		 * XXX: is there a way to validate a remote resource?
		 */
		return IModelStatus.VERIFIED_OK;
	}

	/*
	 * @see org.eclipse.core.resources.IStorage#getContents()
	 */
	public InputStream getContents() throws CoreException {
		try {
			byte[] contents = lookupSource().getBytes();
			return new ByteArrayInputStream(contents);
		} catch (DbgpException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					DLTKCore.PLUGIN_ID,
					Messages.DBGPSourceModule_dbgpSourceLookup, e));
		}
	}

	/*
	 * @see org.eclipse.dltk.compiler.env.IDependent#getFileName()
	 */
	public String getFileName() {
		/*
		 * XXX: remote source should not be touched by compiler
		 * 
		 * remove this and just make the other sub-classes implement
		 * org.eclipse.dltk.compiler.env.IDependent directly?
		 */
		return Util.EMPTY_STRING;
	}

	/*
	 * @see org.eclipse.dltk.internal.core.AbstractSourceModule#getPath()
	 */
	public IPath getPath() {
		/*
		 * return the path of the file on the remote host, allows remote
		 * breakpoints to be set in debugger engine
		 */
		return new Path(frame.getFileName().getPath());
	}

	/*
	 * @see org.eclipse.core.resources.IStorage#getFullPath()
	 */
	public IPath getFullPath() {
		return null;
	}

	/*
	 * @see org.eclipse.core.resources.IStorage#getName()
	 */
	public String getName() {
		return "DBGP: " + getElementName(); //$NON-NLS-1$
	}

	/*
	 * @see AbstractSourceModule#getBufferContent()
	 */
	protected char[] getBufferContent() throws ModelException {
		try {
			return lookupSource().toCharArray();
		} catch (DbgpException e) {
			throw new ModelException(e, IStatus.ERROR);
		}
	}

	/**
	 * FIXME implement caching in the ScriptDebugTarget
	 */
	private String cachedSource = null;

	private String lookupSource() throws DbgpException {
		if (cachedSource == null) {
			/*
			 * XXX: this has problems if the encodings on both hosts don't match
			 * - see getBufferContents/getContents
			 */
			URI uri = frame.getSourceURI();
			cachedSource = frame.getScriptThread().getDbgpSession()
					.getCoreCommands().getSource(uri);
		}
		return cachedSource;
	}

	/*
	 * @see AbstractExternalSourceModule#getModuleType()
	 */
	protected String getModuleType() {
		return "DLTK Remote Source Module"; //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.dltk.internal.core.AbstractSourceModule#getNatureId()
	 */
	protected String getNatureId() {
		IDLTKLanguageToolkit toolkit = lookupLanguageToolkit(getParent());
		if (toolkit == null)
			return null;

		return toolkit.getNatureId();
	}

	/*
	 * @see AbstractSourceModule#getOriginalSourceModule()
	 */
	protected ISourceModule getOriginalSourceModule() {
		return new DBGPSourceModule((ScriptProject) getParent(),
				getElementName(), DefaultWorkingCopyOwner.PRIMARY, frame);
	}
}
