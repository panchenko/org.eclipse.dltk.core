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
package org.eclipse.dltk.core.tests.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.dltk.utils.MultiArrayIterator;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link MultiArrayIterator}
 */
public class MultiArrayIteratorTest extends Assert {

	private static <E> List<E> toList(Iterator<E> iterator) {
		final List<E> list = new ArrayList<E>();
		while (iterator.hasNext()) {
			list.add(iterator.next());
		}
		return list;
	}

	@Test
	public void both() {
		assertEquals(Arrays.asList("A", "B"),
				toList(new MultiArrayIterator<String>(new String[] { "A" },
						new String[] { "B" })));
	}

	@Test
	public void firstEmpty() {
		assertEquals(Arrays.asList("B"), toList(new MultiArrayIterator<String>(
				new String[] {}, new String[] { "B" })));
	}

	@Test
	public void secondEmpty() {
		assertEquals(Arrays.asList("A"), toList(new MultiArrayIterator<String>(
				new String[] { "A" }, new String[] {})));
	}

}
