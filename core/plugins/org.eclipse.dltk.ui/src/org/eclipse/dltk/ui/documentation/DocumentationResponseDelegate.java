/*******************************************************************************
 * Copyright (c) 2011 NumberFour AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     NumberFour AG - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.ui.documentation;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;

class DocumentationResponseDelegate implements IDocumentationResponse {
	private final IDocumentationResponse target;

	public String getTitle() {
		return target.getTitle();
	}

	public ImageDescriptor getImage() {
		return target.getImage();
	}

	public Object getObject() {
		return target.getObject();
	}

	public URL getURL() throws IOException {
		return target.getURL();
	}

	public Reader getReader() throws IOException {
		return target.getReader();
	}

	public String getText() throws IOException {
		return target.getText();
	}

	public DocumentationResponseDelegate(IDocumentationResponse target) {
		this.target = target;
	}

}
