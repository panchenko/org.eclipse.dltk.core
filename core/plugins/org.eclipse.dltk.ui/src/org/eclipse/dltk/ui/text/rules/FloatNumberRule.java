/*******************************************************************************
 * Copyright (c) 2009, 2012 xored software, Inc. and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *     NumberFour AG - made reusable by introducing constructor parameters (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.ui.text.rules;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * An implementation of <code>IRule</code> detecting a numerical value.
 * <p>
 * Supported number formats are:
 * <ul>
 * <li>integer
 * <li>floating point (with dot and exponent)
 * <li>hexadecimal (with 0x prefix)
 * </ul>
 * </p>
 */
public class FloatNumberRule implements IRule {

	private final IToken fToken;
	private final String fDigitSeparators;
	private final String fDecimalSuffixes;
	private final String fFloatSuffixes;

	public FloatNumberRule(IToken token) {
		this(token, null, null, null);
	}

	/**
	 * @param token
	 * @param digitSeparators
	 *            additional digits separator, e.g. "_" for java7 or
	 *            <code>null</code> if not applicable
	 * @param integerSuffixes
	 *            integer number suffixes, e.g. "Ll" for java or
	 *            <code>null</code> if not applicable
	 * @param floatSuffixes
	 *            floating point number suffixes, e.g. "DdFf" for java or
	 *            <code>null</code> if not applicable
	 */
	public FloatNumberRule(IToken token, String digitSeparators,
			String integerSuffixes, String floatSuffixes) {
		Assert.isNotNull(token);
		fToken = token;
		fDigitSeparators = digitSeparators;
		fDecimalSuffixes = integerSuffixes;
		fFloatSuffixes = floatSuffixes;
	}

	private enum Mode {
		DECIMAL, HEX, FLOAT
	}

	public IToken evaluate(ICharacterScanner scanner) {
		int c = scanner.read();
		if (Character.isDigit((char) c) || c == '.') {
			int readCount = 1;
			int digitCount;
			Mode mode;
			if (c == '.') {
				mode = Mode.FLOAT;
				digitCount = 0;
			} else {
				mode = Mode.DECIMAL;
				digitCount = 1;
			}
			for (;;) {
				final char lastDigit = (char) c;
				c = scanner.read();
				++readCount;
				if (c == 'x' || c == 'X') {
					if (mode == Mode.DECIMAL && digitCount == 1
							&& lastDigit == '0') {
						mode = Mode.HEX;
						digitCount = 0;
					} else {
						unread(scanner, readCount);
						return Token.UNDEFINED; // not "0x" prefix
					}
				} else if (Character.isDigit((char) c)
						|| isSeparator(c)
						|| (mode == Mode.HEX && (c >= 'A' && c <= 'F' || c >= 'a'
								&& c <= 'f'))) {
					digitCount++;
				} else if (c == '.' && mode == Mode.DECIMAL) {
					mode = Mode.FLOAT;
				} else {
					break;
				}
			}
			if (digitCount == 0) { // "0x" or "." only
				unread(scanner, readCount);
				return Token.UNDEFINED;
			}
			if ((c == 'e' || c == 'E')
					&& (mode == Mode.DECIMAL || mode == Mode.FLOAT)) {
				mode = Mode.FLOAT;
				c = scanner.read();
				++readCount;
				if (c == '+' || c == '-') {
					c = scanner.read();
					++readCount;
				}
				if (Character.isDigit((char) c)) {
					do {
						++readCount;
						c = scanner.read();
					} while (Character.isDigit((char) c) || isSeparator(c));
				} else {
					unread(scanner, readCount);
					return Token.UNDEFINED;
				}
			}
			if (!(((mode == Mode.DECIMAL || mode == Mode.FLOAT) && isFloatSuffix(c)) || (mode == Mode.DECIMAL && isDecimalSuffix(c)))) {
				scanner.unread();
			}
			return fToken;
		} else {
			scanner.unread();
			return Token.UNDEFINED;
		}
	}

	private final void unread(ICharacterScanner scanner, int count) {
		while (--count >= 0) {
			scanner.unread();
		}
	}

	private final boolean isSeparator(int c) {
		return fDigitSeparators != null && fDigitSeparators.indexOf(c) >= 0;
	}

	private final boolean isDecimalSuffix(int c) {
		return fDecimalSuffixes != null && fDecimalSuffixes.indexOf(c) >= 0;
	}

	private final boolean isFloatSuffix(int c) {
		return fFloatSuffixes != null && fFloatSuffixes.indexOf(c) >= 0;
	}

}
