/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.ui.infoviews;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.internal.ui.DelegatedOpen;
import org.eclipse.dltk.ui.ScriptElementLabels;
import org.eclipse.dltk.utils.TextUtils;
import org.eclipse.osgi.util.NLS;

public class ModelElementArray {

	private final Object[] elements;

	/**
	 * @since 3.0
	 */
	public ModelElementArray(Object[] elements) {
		Assert.isLegal(elements != null && elements.length > 1);
		this.elements = elements;
	}

	/**
	 * @since 5.0
	 */
	public ModelElementArray(Collection<?> elements) {
		Assert.isLegal(elements != null && elements.size() > 1);
		this.elements = elements.toArray();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ModelElementArray) {
			final ModelElementArray other = (ModelElementArray) obj;
			return Arrays.equals(elements, other.elements);
		} else {
			return false;
		}
	}

	/**
	 * @return the elements
	 * @since 3.0
	 */
	public Object[] getElements() {
		return elements;
	}

	/**
	 * Checks that all elements are {@link IModelElement#METHOD} and have the
	 * same name.
	 * 
	 * @return
	 */
	private boolean isSingleMethodName() {
		if (elements[0] instanceof IModelElement) {
			final IModelElement element0 = (IModelElement) elements[0];
			if (element0.getElementType() == IModelElement.METHOD) {
				final String methodName = element0.getElementName();
				for (int i = 1; i < elements.length; ++i) {
					if (!(elements[i] instanceof IModelElement))
						return false;
					final IModelElement element = (IModelElement) elements[i];
					if (element.getElementType() != IModelElement.METHOD
							|| !methodName.equals(element.getElementName())) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	private static final long NAME_FLAGS = ScriptElementLabels.T_FULLY_QUALIFIED
			| ScriptElementLabels.T_CONTAINER_QUALIFIED;

	/**
	 * @return
	 */
	public String getContentDescription() {
		if (isSingleMethodName()) {
			return NLS
					.bind(InfoViewMessages.ContentDescription_multipleMethodsWithSameName,
							((IModelElement) elements[0]).getElementName());
		}
		final ScriptElementLabels labels = ScriptElementLabels.getDefault();
		final Set<String> names = new HashSet<String>();
		for (int i = 0; i < elements.length; ++i) {
			if (elements[i] instanceof IModelElement) {
				names.add(labels.getElementLabel((IModelElement) elements[i],
						NAME_FLAGS));
			} else if (elements[i] instanceof DelegatedOpen) {
				names.add(((DelegatedOpen) elements[i]).getName());
			}
		}
		return sortAndJoin(names, ", ");//$NON-NLS-1$
	}

	private String sortAndJoin(final Set<String> names, final String separator) {
		final List<String> nameList = new ArrayList<String>(names);
		Collections.sort(nameList);
		return join(nameList, separator);
	}

	private static final long TOOLTIP_FLAGS = ScriptElementLabels.T_FULLY_QUALIFIED
			| ScriptElementLabels.T_CONTAINER_QUALIFIED
			| ScriptElementLabels.M_APP_RETURNTYPE
			| ScriptElementLabels.F_APP_TYPE_SIGNATURE
			| ScriptElementLabels.M_PARAMETER_TYPES
			| ScriptElementLabels.M_PARAMETER_NAMES
			| ScriptElementLabels.M_EXCEPTIONS
			| ScriptElementLabels.F_PRE_TYPE_SIGNATURE
			| ScriptElementLabels.T_TYPE_PARAMETERS
			| ScriptElementLabels.APPEND_FILE
			| ScriptElementLabels.CU_QUALIFIED;

	/**
	 * @return
	 */
	public String getTitleTooltip() {
		final ScriptElementLabels labels = ScriptElementLabels.getDefault();
		final Set<String> names = new HashSet<String>();
		for (int i = 0; i < elements.length; ++i) {
			if (elements[i] instanceof IModelElement) {
				names.add(labels.getElementLabel((IModelElement) elements[i],
						TOOLTIP_FLAGS));
			} else if (elements[i] instanceof DelegatedOpen) {
				names.add(((DelegatedOpen) elements[i]).getName());
			}
		}
		return sortAndJoin(names, "\n");//$NON-NLS-1$
	}

	/**
	 * <p>
	 * Joins the elements of the provided <code>Collection</code> into a single
	 * String containing the provided elements.
	 * </p>
	 * 
	 * <p>
	 * No delimiter is added before or after the list. A <code>null</code>
	 * separator is the same as an empty String ("").
	 * </p>
	 * 
	 * @param collection
	 *            the <code>Collection</code> of values to join together, may be
	 *            null
	 * @param separator
	 *            the separator character to use, null treated as ""
	 * @return the joined String, <code>null</code> if null collection input
	 */
	private static String join(Collection<?> collection, String separator) {
		return TextUtils.join(collection, separator);
	}

}
