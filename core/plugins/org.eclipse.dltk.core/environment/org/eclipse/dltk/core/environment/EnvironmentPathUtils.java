/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.dltk.core.environment;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.internal.environment.LocalEnvironment;
import org.eclipse.dltk.internal.core.ExternalSourceModule;
import org.eclipse.osgi.util.NLS;

public class EnvironmentPathUtils {
	public static final String PATH_DELIMITER = ";"; //$NON-NLS-1$
	private static final char SEPARATOR = '/';

	public static IPath getFullPath(IEnvironment env, IPath path) {
		if (isFull(path)) {
			throw new RuntimeException(NLS.bind(
					Messages.EnvironmentPathUtils_invalidPath, path));
		}
		// if( path.segment(0).startsWith("#special#")) {
		// return path;
		// }
		String device = path.getDevice();
		if (device == null)
			device = Character.toString(IPath.DEVICE_SEPARATOR);

		return path.setDevice(env.getId() + SEPARATOR + device);
	}

	public static IPath getFullPath(String envId, IPath path) {
		if (isFull(path)) {
			throw new RuntimeException(NLS.bind(
					Messages.EnvironmentPathUtils_invalidPath, path));
		}
		// if( path.segment(0).startsWith("#special#")) {
		// return path;
		// }
		String device = path.getDevice();
		if (device == null)
			device = Character.toString(IPath.DEVICE_SEPARATOR);

		return path.setDevice(envId + SEPARATOR + device);
	}

	public static IPath getFullPath(File folder) {
		return getFullPath(LocalEnvironment.getInstance(),
				new Path(folder.getAbsolutePath()));
	}

	public static boolean isFull(IPath path) {
		String device = path.getDevice();
		return device != null && device.indexOf(SEPARATOR) >= 0;
	}

	public static IEnvironment getPathEnvironment(IPath path) {
		if (!isFull(path)) {
			return null;
		}

		String envId = path.getDevice();
		if (envId == null)
			return null;

		int index = envId.indexOf(SEPARATOR);
		envId = envId.substring(0, index);
		return EnvironmentManager.getEnvironmentById(envId);
	}

	public static IPath getLocalPath(IPath path) {
		// if( path.segment(0).startsWith("#special#")) {
		// return path;
		// }
		if (!isFull(path)) {
			return path;
			// throw new RuntimeException("Invalid path");
		}

		String device = path.getDevice();
		int index = device.indexOf(SEPARATOR);
		Assert.isTrue(index >= 0);
		device = device.substring(index + 1);
		if (device.length() == 1 && device.charAt(0) == IPath.DEVICE_SEPARATOR)
			device = null;

		return path.setDevice(device);
	}

	public static String getLocalPathString(IPath path) {
		IEnvironment env = getPathEnvironment(path);
		IPath localPath = getLocalPath(path);
		if (env != null) {
			return env.convertPathToString(localPath);
		}
		return localPath.toOSString();
	}

	public static IFileHandle getFile(IPath fullPath) {
		IEnvironment env = getPathEnvironment(fullPath);
		if (env == null)
			return null;

		IPath path = getLocalPath(fullPath);
		return env.getFile(path);
	}

	public static Map<IEnvironment, String> decodePaths(String concatenatedPaths) {
		Map<IEnvironment, String> result = new HashMap<IEnvironment, String>();
		if (concatenatedPaths != null) {
			String[] paths = concatenatedPaths
					.split(EnvironmentPathUtils.PATH_DELIMITER);
			for (int i = 0; i < paths.length; i++) {
				IPath path = Path.fromPortableString(paths[i]);
				IEnvironment env = EnvironmentPathUtils
						.getPathEnvironment(path);
				if (env != null) {
					String localPath = EnvironmentPathUtils
							.getLocalPathString(path);
					result.put(env, localPath);
				}
			}
		}
		return result;
	}

	public static String encodePaths(Map<IEnvironment, String> env2path) {
		StringBuffer concatenatedPaths = new StringBuffer();
		for (Map.Entry<IEnvironment, String> entry : env2path.entrySet()) {
			if (concatenatedPaths.length() != 0) {
				concatenatedPaths.append(EnvironmentPathUtils.PATH_DELIMITER);
			}
			IPath path = EnvironmentPathUtils.getFullPath(entry.getKey(),
					new Path(entry.getValue()));
			concatenatedPaths.append(path.toPortableString());
		}
		return concatenatedPaths.toString();
	}

	public static IFileHandle getFile(IEnvironment environment, IPath path) {
		if (isFull(path)) {
			return getFile(path);
		}
		return environment.getFile(path);
	}

	public static IFileHandle getFile(IResource resource) {
		IEnvironment environment = EnvironmentManager.getEnvironment(resource
				.getProject());
		if (environment == null) {
			return null;
		}
		if (environment.isLocal()) {
			return environment.getFile(resource.getLocation());
		} else {
			return environment.getFile(resource.getLocationURI());
		}
	}

	public static IFileHandle getFile(IModelElement element) {
		return getFile(element, true);
	}

	/**
	 * @since 2.0
	 */
	public static IFileHandle getFile(IModelElement element,
			boolean checkExistance) {

		IEnvironment environment = EnvironmentManager.getEnvironment(element);
		if (environment == null) {
			return null;
		}
		IPath path = element.getPath();
		if (element instanceof ExternalSourceModule) {
			path = ((ExternalSourceModule) element).getFullPath();
		}
		if (environment.isLocal()) {
			IResource res = element.getResource();
			if (res != null) {
				IPath loc = res.getLocation();
				URI uri = res.getLocationURI();

				if (loc != null) {
					return environment.getFile(loc);
				} else if (uri != null) {
					return environment.getFile(uri);
				} else {
					return null;
				}
			}
			IFileHandle file = environment.getFile(path);
			if (checkExistance) {
				if (file.exists()) {
					return file;
				}
			} else {
				return file;
			}
			return null;
		} else {
			IResource res = element.getResource();
			if (res != null) {
				return environment.getFile(res.getLocationURI());
			}
			IFileHandle file = environment.getFile(path);
			if (checkExistance) {
				if (file.exists()) {
					return file;
				}
			} else {
				return file;
			}
			return null;
		}
	}

}
