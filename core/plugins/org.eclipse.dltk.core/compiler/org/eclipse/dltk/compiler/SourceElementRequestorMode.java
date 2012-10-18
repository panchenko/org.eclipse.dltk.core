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
package org.eclipse.dltk.compiler;

public enum SourceElementRequestorMode {
	STRUCTURE, INDEX;

	public boolean matches(ISourceElementRequestor requestor) {
		return of(requestor) == this;
	}

	public static SourceElementRequestorMode of(
			ISourceElementRequestor requestor) {
		if (requestor instanceof ISourceElementRequestorExtension) {
			return ((ISourceElementRequestorExtension) requestor).getMode();
		} else {
			return null;
		}
	}
}
