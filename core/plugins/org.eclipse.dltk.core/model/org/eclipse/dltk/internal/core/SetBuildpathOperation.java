/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *

 *******************************************************************************/
package org.eclipse.dltk.internal.core;

import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.dltk.core.IBuildpathEntry;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IModelStatus;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.core.ModelManager.PerProjectInfo;

/**
 * This operation sets an <code>IScriptProject</code>'s buildpath.
 * 
 * @see IScriptProject
 */
public class SetBuildpathOperation extends ChangeBuildpathOperation {

	IBuildpathEntry[] newRawPath;

	ScriptProject project;

	/**
	 * When executed, this operation sets the buildpath of the given project.
	 */
	public SetBuildpathOperation(ScriptProject project,
			IBuildpathEntry[] newRawPath, boolean canChangeResource) {
		super(new IModelElement[] { project }, canChangeResource);
		this.newRawPath = newRawPath;
		this.project = project;
	}

	/**
	 * Sets the buildpath of the pre-specified project.
	 */
	@Override
	protected void executeOperation() throws ModelException {
		checkCanceled();
		try {
			// set raw classpath and null out resolved info
			PerProjectInfo perProjectInfo = this.project.getPerProjectInfo();
			BuildpathChange buildpathChange = perProjectInfo.setRawBuildpath(
					this.newRawPath, ModelStatus.VERIFIED_OK/* format is ok */);

			// if needed, generate delta, update project ref, create markers,
			buildpathChanged(buildpathChange /*
											 * , true refresh if external linked
											 * folder already exists
											 */);

			// write .classpath file
			if (this.canChangeResources && project.saveBuildpath(newRawPath))
				setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
		} finally {
			done();
		}
	}

	@Override
	protected ISchedulingRule getSchedulingRule() {
		if (this.canChangeResources) {
			IResourceRuleFactory ruleFactory = ResourcesPlugin.getWorkspace()
					.getRuleFactory();
			return new MultiRule(new ISchedulingRule[] {
					// use project modification rule as this is needed
					// to create the .classpath file if it doesn't exist
					// yet, or to update project references
					ruleFactory.modifyRule(this.project.getProject()),
					// and external project modification rule in case
					// the external folders are modified
					ruleFactory.modifyRule(ModelManager.getExternalManager()
							.getExternalFoldersProject()) });
		}
		return super.getSchedulingRule();
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer(20);
		buffer.append("SetBuildpathOperation\n"); //$NON-NLS-1$
		buffer.append(" - buildpath : "); //$NON-NLS-1$
		buffer.append("{"); //$NON-NLS-1$
		for (int i = 0; i < this.newRawPath.length; i++) {
			if (i > 0) {
				buffer.append(","); //$NON-NLS-1$
			}
			IBuildpathEntry element = this.newRawPath[i];
			buffer.append(" ").append(element.toString()); //$NON-NLS-1$
		}
		return buffer.toString();
	}

	@Override
	public IModelStatus verify() {

		IModelStatus status = super.verify();
		if (!status.isOK()) {
			return status;
		}
		this.project.flushBuildpathProblemMarkers(false, false);
		return BuildpathEntry.validateBuildpath(this.project, this.newRawPath);
	}
}
