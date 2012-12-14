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
	public static final IntList EMPTY_LIST = new IntList(0) {
		@Override
		public void add(int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setSize(int newSize) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return "IntList.EMPTY_LIST";
		}
	};

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

	public int removeAt(int index) {
		final int result = values[index];
		int j = length - index - 1;
		if (j > 0) {
			System.arraycopy(values, index + 1, values, index, j);
		}
		length--;
		return result;
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

	public boolean isEmpty() {
		return length == 0;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i = 0; i < length; ++i) {
			if (i != 0)
				sb.append(',');
			sb.append(values[i]);
		}
		sb.append(']');
		return sb.toString();
	}

	public void clear() {
		this.length = 0;
	}

	public void set(int index, int value) {
		assert index < length;
		values[index] = value;
	}

	public int first() {
		return values[0];
	}

	public int last() {
		return values[length - 1];
	}

}
