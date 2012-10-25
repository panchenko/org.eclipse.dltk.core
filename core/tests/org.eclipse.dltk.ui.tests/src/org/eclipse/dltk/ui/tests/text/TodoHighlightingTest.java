/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Sam Faktorovich)
 *******************************************************************************/
package org.eclipse.dltk.ui.tests.text;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.dltk.ui.text.DLTKColorConstants;
import org.eclipse.dltk.ui.text.rules.CombinedWordRule;
import org.eclipse.jface.text.rules.IToken;

public class TodoHighlightingTest extends TestCase {

	private static final String COMMENT_KEY = DLTKColorConstants.DLTK_SINGLE_LINE_COMMENT;
	private static final String TODO_KEY = DLTKColorConstants.TASK_TAG;

	public TodoHighlightingTest(String name) {
		super(name);
	}

	private TestScriptCommentScanner createScanner(String[] tags,
			boolean caseSensitive) {
		return new TestScriptCommentScanner(tags, COMMENT_KEY, TODO_KEY,
				caseSensitive);
	}

	private IToken evaluateTodoRule(final TestScriptCommentScanner scanner) {
		final int oldOffset = scanner.getTokenOffset();
		final IToken token = scanner.createTodoRule().evaluate(scanner);
		assertEquals(oldOffset, scanner.getTokenOffset());
		return token;
	}

	private IToken getDefaultToken(final TestScriptCommentScanner scanner) {
		return ((CombinedWordRule) scanner.createTodoRule()).getDefaultToken();
	}

	protected List<TokenPos> findTodoTokens(String data, String[] tags) {
		TestScriptCommentScanner scanner = createScanner(tags, true);
		scanner.setText(data);
		final IToken todoToken = scanner.getToken(TODO_KEY);
		List<TokenPos> result = new ArrayList<TokenPos>();
		IToken t;
		while (!(t = scanner.nextToken()).isEOF()) {
			if (t.equals(todoToken)) {
				result.add(new TokenPos(scanner.getTokenOffset(), scanner
						.getTokenLength()));
			}
		}
		return result;
	}

	/**
	 * @param data
	 * @param tag
	 * @return
	 */
	protected List<TokenPos> findTodoTokens(String data, String tag) {
		return findTodoTokens(data, new String[] { tag });
	}

	private TokenPos newTokenPos(final String data, final String tag) {
		return new TokenPos(data.indexOf(tag), tag.length());
	}

	private static final String TODO = "TODO";
	private static final String FIXME = "FIXME";

	public void testTodoRuleMiss1() {
		final TestScriptCommentScanner scanner = createScanner(new String[] {
				TODO, FIXME }, true);
		scanner.setText("x");
		final IToken token = evaluateTodoRule(scanner);
		assertEquals(token, getDefaultToken(scanner));
		assertEquals(scanner.getText().length(), scanner.getTokenLength());
	}

	public void testTodoRuleMiss2() {
		final TestScriptCommentScanner scanner = createScanner(new String[] {
				TODO, FIXME }, true);
		scanner.setText("x" + TODO);
		final IToken token = evaluateTodoRule(scanner);
		assertEquals(token, getDefaultToken(scanner));
		assertEquals(scanner.getText().length(), scanner.getTokenLength());
	}

	public void testTodoRuleMiss3() {
		final TestScriptCommentScanner scanner = createScanner(new String[] {
				TODO, FIXME }, true);
		scanner.setText(TODO + "x");
		final IToken token = evaluateTodoRule(scanner);
		assertEquals(token, getDefaultToken(scanner));
		assertEquals(scanner.getText().length(), scanner.getTokenLength());
	}

	public void testTodoRuleMiss4() {
		final TestScriptCommentScanner scanner = createScanner(new String[] {
				TODO, FIXME }, true);
		scanner.setText(FIXME + "x");
		final IToken token = evaluateTodoRule(scanner);
		assertEquals(token, getDefaultToken(scanner));
		assertEquals(scanner.getText().length(), scanner.getTokenLength());
	}

	public void testTodoRuleMiss5() {
		final TestScriptCommentScanner scanner = createScanner(new String[] {
				TODO, FIXME }, true);
		scanner.setText("x" + FIXME);
		final IToken token = evaluateTodoRule(scanner);
		assertEquals(token, getDefaultToken(scanner));
		assertEquals(scanner.getText().length(), scanner.getTokenLength());
	}

	public void testTodoRuleMatch1() {
		final TestScriptCommentScanner scanner = createScanner(new String[] {
				TODO, FIXME }, true);
		scanner.setText(TODO);
		final IToken token = evaluateTodoRule(scanner);
		assertTrue(token.isOther());
		assertEquals(TODO.length(), scanner.getTokenLength());
	}

	public void testTodoRuleMatch2() {
		final TestScriptCommentScanner scanner = createScanner(new String[] {
				TODO, FIXME }, true);
		scanner.setText(FIXME);
		final IToken token = evaluateTodoRule(scanner);
		assertTrue(token.isOther());
		assertEquals(FIXME.length(), scanner.getTokenLength());
	}

	public void testTodoRuleMatch3() {
		final TestScriptCommentScanner scanner = createScanner(new String[] {
				TODO, FIXME }, true);
		scanner.setText(TODO + " ");
		final IToken token = evaluateTodoRule(scanner);
		assertTrue(token.isOther());
		assertEquals(TODO.length(), scanner.getTokenLength());
	}

	public void testTodoRuleMatch4() {
		final TestScriptCommentScanner scanner = createScanner(new String[] {
				TODO, FIXME }, true);
		scanner.setText(FIXME + " ");
		final IToken token = evaluateTodoRule(scanner);
		assertTrue(token.isOther());
		assertEquals(FIXME.length(), scanner.getTokenLength());
	}

	public void testBare() {
		final String data = "#TODO";
		List<TokenPos> tokens = findTodoTokens(data, TODO);
		assertEquals(1, tokens.size());
		assertEquals(newTokenPos(data, TODO), tokens.get(0));
	}

	public void testBare1() {
		final String data = "#TODO";
		List<TokenPos> tokens = findTodoTokens(data,
				new String[] { TODO, FIXME });
		assertEquals(1, tokens.size());
		assertEquals(newTokenPos(data, TODO), tokens.get(0));
	}

	public void testBare2() {
		final String data = "#FIXME";
		List<TokenPos> tokens = findTodoTokens(data,
				new String[] { TODO, FIXME });
		assertEquals(1, tokens.size());
		assertEquals(newTokenPos(data, FIXME), tokens.get(0));
	}

	public void testNormal() {
		final String data = "#TODO comment";
		List<TokenPos> tokens = findTodoTokens(data, TODO);
		assertEquals(1, tokens.size());
		assertEquals(newTokenPos(data, TODO), tokens.get(0));
	}

	public void testSpaced() {
		final String data = "#   TODO comment";
		List<TokenPos> tokens = findTodoTokens(data, TODO);
		assertEquals(1, tokens.size());
		assertEquals(newTokenPos(data, TODO), tokens.get(0));
	}

	public void testPrefixed() {
		final String data = "# aTODO comment";
		List<TokenPos> tokens = findTodoTokens(data, TODO);
		assertEquals(0, tokens.size());
	}

	public void testNoTodo() {
		final String data = "#hello";
		List<TokenPos> tokens = findTodoTokens(data, TODO);
		assertEquals(0, tokens.size());
	}

	public void testSuffixed() {
		final String data = "# TODOa comment";
		List<TokenPos> tokens = findTodoTokens(data, TODO);
		assertEquals(0, tokens.size());
	}

	public void testLegallySuffixed() {
		final String data = "# TODO: comment";
		List<TokenPos> tokens = findTodoTokens(data, TODO);
		assertEquals(1, tokens.size());
		assertEquals(newTokenPos(data, TODO), tokens.get(0));
	}

	public void testDoubleTagOccurence() throws Exception {
		String data = "# TODO add TODO tag support";
		List<TokenPos> tokens = findTodoTokens(data, TODO);
		assertEquals(1, tokens.size());
		assertEquals(newTokenPos(data, TODO), tokens.get(0));
	}

	public void testDifferentTags() {
		String data = "#FIXME tag support";
		List<TokenPos> tokens = findTodoTokens(data,
				new String[] { TODO, FIXME });
		assertEquals(1, tokens.size());
		assertEquals(newTokenPos(data, FIXME), tokens.get(0));
	}

}
