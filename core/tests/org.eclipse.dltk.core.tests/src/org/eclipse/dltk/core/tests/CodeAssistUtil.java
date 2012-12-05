/*******************************************************************************
 * Copyright (c) 2012 NumberFour AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     NumberFour AG - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.core.tests;

import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.osgi.util.NLS;
import org.junit.Assert;

public class CodeAssistUtil {

	private final ISourceModule module;
	private Integer offset;
	private int length;

	private CodeAssistUtil(ISourceModule module) {
		this.module = module;
	}

	public static CodeAssistUtil on(ISourceModule module) {
		return new CodeAssistUtil(module);
	}

	public CodeAssistUtil after(String marker) throws ModelException {
		return calculateOffset(marker, false, true);
	}

	public CodeAssistUtil afterLast(String marker) throws ModelException {
		return calculateOffset(marker, true, true);
	}

	public CodeAssistUtil before(String marker) throws ModelException {
		return calculateOffset(marker, false, false);
	}

	public CodeAssistUtil beforeLast(String marker) throws ModelException {
		return calculateOffset(marker, true, false);
	}

	private CodeAssistUtil calculateOffset(String marker, boolean last,
			boolean after) throws ModelException {
		final String text = module.getSource();
		final int offset = last ? text.lastIndexOf(marker) : text
				.indexOf(marker);
		Assert.assertTrue(
				NLS.bind("Pattern \"{0}\" not found in {1}", marker,
						module.getElementName()), offset != -1);
		this.offset = offset + (after ? marker.length() : 0);
		return this;
	}

	public CodeAssistUtil length(int length) {
		this.length = length;
		return this;
	}

	public IModelElement[] codeSelect() throws ModelException {
		return module.codeSelect(offset, length);
	}

}
