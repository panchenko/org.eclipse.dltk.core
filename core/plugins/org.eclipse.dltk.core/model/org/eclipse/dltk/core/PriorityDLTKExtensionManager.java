/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

/**
 * Extension point must contain nature entry and priority entry. Other entries
 * aren't required.
 * 
 * @author Haiodo
 */
public class PriorityDLTKExtensionManager {
	public static final String PRIORITY_ATTR = "priority"; //$NON-NLS-1$

	/**
	 * guarded by this
	 */
	private Map<String, ElementInfo> extensions;

	private final String extensionPoint;
	private final String identifier;
	/**
	 * The preferred Id is not null, guarded by this.
	 */
	private Map<String, Integer> prefferedLevels;
	/**
	 * guarded by itself
	 */
	private final Map<String, ElementInfo> prefferedExtensionCache = new HashMap<String, ElementInfo>();

	public static class ElementInfo {
		int level;
		public final IConfigurationElement config;
		public Object object;
		public ElementInfo oldInfo;

		protected ElementInfo(IConfigurationElement config) {
			this.config = config;
		}

		public IConfigurationElement getConfig() {
			return config;
		}
	}

	public PriorityDLTKExtensionManager(String extensionPoint, String identifier) {
		Assert.isNotNull(extensionPoint);
		Assert.isNotNull(identifier);
		this.extensionPoint = extensionPoint;
		this.identifier = identifier;
	}

	protected synchronized Map<String, ElementInfo> initialize() {
		if (extensions != null) {
			return extensions;
		}

		extensions = new HashMap<String, ElementInfo>(5);
		IConfigurationElement[] cfg = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(extensionPoint);

		for (int i = 0; i < cfg.length; i++) {
			final IConfigurationElement element = cfg[i];
			if (!isValidConfigurationElement(element))
				continue;
			String nature = element.getAttribute(identifier);
			ElementInfo oldInfo = extensions.get(nature);
			if (oldInfo != null) {
				int lev = getLevel(element);
				if (lev <= oldInfo.level) {
					ElementInfo e = oldInfo;
					ElementInfo nInfo = createNewInfo(element, null);
					while (e != null) {
						if (e.oldInfo == null) {
							e.oldInfo = nInfo;
							break;
						} else {
							if (nInfo.level < e.oldInfo.level) {
								e = e.oldInfo;
							} else {
								nInfo.oldInfo = e.oldInfo;
								e.oldInfo = nInfo;
								break;
							}
						}
					}
					continue;
				}
			}
			ElementInfo info = createNewInfo(element, oldInfo);
			extensions.put(nature, info);
		}
		return extensions;
	}

	/**
	 * @param element
	 * @return
	 */
	protected boolean isValidConfigurationElement(IConfigurationElement element) {
		return true;
	}

	private ElementInfo createNewInfo(IConfigurationElement cfg,
			ElementInfo oldInfo) {
		ElementInfo info = createInfo(cfg);
		info.level = getLevel(info.config);
		info.oldInfo = oldInfo;
		return info;
	}

	protected ElementInfo internalGetElementInfo(String id) {
		ElementInfo info;
		synchronized (prefferedExtensionCache) {
			info = prefferedExtensionCache.get(id);
		}
		if (info != null) {
			return info;
		}
		info = initialize().get(id);
		if (info != null) {
			final Integer level;
			synchronized (this) {
				level = prefferedLevels != null ? prefferedLevels.get(id)
						: null;
			}
			if (level != null) {
				// Search for preferred id.
				final int prefferedLevel = level.intValue();
				for (ElementInfo o = info;;) {
					if (o.level == prefferedLevel) {
						info = o;
						break;
					}
					o = o.oldInfo;
					if (o == null) {
						break;
					}
				}
			}
			synchronized (prefferedExtensionCache) {
				prefferedExtensionCache.put(id, info);
			}
		}
		return info;
	}

	protected ElementInfo getElementInfo(String id) {
		return internalGetElementInfo(id);
	}

	protected int getLevel(IConfigurationElement config) {
		String priority = config.getAttribute(PRIORITY_ATTR);
		if (priority == null) {
			return 0;
		}
		try {
			int parseInt = Integer.parseInt(priority);
			return parseInt;
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	public ElementInfo[] getElementInfos() {
		final Map<String, ElementInfo> exts = initialize();
		final String[] keys = exts.keySet().toArray(new String[exts.size()]);
		final ElementInfo[] values = new ElementInfo[keys.length];
		for (int i = 0; i < keys.length; i++) {
			values[i] = getElementInfo(keys[i]);
		}
		return values;
	}

	/**
	 * Values config, nature, level are setted in current class.
	 * 
	 * @param config
	 * @return
	 */
	protected ElementInfo createInfo(IConfigurationElement config) {
		return new ElementInfo(config);
	}

	public String findScriptNature(IProject project) {
		try {
			if (!project.isAccessible()) {
				return null;
			}
			String[] natureIds = project.getDescription().getNatureIds();
			for (int i = 0; i < natureIds.length; i++) {
				String natureId = natureIds[i];

				if (getElementInfo(natureId) != null) {
					return natureId;
				}
			}
		} catch (CoreException e) {
			return null;
		}

		return null;
	}

	public synchronized void setPreffetedLevel(String id, int level) {
		synchronized (prefferedExtensionCache) {
			prefferedExtensionCache.clear();
		}
		if (level != -1) {
			if (prefferedLevels == null) {
				prefferedLevels = new HashMap<String, Integer>();
			}
			prefferedLevels.put(id, Integer.valueOf(level));
		} else {
			if (prefferedLevels != null) {
				prefferedLevels.remove(id);
			}
		}
	}
}
