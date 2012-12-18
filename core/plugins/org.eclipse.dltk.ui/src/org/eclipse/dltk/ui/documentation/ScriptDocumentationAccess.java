/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.ui.documentation;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IMember;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.utils.AdaptUtils;
import org.eclipse.dltk.utils.NatureExtensionManager;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Helper needed to get access to script documentation.
 * 
 * <p>
 * This class is not intended to be subclassed or instantiated by clients.
 * </p>
 */
public class ScriptDocumentationAccess {
	private static final String DOCUMENTATION_PROVIDERS_EXTENSION_POINT = "org.eclipse.dltk.ui.scriptDocumentationProviders"; //$NON-NLS-1$

	private static final NatureExtensionManager<IScriptDocumentationProvider> providers = new NatureExtensionManager<IScriptDocumentationProvider>(
			DOCUMENTATION_PROVIDERS_EXTENSION_POINT,
			IScriptDocumentationProvider.class) {
		@Override
		protected void initializeDescriptors(java.util.List<Object> descriptors) {
			Collections.sort(descriptors, new Comparator<Object>() {
				int priority(IConfigurationElement element) {
					try {
						return Integer.parseInt(element
								.getAttribute("priority"));
					} catch (NumberFormatException e) {
						return 0;
					}
				}

				public int compare(Object o1, Object o2) {
					return priority((IConfigurationElement) o2)
							- priority((IConfigurationElement) o1);
				}
			});
		}

		@Override
		protected IScriptDocumentationProvider[] createEmptyResult() {
			return new IScriptDocumentationProvider[0];
		}
	};

	private ScriptDocumentationAccess() {
		// do not instantiate
	}

	private static IScriptDocumentationProvider[] getProviders(String nature) {
		return providers.getInstances(nature);
	}

	private static interface Operation {
		Reader getInfo(IScriptDocumentationProvider provider);
	}

	private static interface Operation2 {
		IDocumentationResponse getInfo(IScriptDocumentationProvider provider);
	}

	private static final int BUFF_SIZE = 2048;

	private static Reader merge(String nature, Operation operation) {
		StringBuilder buffer = new StringBuilder();
		char[] buff = null;
		for (IScriptDocumentationProvider p : getProviders(nature)) {
			Reader reader = operation.getInfo(p);
			if (reader != null) {
				if (buffer.length() != 0) {
					buffer.append("<hr/>"); //$NON-NLS-1$
				}
				if (buff == null) {
					buff = new char[BUFF_SIZE];
				}
				try {
					int len;
					while ((len = reader.read(buff, 0, BUFF_SIZE)) != -1) {
						buffer.append(buff, 0, len);
					}
				} catch (IOException e) {
					if (DLTKCore.DEBUG) {
						e.printStackTrace();
					}
				}
			}
		}
		if (buffer.length() > 0) {
			char[] cnt = new char[buffer.length()];
			buffer.getChars(0, buffer.length(), cnt, 0);
			return new CharArrayReader(cnt);
		}
		return null;
	}

	private static IDocumentationResponse merge(String nature,
			Operation2 operation) {
		for (IScriptDocumentationProvider p : getProviders(nature)) {
			final IDocumentationResponse response = operation.getInfo(p);
			if (response != null) {
				return response;
			}
		}
		return null;
	}

	/**
	 * Gets a reader for an IMember documentation. Content are found using
	 * documentation documentationProviders, contributed via extension point.
	 * The content does contain HTML code describing member. It may be for ex.
	 * header comment or a man page. (if <code>allowExternal</code> is
	 * <code>true</code>)
	 * 
	 * @param member
	 *            The member to get documentation for.
	 * @param allowInherited
	 *            For procedures and methods: if member doesn't have it's own
	 *            documentation, look into parent types methods.
	 * @param allowExternal
	 *            Allows external documentation like man-pages.
	 * @return Reader for a content, or <code>null</code> if no documentation is
	 *         found.
	 * @throws ModelException
	 *             is thrown when the elements documentation can not be accessed
	 * @since 3.0
	 */
	public static Reader getHTMLContentReader(String nature,
			final Object member, final boolean allowInherited,
			final boolean allowExternal) {
		return merge(nature, new Operation() {
			public Reader getInfo(IScriptDocumentationProvider provider) {
				if (provider instanceof IScriptDocumentationProviderExtension2) {
					final IScriptDocumentationProviderExtension2 ext = (IScriptDocumentationProviderExtension2) provider;
					final IDocumentationResponse response = ext
							.getDocumentationFor(member);
					return DocumentationUtils.getReader(response);
				} else if (member instanceof IMember) {
					return provider.getInfo((IMember) member, allowInherited,
							allowExternal);
				} else {
					return null;
				}
			}
		});
	}

	/**
	 * @since 3.0
	 */
	public static IDocumentationResponse getDocumentation(String nature,
			final Object member, final IAdaptable context) {
		return merge(nature, new Operation2() {
			public IDocumentationResponse getInfo(
					IScriptDocumentationProvider provider) {
				if (provider instanceof IScriptDocumentationProviderExtension2) {
					final IScriptDocumentationProviderExtension2 ext = (IScriptDocumentationProviderExtension2) provider;
					final IDocumentationResponse response = ext
							.getDocumentationFor(member);
					if (response != null && response.getTitle() == null) {
						final IScriptDocumentationTitleAdapter titleAdapter = AdaptUtils
								.getAdapter(context,
										IScriptDocumentationTitleAdapter.class);
						if (titleAdapter != null) {
							final String title = titleAdapter.getTitle(member);
							// TODO (alex) image
							if (title != null && title.length() != 0) {
								return new DocumentationResponseDelegate(
										response) {
									@Override
									public String getTitle() {
										return title;
									}

									private boolean imageEvaluated;
									private ImageDescriptor image;

									@Override
									public ImageDescriptor getImage() {
										final ImageDescriptor result = super
												.getImage();
										if (result != null) {
											return result;
										}
										if (!imageEvaluated) {
											image = titleAdapter
													.getImage(member);
											imageEvaluated = true;
										}
										return image;
									}
								};
							}
						}
					}
					return response;
				} else if (member instanceof IMember) {
					final IMember m = (IMember) member;
					return DocumentationUtils.wrap(member, context,
							provider.getInfo(m, true, true));
				} else {
					return null;
				}
			}
		});
	}

	/**
	 * Gets a reader for an keyword documentation. Content are found using ALL
	 * documentation documentationProviders, contributed via extension point.
	 * The content does contain HTML code describing member.
	 * 
	 * @param content
	 *            The keyword to find.
	 * @return Reader for a content, or <code>null</code> if no documentation is
	 *         found.
	 * @throws ModelException
	 *             is thrown when the elements documentation can not be accessed
	 */
	@Deprecated
	public static Reader getHTMLContentReader(String nature,
			final String content) throws ModelException {
		return merge(nature, new Operation() {
			public Reader getInfo(IScriptDocumentationProvider provider) {
				return provider.getInfo(content);
			}
		});
	}

	/**
	 * Returns the documentation for the specified keyword
	 * 
	 * @param nature
	 * @param context
	 * @param keyword
	 * @since 2.0
	 */
	public static Reader getKeywordDocumentation(String nature,
			final IModelElement context, final String keyword)
			throws ModelException {
		return merge(nature, new Operation() {
			public Reader getInfo(IScriptDocumentationProvider provider) {
				if (provider instanceof IScriptDocumentationProviderExtension) {
					final IScriptDocumentationProviderExtension ext = (IScriptDocumentationProviderExtension) provider;
					final IDocumentationResponse response = ext
							.describeKeyword(keyword, context);
					return DocumentationUtils.getReader(response);
				} else {
					return provider.getInfo(keyword);
				}
			}
		});
	}
}
