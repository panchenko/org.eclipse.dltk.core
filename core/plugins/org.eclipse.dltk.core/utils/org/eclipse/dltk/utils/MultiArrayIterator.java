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

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class MultiArrayIterator<E> implements Iterator<E> {
	private E[] current;
	private int next;

	private final E[][] arrays;
	private int nextArray;

	@SuppressWarnings("unchecked")
	public MultiArrayIterator(E[] first, E[] second) {
		this.current = first;
		this.arrays = (E[][]) new Object[][] { second };
		advance();
	}

	@SuppressWarnings("unchecked")
	public MultiArrayIterator(Collection<E[]> arrays) {
		this.arrays = (E[][]) arrays.toArray(new Object[arrays.size()][]);
		if (this.arrays.length == 0) {
			current = (E[]) org.eclipse.dltk.compiler.util.Util.EMPTY_ARRAY;
		} else {
			current = this.arrays[0];
			nextArray = 1;
		}
		advance();
	}

	public boolean hasNext() {
		return next < current.length;
	}

	public E next() {
		final E result;
		try {
			result = current[next++];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new NoSuchElementException();
		}
		advance();
		return result;
	}

	private void advance() {
		if (next == current.length) {
			while (nextArray < arrays.length) {
				current = arrays[nextArray++];
				if (current.length != 0) {
					next = 0;
					break;
				}
			}
		}
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}