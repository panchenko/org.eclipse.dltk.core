/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.launching;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.environment.IEnvironment;
import org.eclipse.dltk.core.environment.IExecutionEnvironment;
import org.eclipse.dltk.core.environment.IFileHandle;
import org.eclipse.dltk.internal.launching.DLTKLaunchingPlugin;
import org.eclipse.dltk.internal.launching.DebugRunnerDelegate;
import org.eclipse.dltk.internal.launching.IInterpreterInstallExtensionContainer;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

/**
 * Abstract implementation of a interpreter install.
 * <p>
 * Clients implementing interpreter installs must subclass this class.
 * </p>
 * 
 * @since 2.0
 */
public abstract class AbstractInterpreterInstall implements
		IInterpreterInstall, IInterpreterInstallExtensionContainer {
	private IInterpreterInstallType fType;
	private String fId;
	private String fName;
	private IFileHandle fInstallLocation;
	private LibraryLocation[] fSystemLibraryDescriptions;
	private String fInterpreterArgs;
	private EnvironmentVariable[] fEnvironmentVariables;

	// whether change events should be fired
	private boolean fNotify = true;

	private void firePropertyChangeEvent(PropertyChangeEvent event) {
		if (fNotify) {
			ScriptRuntime.fireInterpreterChanged(event);
		}
	}

	/**
	 * Constructs a new interpreter install.
	 * 
	 * @param type
	 *            The type of this interpreter install. Must not be
	 *            <code>null</code>
	 * @param id
	 *            The unique identifier of this interpreter instance Must not be
	 *            <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if any of the required parameters are <code>null</code>.
	 */
	public AbstractInterpreterInstall(IInterpreterInstallType type, String id) {
		if (type == null || id == null) {
			throw new IllegalArgumentException();
		}

		fType = type;
		fId = id;
	}

	public String getId() {
		return fId;
	}

	public String getNatureId() {
		return fType.getNatureId();
	}

	public String getName() {
		return fName;
	}

	public void setName(String name) {
		if (!name.equals(fName)) {
			PropertyChangeEvent event = new PropertyChangeEvent(this,
					IInterpreterInstallChangedListener.PROPERTY_NAME, fName,
					name);
			fName = name;

			firePropertyChangeEvent(event);
		}
	}

	public IFileHandle getInstallLocation() {
		// return PlatformFileUtils
		// .findAbsoluteOrEclipseRelativeFile(fInstallLocation);
		return fInstallLocation;
	}

	public IFileHandle getRawInstallLocation() {
		return fInstallLocation;
	}

	public IEnvironment getEnvironment() {
		if (fInstallLocation != null)
			return fInstallLocation.getEnvironment();
		return null;
	}

	/*
	 * @see org.eclipse.dltk.launching.IInterpreterInstall#getEnvironmentId()
	 */
	public String getEnvironmentId() {
		if (fInstallLocation != null)
			return fInstallLocation.getEnvironmentId();
		return null;
	}

	public IExecutionEnvironment getExecEnvironment() {
		IEnvironment environment = getEnvironment();
		if (environment != null) {
			return (IExecutionEnvironment) environment
					.getAdapter(IExecutionEnvironment.class);
		}
		return null;
	}

	public void setInstallLocation(IFileHandle installLocation) {
		if (!installLocation.equals(fInstallLocation)) {
			PropertyChangeEvent event = new PropertyChangeEvent(
					this,
					IInterpreterInstallChangedListener.PROPERTY_INSTALL_LOCATION,
					fInstallLocation, installLocation);
			fInstallLocation = installLocation;
			firePropertyChangeEvent(event);
		}
	}

	public IInterpreterInstallType getInterpreterInstallType() {
		return fType;
	}

	public LibraryLocation[] getLibraryLocations() {
		return fSystemLibraryDescriptions;
	}

	public void setLibraryLocations(LibraryLocation[] locations) {
		if (locations == fSystemLibraryDescriptions) {
			return;
		}
		LibraryLocation[] newLocations = locations;
		if (newLocations == null) {
			newLocations = getInterpreterInstallType()
					.getDefaultLibraryLocations(getInstallLocation(),
							getEnvironmentVariables(), null);
		}
		LibraryLocation[] prevLocations = fSystemLibraryDescriptions;
		if (prevLocations == null) {
			prevLocations = getInterpreterInstallType()
					.getDefaultLibraryLocations(getInstallLocation(),
							getEnvironmentVariables(), null);
		}

		if (newLocations.length == prevLocations.length) {
			int i = 0;
			boolean equal = true;
			while (i < newLocations.length && equal) {
				equal = newLocations[i].equals(prevLocations[i]);
				i++;
			}
			if (equal) {
				return;
			}
		}

		PropertyChangeEvent event = new PropertyChangeEvent(this,
				IInterpreterInstallChangedListener.PROPERTY_LIBRARY_LOCATIONS,
				prevLocations, newLocations);
		fSystemLibraryDescriptions = locations;

		firePropertyChangeEvent(event);
	}

	/**
	 * Whether this Interpreter should fire property change notifications.
	 * 
	 * @param notify
	 */
	protected void setNotify(boolean notify) {
		fNotify = notify;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof IInterpreterInstall) {
			IInterpreterInstall interpreter = (IInterpreterInstall) object;
			return getInterpreterInstallType().equals(
					interpreter.getInterpreterInstallType())
					&& getId().equals(interpreter.getId());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getInterpreterInstallType().hashCode() + getId().hashCode();
	}

	public String[] getInterpreterArguments() {
		String args = getInterpreterArgs();
		if (args == null) {
			return null;
		}
		ExecutionArguments ex = new ExecutionArguments(args, ""); //$NON-NLS-1$
		return ex.getInterpreterArgumentsArray();
	}

	public void setInterpreterArguments(String[] args) {
		if (args == null) {
			setInterpreterArgs(null);
		} else {
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < args.length; i++) {
				String string = args[i];
				buf.append(string);
				buf.append(' ');
			}
			setInterpreterArgs(buf.toString().trim());
		}
	}

	public String getInterpreterArgs() {
		return fInterpreterArgs;
	}

	public void setInterpreterArgs(String args) {
		if (fInterpreterArgs == null) {
			if (args == null) {
				return;
			}
		} else if (fInterpreterArgs.equals(args)) {
			return;
		}
		PropertyChangeEvent event = new PropertyChangeEvent(
				this,
				IInterpreterInstallChangedListener.PROPERTY_Interpreter_ARGUMENTS,
				fInterpreterArgs, args);
		fInterpreterArgs = args;

		firePropertyChangeEvent(event);
	}

	/**
	 * Throws a core exception with an error status object built from the given
	 * message, lower level exception, and error code.
	 * 
	 * @param message
	 *            the status message
	 * @param exception
	 *            lower level exception associated with the error, or
	 *            <code>null</code> if none
	 * @param code
	 *            error code
	 * @throws CoreException
	 *             the "abort" core exception
	 * 
	 */
	protected void abort(String message, Throwable exception, int code)
			throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR,
				DLTKLaunchingPlugin.getUniqueIdentifier(), code, message,
				exception));
	}

	// IBuiltinModuleProvider
	public String[] getBuiltinModules() {
		return null;
	}

	public String getBuiltinModuleContent(String name) {
		return null;
	}

	public long lastModified() {
		return 0;
	}

	protected IInterpreterRunner getDebugInterpreterRunner() {
		return new DebugRunnerDelegate(this);
	}

	public IInterpreterRunner getInterpreterRunner(String mode) {
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			return getDebugInterpreterRunner();
		}

		return null;
	}

	public EnvironmentVariable[] getEnvironmentVariables() {
		return fEnvironmentVariables;
	}

	public void setEnvironmentVariables(EnvironmentVariable[] variables) {
		PropertyChangeEvent event = new PropertyChangeEvent(
				this,
				IInterpreterInstallChangedListener.PROPERTY_ENVIRONMENT_VARIABLES,
				this.fEnvironmentVariables, variables);
		this.fEnvironmentVariables = variables;
		firePropertyChangeEvent(event);
	}

	private XMLResource resource = null;

	/**
	 * @since 2.0
	 */
	protected XMIResource createResource() {
		final XMIResourceImpl r = new XMIResourceImpl();
		r.setEncoding(ENCODING);
		return r;
	}

	private static final String ENCODING = "UTF-8"; //$NON-NLS-1$

	/**
	 * @since 2.0
	 */
	public EObject findExtension(EClass clazz) {
		if (resource != null) {
			for (EObject object : resource.getContents()) {
				if (clazz.equals(object.eClass())) {
					return object;
				}
			}
		}
		return null;
	}

	/**
	 * @since 2.0
	 */
	public EObject replaceExtension(EClass clazz, EObject value) {
		if (value != null) {
			Assert.isLegal(clazz.equals(value.eClass()));
		}
		if (resource == null) {
			resource = createResource();
		}
		for (ListIterator<EObject> i = resource.getContents().listIterator(); i
				.hasNext();) {
			EObject object = i.next();
			if (clazz.equals(object.eClass())) {
				if (value != null) {
					i.set(value);
				} else {
					i.remove();
				}
				firePropertyChangeEvent(new PropertyChangeEvent(this,
						IInterpreterInstallChangedListener.PROPERTY_EXTENSIONS,
						Collections.singletonList(object),
						value != null ? Collections.singletonList(value) : null));
				return object;
			}
		}
		if (value != null) {
			resource.getContents().add(value);
			firePropertyChangeEvent(new PropertyChangeEvent(this,
					IInterpreterInstallChangedListener.PROPERTY_EXTENSIONS,
					null, Collections.singletonList(value)));
		}
		return null;
	}

	/**
	 * @since 2.0
	 */
	public List<EObject> copyExtensions() {
		if (resource != null && !resource.getContents().isEmpty()) {
			Collection<EObject> copy = EcoreUtil
					.copyAll(resource.getContents());
			if (copy instanceof List<?>) {
				return (List<EObject>) copy;
			} else {
				return new ArrayList<EObject>(copy);
			}
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * @since 2.0
	 */
	public List<EObject> getExtensions() {
		if (resource != null && !resource.getContents().isEmpty()) {
			return new ArrayList<EObject>(resource.getContents());
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * @since 2.0
	 */
	public void setExtensions(List<EObject> value) {
		final List<EObject> oldValue;
		if (resource == null) {
			resource = createResource();
			oldValue = null;
		} else {
			oldValue = new ArrayList<EObject>(resource.getContents());
			resource.getContents().clear();
		}
		resource.getContents().addAll(value);
		firePropertyChangeEvent(new PropertyChangeEvent(this,
				IInterpreterInstallChangedListener.PROPERTY_EXTENSIONS,
				oldValue, value));
	}

	/**
	 * @since 2.0
	 */
	public String saveExtensions() {
		if (resource != null && !resource.getContents().isEmpty()) {
			StringWriter stringWriter = new StringWriter();
			try {
				Map<String, Object> saveOptions = new HashMap<String, Object>();
				saveOptions.put(XMLResource.OPTION_DECLARE_XML, Boolean.FALSE);
				saveOptions.put(XMLResource.OPTION_FORMATTED, Boolean.FALSE);
				resource.save(new URIConverter.WriteableOutputStream(
						stringWriter, ENCODING), saveOptions);
			} catch (IOException e) {
				if (DLTKCore.DEBUG) {
					e.printStackTrace();
				}
			}
			return stringWriter.toString();
		} else {
			return null;
		}
	}

	/**
	 * @since 2.0
	 */
	public void loadExtensions(String value) {
		if (value != null && value.length() != 0) {
			if (resource == null) {
				resource = createResource();
			}
			try {
				resource.load(new URIConverter.ReadableInputStream(value,
						ENCODING), null);
			} catch (IOException e) {
				if (DLTKCore.DEBUG) {
					e.printStackTrace();
				}
			}
		} else {
			if (resource != null) {
				resource.getContents().clear();
			}
		}
	}

}
