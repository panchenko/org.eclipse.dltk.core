/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.internal.ui.text.hover;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.dltk.annotations.Internal;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.ui.text.HTMLPrinter;
import org.eclipse.dltk.ui.DLTKPluginImages;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.dltk.ui.PreferenceConstants;
import org.eclipse.dltk.ui.ScriptElementImageProvider;
import org.eclipse.dltk.ui.ScriptElementLabels;
import org.eclipse.dltk.ui.documentation.IDocumentationResponse;
import org.eclipse.dltk.ui.documentation.IScriptDocumentationTitleAdapter;
import org.eclipse.dltk.ui.documentation.ScriptDocumentationAccess;
import org.eclipse.dltk.ui.documentation.TextDocumentationResponse;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.internal.text.html.BrowserInformationControlInput;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension4;
import org.eclipse.jface.text.IInformationControlExtension5;
import org.eclipse.jface.text.IInputChangedListener;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.information.IInformationProviderExtension2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;

/**
 * Provides documentation as hover info for Script elements and keywords.
 */
@SuppressWarnings("restriction")
public class DocumentationHover extends AbstractScriptEditorTextHover implements
		IInformationProviderExtension2, ITextHoverExtension {

	private static final long LABEL_FLAGS = ScriptElementLabels.ALL_FULLY_QUALIFIED
			| ScriptElementLabels.M_APP_RETURNTYPE
			| ScriptElementLabels.F_APP_TYPE_SIGNATURE
			| ScriptElementLabels.M_PARAMETER_TYPES
			| ScriptElementLabels.M_PARAMETER_NAMES
			| ScriptElementLabels.M_EXCEPTIONS
			| ScriptElementLabels.F_PRE_TYPE_SIGNATURE
			| ScriptElementLabels.M_PRE_TYPE_PARAMETERS
			| ScriptElementLabels.T_TYPE_PARAMETERS
			| ScriptElementLabels.USE_RESOLVED;
	private static final long LOCAL_VARIABLE_FLAGS = LABEL_FLAGS
			& ~ScriptElementLabels.F_FULLY_QUALIFIED
			| ScriptElementLabels.F_POST_QUALIFIED;

	/**
	 * The hover control creator.
	 */
	private IInformationControlCreator fHoverControlCreator;

	/**
	 * The presentation control creator.
	 */
	private IInformationControlCreator fPresenterControlCreator;

	/**
	 * Action to go back to the previous input in the hover control.
	 * 
	 * @since 4.0
	 */
	private static final class BackAction extends Action {
		private final BrowserInformationControl fInfoControl;

		public BackAction(BrowserInformationControl infoControl) {
			fInfoControl = infoControl;
			setText(ScriptHoverMessages.ScriptdocHover_back);
			ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
			setImageDescriptor(images
					.getImageDescriptor(ISharedImages.IMG_TOOL_BACK));
			setDisabledImageDescriptor(images
					.getImageDescriptor(ISharedImages.IMG_TOOL_BACK_DISABLED));

			update();
		}

		@Override
		public void run() {
			BrowserInformationControlInput previous = (BrowserInformationControlInput) fInfoControl
					.getInput().getPrevious();
			if (previous != null) {
				fInfoControl.setInput(previous);
			}
		}

		public void update() {
			BrowserInformationControlInput current = fInfoControl.getInput();

			if (current != null && current.getPrevious() != null) {
				// BrowserInput previous = current.getPrevious();
				// setToolTipText(Messages.format(
				// JavaHoverMessages.JavadocHover_back_toElement_toolTip,
				// BasicElementLabels.getJavaElementName(previous
				// .getInputName())));
				setEnabled(true);
			} else {
				setEnabled(false);
			}
			setToolTipText(ScriptHoverMessages.ScriptdocHover_back);
		}
	}

	/**
	 * Action to go forward to the next input in the hover control.
	 * 
	 * @since 4.0
	 */
	private static final class ForwardAction extends Action {
		private final BrowserInformationControl fInfoControl;

		public ForwardAction(BrowserInformationControl infoControl) {
			fInfoControl = infoControl;
			setText(ScriptHoverMessages.ScriptdocHover_forward);
			ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
			setImageDescriptor(images
					.getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
			setDisabledImageDescriptor(images
					.getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD_DISABLED));

			update();
		}

		@Override
		public void run() {
			BrowserInformationControlInput next = (BrowserInformationControlInput) fInfoControl
					.getInput().getNext();
			if (next != null) {
				fInfoControl.setInput(next);
			}
		}

		public void update() {
			BrowserInformationControlInput current = fInfoControl.getInput();

			if (current != null && current.getNext() != null) {
				// setToolTipText(Messages
				// .format(JavaHoverMessages.JavadocHover_forward_toElement_toolTip,
				// BasicElementLabels.getJavaElementName(current
				// .getNext().getInputName())));
				setEnabled(true);
			} else {
				setEnabled(false);
			}
			setToolTipText(ScriptHoverMessages.ScriptdocHover_forward);
		}
	}

	/**
	 * Action that opens the current hover input element.
	 * 
	 * @since 4.0
	 */
	private static final class OpenDeclarationAction extends Action {
		private final BrowserInformationControl fInfoControl;

		public OpenDeclarationAction(BrowserInformationControl infoControl) {
			fInfoControl = infoControl;
			setText(ScriptHoverMessages.ScriptdocHover_openDeclaration);
			DLTKPluginImages.setLocalImageDescriptors(this, "goto_input.gif"); //$NON-NLS-1$ //TODO: better images
		}

		/*
		 * @see org.eclipse.jface.action.Action#run()
		 */
		@Override
		public void run() {
			DocumentationBrowserInformationControlInput infoInput = (DocumentationBrowserInformationControlInput) fInfoControl
					.getInput(); // TODO: check cast
			fInfoControl.notifyDelayedInputChange(null);
			fInfoControl.dispose(); // FIXME: should have protocol to hide,
									// rather than dispose

			try {
				if (infoInput.getElement() instanceof IModelElement) {
					// FIXME: add hover location to editor navigation history?
					DLTKUIPlugin.openInEditor((IModelElement) infoInput
							.getElement());
				}
				// TODO (alex) try via IOpenDelegate
			} catch (PartInitException e) {
				DLTKUIPlugin.log(e);
			} catch (ModelException e) {
				DLTKUIPlugin.log(e);
			}
		}
	}

	/**
	 * Presenter control creator.
	 * 
	 * @since 4.0
	 */
	public static final class PresenterControlCreator extends
			AbstractReusableInformationControlCreator {

		private IWorkbenchSite fSite;

		/**
		 * Creates a new PresenterControlCreator.
		 * 
		 * @param site
		 *            the site or <code>null</code> if none
		 */
		public PresenterControlCreator(IWorkbenchSite site) {
			fSite = site;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.java.hover.
		 * AbstractReusableInformationControlCreator
		 * #doCreateInformationControl(org.eclipse.swt.widgets.Shell)
		 */
		@Override
		public IInformationControl doCreateInformationControl(Shell parent) {
			if (BrowserInformationControl.isAvailable(parent)) {
				ToolBarManager tbm = new ToolBarManager(SWT.FLAT);
				String font = PreferenceConstants.APPEARANCE_DOCUMENTATION_FONT;
				BrowserInformationControl iControl = new BrowserInformationControl(
						parent, font, tbm);

				final BackAction backAction = new BackAction(iControl);
				backAction.setEnabled(false);
				tbm.add(backAction);
				final ForwardAction forwardAction = new ForwardAction(iControl);
				tbm.add(forwardAction);
				forwardAction.setEnabled(false);

				// final ShowInJavadocViewAction showInJavadocViewAction = new
				// ShowInJavadocViewAction(
				// iControl);
				// tbm.add(showInJavadocViewAction);
				final OpenDeclarationAction openDeclarationAction = new OpenDeclarationAction(
						iControl);
				tbm.add(openDeclarationAction);

				// final SimpleSelectionProvider selectionProvider = new
				// SimpleSelectionProvider();
				// if (fSite != null) {
				// OpenAttachedJavadocAction openAttachedJavadocAction = new
				// OpenAttachedJavadocAction(
				// fSite);
				// openAttachedJavadocAction
				// .setSpecialSelectionProvider(selectionProvider);
				// openAttachedJavadocAction
				// .setImageDescriptor(DLTKPluginImages.DESC_ELCL_OPEN_BROWSER);
				// openAttachedJavadocAction
				// .setDisabledImageDescriptor(DLTKPluginImages.DESC_DLCL_OPEN_BROWSER);
				// selectionProvider
				// .addSelectionChangedListener(openAttachedJavadocAction);
				// selectionProvider.setSelection(new StructuredSelection());
				// tbm.add(openAttachedJavadocAction);
				// }

				IInputChangedListener inputChangeListener = new IInputChangedListener() {
					public void inputChanged(Object newInput) {
						backAction.update();
						forwardAction.update();
						if (newInput == null) {
							// selectionProvider
							// .setSelection(new StructuredSelection());
						} else if (newInput instanceof BrowserInformationControlInput) {
							BrowserInformationControlInput input = (BrowserInformationControlInput) newInput;
							Object inputElement = input.getInputElement();
							// selectionProvider
							// .setSelection(new StructuredSelection(
							// inputElement));
							boolean isJavaElementInput = inputElement instanceof IModelElement;
							// showInJavadocViewAction
							// .setEnabled(isJavaElementInput);
							openDeclarationAction
									.setEnabled(isJavaElementInput);
						}
					}
				};
				iControl.addInputChangeListener(inputChangeListener);

				tbm.update(true);

				// TODO (alex) addLinkListener(iControl);
				return iControl;

			} else {
				return new DefaultInformationControl(parent, true);
			}
		}
	}

	public IInformationControlCreator getInformationPresenterControlCreator() {
		if (fPresenterControlCreator == null) {
			fPresenterControlCreator = new PresenterControlCreator(getSite());
		}
		return fPresenterControlCreator;
	}

	private IWorkbenchSite getSite() {
		IEditorPart editor = getEditor();
		if (editor == null) {
			IWorkbenchPage page = DLTKUIPlugin.getActivePage();
			if (page != null)
				editor = page.getActiveEditor();
		}
		if (editor != null)
			return editor.getSite();

		return null;
	}

	/**
	 * Hover control creator.
	 * 
	 * @since 4.0
	 */
	public static final class HoverControlCreator extends
			AbstractReusableInformationControlCreator {
		/**
		 * The information presenter control creator.
		 */
		private final IInformationControlCreator fInformationPresenterControlCreator;

		/**
		 * <code>true</code> to use the additional info affordance,
		 * <code>false</code> to use the hover affordance.
		 */
		private final boolean fAdditionalInfoAffordance;

		/**
		 * @param informationPresenterControlCreator
		 *            control creator for enriched hover
		 */
		public HoverControlCreator(
				IInformationControlCreator informationPresenterControlCreator) {
			this(informationPresenterControlCreator, false);
		}

		/**
		 * @param informationPresenterControlCreator
		 *            control creator for enriched hover
		 * @param additionalInfoAffordance
		 *            <code>true</code> to use the additional info affordance,
		 *            <code>false</code> to use the hover affordance
		 */
		public HoverControlCreator(
				IInformationControlCreator informationPresenterControlCreator,
				boolean additionalInfoAffordance) {
			fInformationPresenterControlCreator = informationPresenterControlCreator;
			fAdditionalInfoAffordance = additionalInfoAffordance;
		}

		/**
		 * @see AbstractReusableInformationControlCreator#doCreateInformationControl(Shell)
		 */
		@Override
		public IInformationControl doCreateInformationControl(Shell parent) {
			String tooltipAffordanceString = fAdditionalInfoAffordance ? DLTKUIPlugin
					.getAdditionalInfoAffordanceString() : EditorsUI
					.getTooltipAffordanceString();
			if (BrowserInformationControl.isAvailable(parent)) {
				String font = PreferenceConstants.APPEARANCE_DOCUMENTATION_FONT;
				BrowserInformationControl iControl = new BrowserInformationControl(
						parent, font, tooltipAffordanceString) {
					/**
					 * @see IInformationControlExtension5#getInformationPresenterControlCreator()
					 */
					@Override
					public IInformationControlCreator getInformationPresenterControlCreator() {
						return fInformationPresenterControlCreator;
					}
				};
				// TODO (alex) addLinkListener(iControl);
				return iControl;
			} else {
				return new DefaultInformationControl(parent,
						tooltipAffordanceString);
			}
		}

		/**
		 * @see AbstractReusableInformationControlCreator#canReuse(IInformationControl)
		 */
		@Override
		public boolean canReuse(IInformationControl control) {
			if (!super.canReuse(control))
				return false;

			if (control instanceof IInformationControlExtension4) {
				String tooltipAffordanceString = fAdditionalInfoAffordance ? DLTKUIPlugin
						.getAdditionalInfoAffordanceString() : EditorsUI
						.getTooltipAffordanceString();
				((IInformationControlExtension4) control)
						.setStatusText(tooltipAffordanceString);
			}

			return true;
		}
	}

	@Override
	public IInformationControlCreator getHoverControlCreator() {
		if (fHoverControlCreator == null)
			fHoverControlCreator = new HoverControlCreator(
					getInformationPresenterControlCreator());
		return fHoverControlCreator;
	}

	@Override
	protected String getHoverInfo(String nature, Object[] result) {
		StringBuffer buffer = new StringBuffer();
		int nResults = result.length;
		if (nResults == 0)
			return null;

		boolean hasContents = false;
		if (nResults > 1) {
			HTMLPrinter.addSmallHeader(
					buffer,
					getInfoText(result[0], titleAdapter.getTitle(result[0]),
							titleAdapter.getImage(result[0])));
			HTMLPrinter.addParagraph(buffer, "<hr>"); //$NON-NLS-1$
			for (int i = 0; i < result.length; i++) {
				Object element = result[i];
				Reader reader = ScriptDocumentationAccess.getHTMLContentReader(
						nature, element, true, true);
				if (reader == null) {
					continue;
				}
				if (hasContents) {
					HTMLPrinter.addParagraph(buffer, "<hr>"); //$NON-NLS-1$
				}
				HTMLPrinter.addParagraph(buffer, reader);
				hasContents = true;
			}
		} else {
			Object element = result[0];
			IDocumentationResponse response = ScriptDocumentationAccess
					.getDocumentation(nature, element, titleAdapter);
			// Provide hint why there's no doc
			if (response == null) {
				response = new TextDocumentationResponse(
						element,
						titleAdapter.getTitle(element),
						titleAdapter.getImage(element),
						ScriptHoverMessages.ScriptdocHover_noAttachedInformation);
			}
			try {
				HTMLPrinter.addSmallHeader(
						buffer,
						getInfoText(element, response.getTitle(),
								response.getImage()));
				HTMLPrinter.addParagraph(buffer, response.getReader());
				hasContents = true;
			} catch (IOException e) {
				return null;
			}
			/*
			 * else if (curr.getElementType() == IModelElement.LOCAL_VARIABLE ||
			 * curr.getElementType() == IModelElement.TYPE_PARAMETER) {
			 * HTMLPrinter.addSmallHeader(buffer, getInfoText(curr));
			 * hasContents= true; }
			 */
		}
		if (!hasContents)
			return null;
		if (buffer.length() > 0) {
			HTMLPrinter.insertPageProlog(buffer, 0, getStyleSheet());
			HTMLPrinter.addPageEpilog(buffer);
			return buffer.toString();
		}
		return null;
	}

	private String getInfoText(Object element, String title,
			ImageDescriptor image) {
		String imageName = null;
		if (image != null) {
			final URL imageURL = DLTKUIPlugin.getDefault()
					.getImagesOnFSRegistry().getImageURL(image);
			if (imageURL != null) {
				imageName = imageURL.toExternalForm();
			}
		}
		StringBuffer buf = new StringBuffer();
		addImageAndLabel(buf, element, imageName, 16, 16,
				org.eclipse.jface.internal.text.html.HTMLPrinter
						.convertToHTMLContent(title), 20, 2);
		return buf.toString();
	}

	private static void addImageAndLabel(StringBuffer buf, Object element,
			String imageSrcPath, int imageWidth, int imageHeight, String label,
			int labelLeft, int labelTop) {
		buf.append("<div style='word-wrap: break-word; position: relative; "); //$NON-NLS-1$

		if (imageSrcPath != null) {
			buf.append("margin-left: ").append(labelLeft).append("px; "); //$NON-NLS-1$ //$NON-NLS-2$
			buf.append("padding-top: ").append(labelTop).append("px; "); //$NON-NLS-1$ //$NON-NLS-2$
		}

		buf.append("'>"); //$NON-NLS-1$
		if (imageSrcPath != null) {
			// if (element != null) {
			// try {
			// String uri = JavaElementLinks.createURI(
			// JavaElementLinks.OPEN_LINK_SCHEME, element);
			//					buf.append("<a href='").append(uri).append("'>"); //$NON-NLS-1$//$NON-NLS-2$
			// } catch (URISyntaxException e) {
			// element = null; // no link
			// }
			// }
			StringBuffer imageStyle = new StringBuffer(
					"border:none; position: absolute; "); //$NON-NLS-1$
			imageStyle.append("width: ").append(imageWidth).append("px; "); //$NON-NLS-1$ //$NON-NLS-2$
			imageStyle.append("height: ").append(imageHeight).append("px; "); //$NON-NLS-1$ //$NON-NLS-2$
			imageStyle.append("left: ").append(-labelLeft - 1).append("px; "); //$NON-NLS-1$ //$NON-NLS-2$

			// hack for broken transparent PNG support in IE 6, see
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=223900 :
			buf.append("<!--[if lte IE 6]><![if gte IE 5.5]>\n"); //$NON-NLS-1$
			String tooltip = element == null ? "" : "alt='" + ScriptHoverMessages.ScriptdocHover_openDeclaration + "' "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			buf.append("<span ").append(tooltip).append("style=\"").append(imageStyle). //$NON-NLS-1$ //$NON-NLS-2$
					append("filter:progid:DXImageTransform.Microsoft.AlphaImageLoader(src='").append(imageSrcPath).append("')\"></span>\n"); //$NON-NLS-1$ //$NON-NLS-2$
			buf.append("<![endif]><![endif]-->\n"); //$NON-NLS-1$

			buf.append("<!--[if !IE]>-->\n"); //$NON-NLS-1$
			buf.append("<img ").append(tooltip).append("style='").append(imageStyle).append("' src='").append(imageSrcPath).append("'/>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			buf.append("<!--<![endif]-->\n"); //$NON-NLS-1$
			buf.append("<!--[if gte IE 7]>\n"); //$NON-NLS-1$
			buf.append("<img ").append(tooltip).append("style='").append(imageStyle).append("' src='").append(imageSrcPath).append("'/>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			buf.append("<![endif]-->\n"); //$NON-NLS-1$
			// if (element != null) {
			//				buf.append("</a>"); //$NON-NLS-1$
			// }
		}

		buf.append(label);
		buf.append("</div>"); //$NON-NLS-1$
	}

	@Override
	protected String getHoverInfo(String nature, String content) {
		try {
			Reader reader = ScriptDocumentationAccess.getKeywordDocumentation(
					nature, getEditorInputModelElement(), content);
			if (reader != null) {
				StringBuffer buffer = new StringBuffer();
				HTMLPrinter.addParagraph(buffer, reader);
				if (buffer.length() > 0) {
					HTMLPrinter.insertPageProlog(buffer, 0, getStyleSheet());
					HTMLPrinter.addPageEpilog(buffer);
					return buffer.toString();
				}
			}
		} catch (ModelException ex) {
			// TODO: log
		}
		return null;
	}

	private static final ScriptDocumentationTitleAdapter titleAdapter = new ScriptDocumentationTitleAdapter();

	@Internal
	static class ScriptDocumentationTitleAdapter extends PlatformObject
			implements IScriptDocumentationTitleAdapter {

		private ScriptElementImageProvider fImageProvider;

		public String getTitle(Object element) {
			if (element instanceof IModelElement) {
				IModelElement member = (IModelElement) element;
				long flags = member.getElementType() == IModelElement.LOCAL_VARIABLE ? LOCAL_VARIABLE_FLAGS
						: LABEL_FLAGS;
				String label = ScriptElementLabels.getDefault()
						.getElementLabel(member, flags);
				return label;
			} else {
				return null;
			}
		}

		public ImageDescriptor getImage(Object element) {
			if (element instanceof IModelElement) {
				final IModelElement modelElement = (IModelElement) element;
				if (fImageProvider == null) {
					fImageProvider = new ScriptElementImageProvider();
				}
				return fImageProvider.getScriptImageDescriptor(modelElement,
						ScriptElementImageProvider.OVERLAY_ICONS
								| ScriptElementImageProvider.SMALL_ICONS);
			}
			return null;
		}
	}
}
