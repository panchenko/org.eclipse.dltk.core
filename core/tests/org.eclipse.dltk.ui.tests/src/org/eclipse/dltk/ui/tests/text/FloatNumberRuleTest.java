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
package org.eclipse.dltk.ui.tests.text;

import static org.eclipse.jface.text.rules.Token.UNDEFINED;
import junit.framework.TestCase;

import org.eclipse.dltk.ui.text.rules.FloatNumberRule;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

public class FloatNumberRuleTest extends TestCase {

	static final IToken NUMBER = new Token("Number");

	static class TestScanner extends BufferedRuleBasedScanner {
		int getOffset() {
			return fOffset;
		}
	}

	private static class Value {
		final IToken token;
		final int length;

		public Value(IToken token, int length) {
			assert token == NUMBER || token == Token.UNDEFINED;
			this.token = token;
			this.length = length;
		}

		@Override
		public String toString() {
			return (token == NUMBER ? "NUMBER" : "UNDEFINED") + "," + length;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Value) {
				final Value other = (Value) obj;
				return token == other.token && length == other.length;
			}
			return false;
		}
	}

	static Value value(IToken token, int offset) {
		return new Value(token, offset);
	}

	private final FloatNumberRule rule = new FloatNumberRule(NUMBER, null, "L",
			"F");

	private Value evaluate(final String value) {
		final TestScanner scanner = new TestScanner();
		scanner.setRange(new Document(value), 0, value.length());
		final IToken token = rule.evaluate(scanner);
		return new Value(token, scanner.getOffset());
	}

	public void testDecimal() {
		assertEquals(value(NUMBER, 3), evaluate("123"));
		assertEquals(value(NUMBER, 3), evaluate("123,"));
	}

	public void testHex() {
		assertEquals(value(NUMBER, 4), evaluate("0xFF"));
		assertEquals(value(NUMBER, 4), evaluate("0xFF,"));
		assertEquals(value(UNDEFINED, 0), evaluate("0x,"));
	}

	public void testFloat() {
		assertEquals(value(NUMBER, 1), evaluate("1"));
		assertEquals(value(NUMBER, 2), evaluate("1L"));
		assertEquals(value(NUMBER, 2), evaluate("1F"));
		assertEquals(value(NUMBER, 2), evaluate(".1"));
		assertEquals(value(NUMBER, 4), evaluate(".1e1,"));
		assertEquals(value(NUMBER, 2), evaluate(".1,"));
		assertEquals(value(NUMBER, 3), evaluate("1.2"));
		assertEquals(value(NUMBER, 3), evaluate("1.2,"));
		assertEquals(value(NUMBER, 4), evaluate("1.2F"));
		assertEquals(value(NUMBER, 4), evaluate("1.2F,"));
		assertEquals(value(NUMBER, 5), evaluate("1.2e1"));
		assertEquals(value(NUMBER, 5), evaluate("1.2e1,"));
		assertEquals(value(NUMBER, 6), evaluate("1.2e1F,"));
		assertEquals(value(NUMBER, 3), evaluate("1e1"));
		assertEquals(value(NUMBER, 3), evaluate("1e1,"));
		assertEquals(value(NUMBER, 4), evaluate("1e+1"));
		assertEquals(value(NUMBER, 5), evaluate("1e+1F,"));
		assertEquals(value(NUMBER, 4), evaluate("1e-1"));
		assertEquals(value(UNDEFINED, 0), evaluate("1e+"));
	}

	public void testDot() {
		assertEquals(value(UNDEFINED, 0), evaluate("."));
		assertEquals(value(UNDEFINED, 0), evaluate("..1"));
		assertEquals(value(UNDEFINED, 0), evaluate(".a"));
		assertEquals(value(UNDEFINED, 0), evaluate(".x"));
		assertEquals(value(UNDEFINED, 0), evaluate("a.x"));
	}

}
