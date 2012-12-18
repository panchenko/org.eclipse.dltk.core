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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.utils.AdaptUtils;

/**
 * @since 2.0
 */
public class DocumentationUtils {

	/**
	 * Returns the {@link Reader} for the specified
	 * {@link IDocumentationResponse} or <code>null</code>.
	 * 
	 * @param response
	 *            input response, probably <code>null</code>
	 * @return
	 */
	public static Reader getReader(IDocumentationResponse response) {
		if (response != null) {
			try {
				return response.getReader();
			} catch (IOException e) {
				if (DLTKCore.DEBUG)
					e.printStackTrace();
			}
		}
		return null;
	}

	public static String readAll(Reader rd) {
		final StringBuilder buffer = new StringBuilder();
		char[] readBuffer = new char[2048];
		try {
			int n = rd.read(readBuffer);
			while (n > 0) {
				buffer.append(readBuffer, 0, n);
				n = rd.read(readBuffer);
			}
			return buffer.toString();
		} catch (IOException x) {
		}
		return null;
	}

	/**
	 * @param member
	 * @param context
	 * @param reader
	 * @return
	 */
	public static IDocumentationResponse wrap(Object member,
			IAdaptable context, Reader reader) {
		if (reader != null) {
			final IScriptDocumentationTitleAdapter titleAdapter = AdaptUtils
					.getAdapter(context, IScriptDocumentationTitleAdapter.class);
			return new TextDocumentationResponse(
					member,
					titleAdapter != null ? titleAdapter.getTitle(member) : null,
					titleAdapter != null ? titleAdapter.getImage(member) : null,
					readAll(reader));
		}
		return null;
	}

}
