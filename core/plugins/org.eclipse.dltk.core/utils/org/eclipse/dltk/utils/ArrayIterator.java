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
package org.eclipse.dltk.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayIterator<T> implements Iterator<T> {
	private final T[] items;
	private int index;
	private final int endIndex;

	public ArrayIterator(T[] items) {
		this(items, 0, items.length);
	}

	public ArrayIterator(T[] items, int startIndex, int endIndex) {
		super();
		this.items = items;
		index = startIndex;
		this.endIndex = endIndex;
	}

	public boolean hasNext() {
		return index < endIndex;
	}

	public T next() throws NoSuchElementException {
		try {
			return items[index++];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new NoSuchElementException();
		}
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
