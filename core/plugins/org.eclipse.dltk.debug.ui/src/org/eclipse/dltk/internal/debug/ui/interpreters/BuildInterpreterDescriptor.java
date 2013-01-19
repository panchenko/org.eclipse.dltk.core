/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.internal.debug.ui.interpreters;

import org.eclipse.dltk.debug.ui.messages.ScriptLaunchMessages;
import org.eclipse.dltk.launching.IInterpreterInstall;
import org.eclipse.dltk.launching.ScriptRuntime;
import org.eclipse.osgi.util.NLS;

/**
 * Interpreter Descriptor used for the Interpreter container wizard page.
 */
public class BuildInterpreterDescriptor extends InterpreterDescriptor {

	private final IInterpreterComboBlockContext fContext;

	/**
	 * @since 2.0
	 */
	public BuildInterpreterDescriptor(IInterpreterComboBlockContext context) {
		this.fContext = context;
	}

	@Override
	public String getDescription() {
		String name = ScriptLaunchMessages.InterpreterTab_7;
		IInterpreterInstall interpreter = getInterpreter();
		if (interpreter != null) {
			name = interpreter.getName();
		}
		return NLS.bind(ScriptLaunchMessages.InterpreterTab_8, name);
	}

	/*
	 * @see InterpreterDescriptor#getInterpreter()
	 */
	@Override
	public IInterpreterInstall getInterpreter() {
		return ScriptRuntime.getDefaultInterpreterInstall(
				fContext.getNatureId(), fContext.getEnvironment());
	}
}
