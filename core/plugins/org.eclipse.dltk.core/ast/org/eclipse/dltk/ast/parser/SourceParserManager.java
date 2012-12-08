/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.dltk.ast.parser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.dltk.annotations.Nullable;
import org.eclipse.dltk.core.DLTKContributedExtension;
import org.eclipse.dltk.core.DLTKContributionExtensionManager;
import org.eclipse.dltk.core.DLTKCore;

/**
 * Manager responsible for all contributed <code>ISourceParser</code>
 * extension implementations.
 */
public class SourceParserManager extends DLTKContributionExtensionManager {

	static final String SOURCE_PARSER_EXT_POINT = DLTKCore.PLUGIN_ID
			+ ".sourceParsers"; //$NON-NLS-1$

	static final String PARSER_CONTRIBUTION_TAG = "parserContribution"; //$NON-NLS-1$

	static final String PARSER_TAG = "parser"; //$NON-NLS-1$

	static final String PARSER_CONFIGURATOR_TAG = "parserConfigurator"; //$NON-NLS-1$

	static final String PARSER_CONFIGURATOR_CLASS = "class"; //$NON-NLS-1$

	private static SourceParserManager instance;

	public static synchronized SourceParserManager getInstance() {
		if (instance == null) {
			instance = new SourceParserManager();
		}
		return instance;
	}

	public ISourceParser getSourceParserById(String id) {
		return ((SourceParserContribution) getContributionById(id))
				.createSourceParser(null);
	}

	@Override
	protected boolean isNatureContribution(IConfigurationElement main) {
		return PARSER_CONTRIBUTION_TAG.equals(main.getName()); //$NON-NLS-1$
	}
	
	/*
	 * @see org.eclipse.dltk.core.DLTKContributionExtensionManager#getContributionElementName()
	 */
	protected String getContributionElementName() {
		return PARSER_TAG;
	}

	/*
	 * @see org.eclipse.dltk.core.DLTKContributionExtensionManager#getExtensionPoint()
	 */
	protected String getExtensionPoint() {
		return SOURCE_PARSER_EXT_POINT;
	}

	/*
	 * @see org.eclipse.dltk.core.DLTKContributionExtensionManager#isValidContribution(java.lang.Object)
	 */
	protected boolean isValidContribution(Object object) {
		return (object instanceof ISourceParserFactory);
	}
	
	/*
	 * @see org.eclipse.dltk.core.DLTKContributionExtensionManager#configureContribution(java.lang.Object, org.eclipse.core.runtime.IConfigurationElement)
	 */
	protected Object configureContribution(Object object,
			IConfigurationElement config) {
		/*
		 * using the following delegate class allows for integration with the
		 * generic managed contribution preference page.
		 * 
		 * not all source parsers are thread safe, so the factory allows us
		 * to create a new instance each time one is requested
		 */		
		return new SourceParserContribution((ISourceParserFactory) object, config);
	}

	public ISourceParser getSourceParser(IProject project, String natureId) {
		SourceParserContribution contribution = (SourceParserContribution) getSelectedContribution(
				project, natureId);
		if (contribution != null) {
			return contribution.createSourceParser(project);
		}
		return null;
	}
	
	static class SourceParserContribution extends DLTKContributedExtension {		

		private final ISourceParserFactory factory;
		private final IConfigurationElement config;
		@Nullable
		final ISourceParserConfigurator[] configurators;
		
		SourceParserContribution(ISourceParserFactory factory, IConfigurationElement config) {
			this.factory = factory;
			this.config = config;
			
			/*
			 * this is a cheat - this class contains all the attributes of the 
			 * configured extension, so leverage the code DLTKContributedExtension
			 * already provides
			 */
			setInitializationData(config, null, null);
			this.configurators = getId() != null ? createConfigurators(getId())
					: null;
		}

		ISourceParser createSourceParser(@Nullable IProject project) {
			final ISourceParser parser = factory.createSourceParser();
			/*
			 * another cheat - not all source parsers are thread safe, so
			 * we need to create a new instance each time one is requested (hence
			 * the factory). 
			 *
			 * the parser instance should be initialized with all it's attribute
			 * data
			 */
			if (parser instanceof IExecutableExtension) {
				try {
					((IExecutableExtension) parser).setInitializationData(
							config, null, null);
				} catch (CoreException e) {
					DLTKCore.error(e);
				}
			}
			if (configurators != null) {
				for (ISourceParserConfigurator configurator : configurators) {
					configurator.configure(parser, project);
				}
			}
			
			return parser;
		}

		private static ISourceParserConfigurator[] createConfigurators(
				String parserId) {
			List<ISourceParserConfigurator> result = null;
			for (IConfigurationElement element : Platform
					.getExtensionRegistry().getConfigurationElementsFor(
							SOURCE_PARSER_EXT_POINT)) {
				if (PARSER_CONFIGURATOR_TAG.equals(element.getName())
						&& parserId.equals(element.getAttribute("id"))) { //$NON-NLS-1$
					try {
						final Object configurator = element
								.createExecutableExtension(PARSER_CONFIGURATOR_CLASS);
						if (configurator instanceof ISourceParserConfigurator) {
							if (result == null) {
								result = new ArrayList<ISourceParserConfigurator>();
							}
							result.add((ISourceParserConfigurator) configurator);
						}
					} catch (CoreException e) {
						DLTKCore.error(e);
					}
				}
			}
			return result != null ? result
					.toArray(new ISourceParserConfigurator[result.size()])
					: null;
		}
	}
}
