/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.core.model.binary;


/**
 * @since 2.0
 */
class BinaryTypeElementInfo extends BinaryMemberInfo {
	/**
	 * The name of the superclasses for this type.
	 */
	protected String[] superclassNames;

	protected void setSuperclassNames(String[] superclassNames) {
		this.superclassNames = superclassNames;
	}

	public String[] getSuperclassNames() {
		return superclassNames;
	}
}
