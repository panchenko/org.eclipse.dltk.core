/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.ast;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dltk.utils.CorePrinter;

public class ASTListNode extends ASTNode {

	private final List<ASTNode> nodes;

	public ASTListNode(int start, int end, List<ASTNode> nodes) {
		super(start, end);
		this.nodes = nodes;
	}

	public ASTListNode(int start, int end) {
		super(start, end);
		this.nodes = new ArrayList<ASTNode>();
	}

	public ASTListNode() {
		super(0, -1);
		this.nodes = new ArrayList<ASTNode>();
	}

	public void addNode(ASTNode s) {
		if (s != null) {
			nodes.add(s);
		}
	}

	public List<ASTNode> getChilds() {
		return nodes;
	}

	public void setChilds(List<ASTNode> l) {
		this.nodes.clear();
		this.nodes.addAll(l);
	}

	public int getKind() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void traverse(ASTVisitor visitor) throws Exception {
		if (visitor.visit(this)) {
			if (nodes != null) {
				for (ASTNode s : nodes) {
					s.traverse(visitor);
				}
			}
			visitor.endvisit(this);
		}
	}

	public void printNode(CorePrinter output) {
		if (this.nodes != null) {
			output.print('[');
			for (ASTNode s : nodes) {
				s.printNode(output);
			}
			output.print(']');
		}
	}

}
