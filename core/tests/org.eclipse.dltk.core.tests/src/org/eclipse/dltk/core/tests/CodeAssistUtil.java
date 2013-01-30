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
package org.eclipse.dltk.core.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.dltk.codeassist.ICompletionEngine;
import org.eclipse.dltk.codeassist.ISelectionEngine;
import org.eclipse.dltk.codeassist.ISelectionRequestor;
import org.eclipse.dltk.compiler.env.IModuleSource;
import org.eclipse.dltk.core.CompletionProposal;
import org.eclipse.dltk.core.CompletionRequestor;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ISourceRange;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.tests.util.StringList;
import org.eclipse.osgi.util.NLS;
import org.junit.Assert;

public class CodeAssistUtil {

	static abstract class Module<M> {
		final M module;

		public Module(M module) {
			this.module = module;
		}

		abstract String getSource();

		abstract String getName();

		abstract IModelElement[] codeSelect(int offset, int length)
				throws ModelException;

		abstract void codeComplete(int offset, CompletionRequestor requestor)
				throws ModelException;

	}

	private static class SourceModule extends Module<ISourceModule> {

		public SourceModule(ISourceModule module) {
			super(module);
		}

		@Override
		String getSource() {
			try {
				return module.getSource();
			} catch (ModelException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		String getName() {
			return module.getElementName();
		}

		@Override
		IModelElement[] codeSelect(int offset, int length)
				throws ModelException {
			return module.codeSelect(offset, length);
		}

		@Override
		void codeComplete(int offset, CompletionRequestor requestor)
				throws ModelException {
			module.codeComplete(offset, requestor);
		}
	}

	private static class Source extends Module<IModuleSource> {
		public Source(IModuleSource source) {
			super(source);
		}

		@Override
		String getSource() {
			return module.getSourceContents();
		}

		@Override
		String getName() {
			return module.getFileName();
		}

		@Override
		IModelElement[] codeSelect(int offset, int length) {
			throw new UnsupportedOperationException();
		}

		@Override
		void codeComplete(int offset, CompletionRequestor requestor)
				throws ModelException {
			throw new UnsupportedOperationException();
		}
	}

	private final Module<?> module;
	private Integer offset;
	private int length;

	private CodeAssistUtil(Module<?> module) {
		this.module = module;
	}

	public static CodeAssistUtil on(IFile file) {
		return on(DLTKCore.createSourceModuleFrom(file));
	}

	public static CodeAssistUtil on(ISourceModule module) {
		return new CodeAssistUtil(new SourceModule(module));
	}

	public static CodeAssistUtil on(IModuleSource module) {
		return new CodeAssistUtil(new Source(module));
	}

	public CodeAssistUtil after(String marker) {
		return calculateOffset(marker, false, true);
	}

	public CodeAssistUtil afterLast(String marker) {
		return calculateOffset(marker, true, true);
	}

	public CodeAssistUtil before(String marker) {
		return calculateOffset(marker, false, false);
	}

	public CodeAssistUtil beforeLast(String marker) {
		return calculateOffset(marker, true, false);
	}

	private CodeAssistUtil calculateOffset(String marker, boolean last,
			boolean after) {
		final String text = module.getSource();
		final int offset = last ? text.lastIndexOf(marker) : text
				.indexOf(marker);
		Assert.assertTrue(
				NLS.bind("Pattern \"{0}\" not found in {1}", marker,
						module.getName()), offset != -1);
		this.offset = offset + (after ? marker.length() : 0);
		return this;
	}

	public CodeAssistUtil length(int length) {
		this.length = length;
		return this;
	}

	public int length() {
		return length;
	}

	public CodeAssistUtil offset(int offset) {
		this.offset = offset;
		return this;
	}

	public int offset() {
		return offset;
	}

	public IModelElement[] codeSelect() throws ModelException {
		return module.codeSelect(offset, length);
	}

	private IModuleSource getModuleSource() {
		return (IModuleSource) module.module;
	}

	public class CodeCompletionResult {

		private final List<CompletionProposal> proposals;

		public CodeCompletionResult(List<CompletionProposal> proposals) {
			this.proposals = proposals;
		}

		public int size() {
			return proposals.size();
		}

		public CompletionProposal get(int index) {
			return proposals.get(index);
		}

		private boolean compareProposalNames(String[] names) {
			if (names.length != proposals.size()) {
				return false;
			}
			final CompletionProposal[] sorted = proposals
					.toArray(new CompletionProposal[proposals.size()]);
			Arrays.sort(sorted, new Comparator<CompletionProposal>() {
				public int compare(CompletionProposal pr, CompletionProposal pr1) {
					return pr.getName().compareTo(pr1.getName());
				}

			});
			final String[] sortedNames = new String[names.length];
			System.arraycopy(names, 0, sortedNames, 0, names.length);
			Arrays.sort(sortedNames);
			for (int i = 0, size = proposals.size(); i < size; ++i) {
				if (!names[i].equals(proposals.get(i).getName())) {
					return false;
				}
			}
			return true;
		}

		private StringList exractProposalNames(boolean withKinds) {
			final StringList list = new StringList(proposals.size());
			for (int i = 0, size = proposals.size(); i < size; ++i) {
				final CompletionProposal proposal = proposals.get(i);
				String name = proposal.getName();
				if (withKinds
						&& proposal.getKind() == CompletionProposal.METHOD_REF) {
					name += "()";
				}
				list.add(name);
			}
			return list;
		}

		public void assertEquals(String... expectedProposalNames) {
			if (!compareProposalNames(expectedProposalNames)) {
				Assert.assertEquals(new StringList(expectedProposalNames)
						.sort().toString(), exractProposalNames(false).sort()
						.toString());
			}
		}
	}

	public CodeCompletionResult codeComplete(ICompletionEngine engine) {
		final List<CompletionProposal> proposals = new ArrayList<CompletionProposal>();
		engine.setRequestor(new TestCompletionRequestor(proposals));
		engine.complete(getModuleSource(), offset(), 0);
		return new CodeCompletionResult(proposals);
	}

	/**
	 * Performs code completion in this source module.
	 */
	public CodeCompletionResult codeComplete() throws ModelException {
		final List<CompletionProposal> proposals = new ArrayList<CompletionProposal>();
		module.codeComplete(offset(), new TestCompletionRequestor(proposals));
		return new CodeCompletionResult(proposals);
	}

	public Object[] codeSelectAll(ISelectionEngine engine) {
		final List<Object> elements = new ArrayList<Object>();
		engine.setRequestor(new ISelectionRequestor() {
			public void acceptModelElement(IModelElement element) {
				elements.add(element);
			}

			public void acceptForeignElement(Object element) {
				elements.add(element);
			}

			public void acceptElement(Object element, ISourceRange range) {
				elements.add(element);
			}
		});
		final IModelElement[] result = engine.select(getModuleSource(), offset,
				offset);
		if (result != null) {
			Collections.addAll(elements, result);
		}
		return elements.toArray();
	}

	public IModelElement[] codeSelect(ISelectionEngine engine) {
		final List<IModelElement> elements = new ArrayList<IModelElement>();
		engine.setRequestor(new ISelectionRequestor() {

			public void acceptModelElement(IModelElement element) {
				elements.add(element);
			}

			public void acceptForeignElement(Object element) {
				if (element instanceof IModelElement) {
					elements.add((IModelElement) element);
				}
			}

			public void acceptElement(Object element, ISourceRange range) {
				acceptForeignElement(element);
			}
		});
		final IModelElement[] result = engine.select(getModuleSource(), offset,
				offset);
		if (result != null) {
			Collections.addAll(elements, result);
		}
		return elements.toArray(new IModelElement[elements.size()]);
	}

}
