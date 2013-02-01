/*******************************************************************************
 * Copyright (c) 2011 NumberFour AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     NumberFour AG - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.internal.core.builder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.core.builder.IBuildState;

abstract class AbstractBuildState implements IBuildState {

	private final String projectName;

	public AbstractBuildState(String projectName) {
		this.projectName = projectName;
	}

	private final Set<IPath> structuralChanges = new HashSet<IPath>();

	public void recordStructuralChange(IPath path) {
		Assert.isLegal(projectName.equals(path.segment(0)));
		structuralChanges.add(path);
	}

	public void recordStructuralChanges(Collection<IPath> paths) {
		for (IPath path : paths) {
			recordStructuralChange(path);
		}
	}

	public Set<IPath> getStructuralChanges() {
		return Collections.unmodifiableSet(structuralChanges);
	}

	public void resetStructuralChanges() {
		structuralChanges.clear();
	}

	public final void recordDependency(IPath path, IPath dependency) {
		recordDependency(path, dependency, STRUCTURAL);
	}
}
