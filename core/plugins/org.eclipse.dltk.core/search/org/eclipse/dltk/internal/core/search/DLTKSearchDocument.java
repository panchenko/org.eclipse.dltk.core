/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.internal.core.search;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.core.search.SearchDocument;
import org.eclipse.dltk.core.search.SearchParticipant;

public class DLTKSearchDocument extends SearchDocument {
	protected char[] charContents;
	private boolean external;

	public DLTKSearchDocument(String path, char[] contents,
			SearchParticipant participant, boolean external, IProject project) {
		super(path, participant, project);
		this.charContents = contents;
		this.external = external;
	}

	public DLTKSearchDocument(String path, IPath containerPath,
			char[] contents, SearchParticipant participant, boolean external,
			IProject project) {
		super(IDLTKSearchScope.FILE_ENTRY_SEPARATOR + path, participant,
				project);
		this.charContents = contents;
		this.external = external;
	}

	public void setCharContents(char[] charContents) {
		this.charContents = charContents;
	}

	public String getContents() {
		return new String(charContents);
	}

	public char[] getCharContents() {
		return charContents;
	}

	public String getEncoding() {
		try {
			return ResourcesPlugin.getWorkspace().getRoot().getDefaultCharset();
		} catch (CoreException e) {
			// use no encoding
		}
		return null;
	}

	public String toString() {
		return "SearchDocument(" + getPath() + ')'; //$NON-NLS-1$
	}

	public boolean isExternal() {
		return external;
	}
}
