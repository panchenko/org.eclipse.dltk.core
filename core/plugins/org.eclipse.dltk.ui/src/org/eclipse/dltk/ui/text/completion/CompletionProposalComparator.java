/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.ui.text.completion;

import java.util.Comparator;

import org.eclipse.dltk.ui.templates.ScriptTemplateProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.TemplateProposal;

/**
 * Comparator for script completion proposals. Completion proposals can be
 * sorted by relevance or alphabetically.
 * <p>
 * Note: this comparator imposes orderings that are inconsistent with equals.
 * </p>
 * 
 */
public final class CompletionProposalComparator implements
		Comparator<ICompletionProposal> {

	private boolean fOrderAlphabetically;

	/**
	 * Creates a comparator that sorts by relevance.
	 */
	public CompletionProposalComparator() {
		fOrderAlphabetically = false;
	}

	/**
	 * Sets the sort order. Default is <code>false</code>, i.e. order by
	 * relevance.
	 * 
	 * @param orderAlphabetically
	 *            <code>true</code> to order alphabetically, <code>false</code>
	 *            to order by relevance
	 */
	public void setOrderAlphabetically(boolean orderAlphabetically) {
		fOrderAlphabetically = orderAlphabetically;
	}

	public int compare(ICompletionProposal p1, ICompletionProposal p2) {

		if (!fOrderAlphabetically) {
			int relevanceDif = getRelevance(p2) - getRelevance(p1);
			if (relevanceDif != 0) {
				return relevanceDif;
			}
			relevanceDif = getSortKey(p1).compareToIgnoreCase(getSortKey(p2));
			if (relevanceDif != 0) {
				return relevanceDif;
			}
			relevanceDif = getSubRelevance(p2) - getSubRelevance(p1);
			if (relevanceDif != 0) {
				return relevanceDif;
			}
			return getSubSortKey(p1).compareToIgnoreCase(getSubSortKey(p2));
		}
		/*
		 * TODO the correct (but possibly much slower) sorting would use a
		 * collator.
		 */
		// fix for bug 67468
		return getSortKey(p1).compareToIgnoreCase(getSortKey(p2));
	}

	private String getSortKey(ICompletionProposal p) {
		if (p instanceof AbstractScriptCompletionProposal) {
			String sortString = ((AbstractScriptCompletionProposal) p)
					.getSortString();
			if (sortString != null)
				return sortString;
		} else if (p instanceof ScriptTemplateProposal) {
			return ((ScriptTemplateProposal) p).getTemplateName();
		}
		return p.getDisplayString();
	}

	private int getRelevance(ICompletionProposal obj) {
		if (obj instanceof IScriptCompletionProposal) {
			IScriptCompletionProposal jcp = (IScriptCompletionProposal) obj;
			return jcp.getRelevance();
		} else if (obj instanceof TemplateProposal) {
			TemplateProposal tp = (TemplateProposal) obj;
			return tp.getRelevance();
		}
		// catch all
		return 0;
	}

	private int getSubRelevance(ICompletionProposal obj) {
		if (obj instanceof IScriptCompletionProposal) {
			return 1;
		} else if (obj instanceof TemplateProposal) {
			return 2;
		}
		// catch all
		return 0;
	}

	private String getSubSortKey(ICompletionProposal obj) {
		if (obj instanceof TemplateProposal) {
			return ((TemplateProposal) obj).getDisplayString();
		} else {
			return "";
		}
	}

}
