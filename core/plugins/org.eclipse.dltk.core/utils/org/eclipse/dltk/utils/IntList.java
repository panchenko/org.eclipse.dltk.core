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
package org.eclipse.dltk.utils;

public class IntList {
	private int[] values;
	private int length = 0;

	public IntList() {
		this(16);
	}

	public IntList(int initialSize) {
		this.values = new int[initialSize];
	}

	public void add(int value) {
		if (values.length == length) {
			System.arraycopy(values, 0, values = new int[this.length * 2], 0,
					length);
		}
		values[length++] = value;
	}

	public int get(int index) {
		assert index < length;
		return values[index];
	}

	public int size() {
		return length;
	}

	public void setSize(int newSize) {
		// FIXME
		this.length = newSize;
	}

	public int[] toArray() {
		if (length == values.length) {
			return values;
		} else {
			final int[] result = new int[length];
			System.arraycopy(values, 0, result, 0, length);
			return result;
		}
	}

}