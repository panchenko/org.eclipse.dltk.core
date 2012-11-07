/*******************************************************************************
 * Copyright (c) 2009 xored software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.ui.documentation;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * @since 2.0
 */
public abstract class AbstractDocumentationResponse implements
		IDocumentationResponse {

	private final Object object;

	public AbstractDocumentationResponse(Object object) {
		this.object = object;
	}

	/**
	 * @since 3.0
	 */
	public String getTitle() {
		return null;
	}

	public ImageDescriptor getImage() {
		return null;
	}

	public Object getObject() {
		return object;
	}

	public URL getURL() throws IOException {
		return null;
	}

	public String getText() throws IOException {
		final Reader reader = getReader();
		try {
			return DocumentationUtils.readAll(reader);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// ignore close exception
			}
		}
	}

}
