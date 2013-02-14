/*******************************************************************************
 * Copyright (c) 2013 NumberFour AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     NumberFour AG - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.core;

/**
 * The marker interface for the custom {@link CompletionProposal} flags, the
 * recommended way to create instances of this interface is {@code enum}.
 * 
 * @see CompletionProposal#setFlag(CompletionProposalFlag)
 * @see CompletionProposal#hasFlag(CompletionProposalFlag)
 */
public interface CompletionProposalFlag {
}
