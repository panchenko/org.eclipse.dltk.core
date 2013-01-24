package org.eclipse.dltk.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.dltk.compiler.CharOperation;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.ui.text.ScriptSourceViewerConfiguration;
import org.eclipse.dltk.ui.text.ScriptTextTools;
import org.eclipse.dltk.ui.text.templates.ITemplateAccess;
import org.eclipse.dltk.ui.viewsupport.ScriptUILabelProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.ITextEditor;

public abstract class AbstractDLTKUILanguageToolkit implements
		IDLTKUILanguageToolkit {

	public ScriptUILabelProvider createScriptUILabelProvider() {
		// TODO Auto-generated method stub
		return null;
	}

	public ScriptSourceViewerConfiguration createSourceViewerConfiguration() {
		return null;
	}

	public String getDebugPreferencePage() {
		return null;
	}

	/**
	 * The combined preference store.
	 */
	private IPreferenceStore fCombinedPreferenceStore;

	/**
	 * Returns a combined preference store, this store is read-only.
	 * 
	 * @return the combined preference store
	 */
	public IPreferenceStore getCombinedPreferenceStore() {
		if (fCombinedPreferenceStore == null) {
			final List<IPreferenceStore> scopes = new ArrayList<IPreferenceStore>();
			scopes.add(getPreferenceStore());
			final String qualifier = getCoreToolkit().getPreferenceQualifier();
			if (qualifier != null) {
				scopes.add(new EclipsePreferencesAdapter(
						InstanceScope.INSTANCE, qualifier));
			}
			scopes.add(DLTKUIPlugin.getDefault().getPreferenceStore());
			scopes.add(new EclipsePreferencesAdapter(InstanceScope.INSTANCE,
					DLTKCore.PLUGIN_ID));
			scopes.add(EditorsUI.getPreferenceStore());
			fCombinedPreferenceStore = new ChainedPreferenceStore(
					scopes.toArray(new IPreferenceStore[scopes.size()]));
		}
		return fCombinedPreferenceStore;
	}

	public boolean getProvideMembers(ISourceModule element) {
		return true;
	}

	public ScriptElementLabels getScriptElementLabels() {
		return new ScriptElementLabels();
	}

	/**
	 * @deprecated
	 */
	protected final Object getUIPLugin() {
		return null;
	}

	public String getEditorId(Object inputElement) {
		IDLTKLanguageToolkit toolkit = this.getCoreToolkit();
		String contentTypeID = toolkit.getLanguageContentType();
		if (contentTypeID == null) {
			return null;
		}
		IEditorRegistry editorRegistry = PlatformUI.getWorkbench()
				.getEditorRegistry();
		IContentTypeManager contentTypeManager = Platform
				.getContentTypeManager();
		IContentType contentType = contentTypeManager
				.getContentType(contentTypeID);
		if (contentType == null) {
			return null;
		}

		String fileName = null;
		if (inputElement instanceof ISourceModule) {
			fileName = ((ISourceModule) inputElement).getPath().toString();
		} else if (inputElement instanceof IResource) {
			fileName = ((IResource) inputElement).getFullPath().toString();
		}

		IEditorDescriptor editor = editorRegistry.getDefaultEditor(fileName,
				contentType);
		if (editor != null) {
			return editor.getId();
		}
		return null;
	}

	public String getInterpreterContainerId() {
		return null;
	}

	public String getInterpreterPreferencePage() {
		return null;
	}

	public String getPartitioningId() {
		return "__default_dltk_partitioning"; //$NON-NLS-1$
	}

	public ScriptTextTools getTextTools() {
		return new ScriptTextTools(getPartitioningId(),
				CharOperation.NO_STRINGS, true) {
			@Override
			public ScriptSourceViewerConfiguration createSourceViewerConfiguraton(
					IPreferenceStore preferenceStore, ITextEditor editor,
					String partitioning) {
				return null;
			}
		};
	}

	public String[] getEditorPreferencePages() {
		return null;
	}

	public String getEditorTemplatesPreferencePageId() {
		final String[] pages = getEditorPreferencePages();
		if (pages != null && pages.length != 0) {
			String selected = null;
			for (String page : pages) {
				if (page.toLowerCase().contains("templates")) {
					if (selected != null) {
						return null;
					}
					selected = page;
				}
			}
			return selected;
		}
		return null;
	}

	public ITemplateAccess getEditorTemplates() {
		return null;
	}

	/**
	 * @since 2.0
	 */
	public boolean getBoolean(String name) {
		return getPreferenceStore().getBoolean(name);
	}

	/**
	 * @since 2.0
	 */
	public int getInt(String name) {
		return getPreferenceStore().getInt(name);
	}

	/**
	 * @since 2.0
	 */
	public String getString(String name) {
		return getPreferenceStore().getString(name);
	}
}
