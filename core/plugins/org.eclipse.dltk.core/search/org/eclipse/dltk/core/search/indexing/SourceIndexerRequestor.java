/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.core.search.indexing;

import java.util.Stack;

import org.eclipse.dltk.compiler.IBinaryElementRequestor;
import org.eclipse.dltk.compiler.ISourceElementRequestor;
import org.eclipse.dltk.compiler.ISourceElementRequestorExtension;
import org.eclipse.dltk.compiler.SourceElementRequestorMode;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.ISearchFactory;
import org.eclipse.dltk.core.ISearchPatternProcessor;

/**
 * This class is used by the JavaParserIndexer. When parsing thescriptfile, the
 * requestor recognises thescriptelements (methods, fields, ...) and add them to
 * an index.
 */
public class SourceIndexerRequestor implements ISourceElementRequestor,
		IBinaryElementRequestor, IIndexConstants,
		ISourceElementRequestorExtension {
	protected AbstractIndexer indexer;
	// char[] packageName = CharOperation.NO_CHAR;
	protected String[] enclosingTypeNames = new String[5];
	protected int depth = 0;
	protected int methodDepth = 0;
	// protected String pkgName = Util.EMPTY_STRING;

	protected ISearchFactory searchFactory;
	protected ISearchPatternProcessor searchPatternProcessor;
	private Stack<String[]> namespaces = new Stack<String[]>();

	public SourceIndexerRequestor(AbstractIndexer indexer) {
		this.indexer = indexer;
	}

	public SourceIndexerRequestor() {
	}

	public void setIndexer(AbstractIndexer indexer) {
		this.indexer = indexer;
	}

	public void setSearchFactory(ISearchFactory searchFactory) {
		this.searchFactory = searchFactory;
		if (searchFactory != null) {
			searchPatternProcessor = searchFactory
					.createSearchPatternProcessor();
		} else {
			searchPatternProcessor = null;
		}
	}

	/**
	 * @see ISourceElementRequestor#acceptFieldReference(char[], int)
	 * @since 2.0
	 */
	public void acceptFieldReference(String fieldName, int sourcePosition) {
		this.indexer.addFieldReference(fieldName);
	}

	/**
	 * @see ISourceElementRequestor#acceptLineSeparatorPositions(int[])
	 */
	public void acceptLineSeparatorPositions(int[] positions) {
		// implements interface method
	}

	/**
	 * @see ISourceElementRequestor#acceptMethodReference(char[], int, int, int)
	 * @since 2.0
	 */
	public void acceptMethodReference(String methodName, int argCount,
			int sourcePosition, int sourceEndPosition) {
		this.indexer.addMethodReference(methodName, argCount);
	}

	// /**
	// * @see ISourceElementRequestor#acceptPackage(int, int, char[])
	// */
	// public void acceptPackage(int declarationStart, int declarationEnd,
	// char[] name) {
	// this.packageName = name;
	// }

	/**
	 * @see ISourceElementRequestor#acceptTypeReference(char[], int)
	 */
	public void acceptTypeReference(String simpleTypeName, int sourcePosition) {
		this.indexer.addTypeReference(simpleTypeName);
	}

	/**
	 * @see ISourceElementRequestor#acceptUnknownReference(char[], int)
	 */
	public void acceptUnknownReference(String name, int sourcePosition) {
		this.indexer.addNameReference(name);
	}

	/*
	 * Rebuild the proper qualification for the current source type:
	 * 
	 * java.lang.Object ---> null java.util.Hashtable$Entry --> [Hashtable]
	 * x.y.A$B$C --> [A, B]
	 */
	protected String[] enclosingTypeNames() {
		if (depth == 0)
			return null;
		String[] qualification = new String[this.depth];
		System.arraycopy(this.enclosingTypeNames, 0, qualification, 0,
				this.depth);

		return qualification;
	}

	protected String[] namespace() {
		return !namespaces.isEmpty() ? namespaces.peek() : null;
	}

	/**
	 * @since 2.0
	 */
	public void enterType(TypeInfo typeInfo) {
		// eliminate possible qualifications, given they need to be fully
		// resolved again
		if (typeInfo.superclasses != null) {
			// typeInfo.superclasses = typeInfo.superclasses;
			for (int i = 0, length = typeInfo.superclasses.length; i < length; i++) {
				typeInfo.superclasses[i] = getSimpleName(typeInfo.superclasses[i]);
			}
			// add implicit constructor reference to default constructor
			if (DLTKCore.DEBUG) {
				System.err.println("TODO: Add constructore references..."); //$NON-NLS-1$
			}
			// this.indexer.addConstructorReference(typeInfo.superclasss, 0);
		}
		String[] typeNames;
		if (this.methodDepth > 0) {
			typeNames = ONE_ZERO_CHAR_STRINGS;
		} else {
			typeNames = this.enclosingTypeNames();
		}
		this.indexer.addTypeDeclaration(typeInfo.modifiers, namespace(),
				typeInfo.name, typeNames, typeInfo.superclasses);
		this.pushTypeName(typeInfo.name);
	}

	/**
	 * @since 2.0
	 */
	public void enterConstructor(MethodInfo methodInfo) {
		this.indexer.addConstructorDeclaration(methodInfo.name,
				methodInfo.parameterNames, methodInfo.exceptionTypes);
		this.methodDepth++;
	}

	/**
	 * @see ISourceElementRequestor#enterField(FieldInfo)
	 * @since 2.0
	 */
	public void enterField(FieldInfo fieldInfo) {
		this.indexer.addFieldDeclaration(fieldInfo.name, fieldInfo.type);
		this.methodDepth++;
	}

	/**
	 * @see ISourceElementRequestor#enterMethod(MethodInfo)
	 * @since 2.0
	 */
	public void enterMethod(MethodInfo methodInfo) {
		this.indexer.addMethodDeclaration(methodInfo.modifiers, namespace(),
				this.enclosingTypeNames(), methodInfo.name,
				methodInfo.parameterNames, methodInfo.exceptionTypes);
		if (methodInfo.returnType != null) {
			this.indexer.addTypeReference(methodInfo.returnType);
		}
		if (methodInfo.parameterTypes != null) {
			for (String type : methodInfo.parameterTypes) {
				if (type != null) {
					this.indexer.addTypeReference(type);
				}
			}
		}
		this.methodDepth++;
	}

	/**
	 * @see ISourceElementRequestor#exitType(int)
	 */
	public void exitType(int declarationEnd) {
		popTypeName();
	}

	/*
	 * Returns the unqualified name without parameters from the given type name.
	 */
	private String getSimpleName(String typeName) {
		if (searchPatternProcessor != null) {
			return searchPatternProcessor.extractTypeChars(typeName);
		} else {
			return typeName;
		}
	}

	public void popTypeName() {
		if (depth > 0) {
			// System.out.println("POPNAME:" + new String(
			// enclosingTypeNames[depth-1]));
			enclosingTypeNames[--depth] = null;
		}
		// else if (JobManager.VERBOSE) {
		// // dump a trace so it can be tracked down
		// try {
		// enclosingTypeNames[-1] = null;
		// } catch (ArrayIndexOutOfBoundsException e) {
		// e.printStackTrace();
		// }
		// }
	}

	public void pushTypeName(String typeName) {
		if (depth == enclosingTypeNames.length)
			System.arraycopy(enclosingTypeNames, 0,
					enclosingTypeNames = new String[depth * 2], 0, depth);
		enclosingTypeNames[depth++] = typeName;
	}

	/**
	 * @since 2.0
	 */
	public void enterMethodRemoveSame(MethodInfo info) {
		if (DLTKCore.DEBUG) {
			System.out.println("TODO: Add replace method code."); //$NON-NLS-1$
		}
	}

	public void enterModule() {
	}

	public void exitField(int declarationEnd) {
		this.methodDepth--;
	}

	public void exitMethod(int declarationEnd) {
		this.methodDepth--;
	}

	public void exitModule(int declarationEnd) {
		indexer.ensureDocumentAdded();
	}

	/**
	 * @since 2.0
	 */
	public void acceptPackage(int declarationStart, int declarationEnd,
			String name) {
		// this.pkgName = name;
	}

	/**
	 * @since 2.0
	 */
	public boolean enterFieldCheckDuplicates(FieldInfo info) {
		this.indexer.addFieldDeclaration(info.name, info.type);
		this.methodDepth++;
		return true;
	}

	/**
	 * @since 2.0
	 */
	public boolean enterTypeAppend(TypeInfo info, String fullName,
			String delimiter) {
		enterType(info);
		return true;
	}

	public void enterModuleRoot() {
		// TODO Auto-generated method stub

	}

	public boolean enterTypeAppend(String fullName, String delimiter) {
		return false;
	}

	public void exitModuleRoot() {
	}

	/**
	 * @since 2.0
	 */
	public void acceptImport(ImportInfo importInfo) {
	}

	public void enterNamespace(String[] namespace) {
		namespaces.push(namespace);
	}

	public void exitNamespace() {
		namespaces.pop();
	}

	public SourceElementRequestorMode getMode() {
		return SourceElementRequestorMode.INDEX;
	}

}
