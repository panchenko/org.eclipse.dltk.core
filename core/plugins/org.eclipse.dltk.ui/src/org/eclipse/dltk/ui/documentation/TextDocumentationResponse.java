/*******************************************************************************
 * Copyright (c) 2010 xored software, Inc.  
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
import java.io.StringReader;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * @since 3.0
 */
public class TextDocumentationResponse extends AbstractDocumentationResponse {

	private final String content;
	private final String title;
	private final ImageDescriptor image;

	public TextDocumentationResponse(Object object, String content) {
		this(object, null, content);
	}

	public TextDocumentationResponse(Object object, String title, String content) {
		this(object, title, null, content);
	}

	public TextDocumentationResponse(Object object, String title,
			ImageDescriptor image, String content) {
		super(object);
		this.content = content;
		this.title = title;
		this.image = image;
	}

	public Reader getReader() throws IOException {
		return new StringReader(content);
	}

	@Override
	public String getText() {
		return content;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public ImageDescriptor getImage() {
		return image;
	}

}
