package org.eclipse.dltk.core.model.binary;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dltk.core.CompletionRequestor;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IField;
import org.eclipse.dltk.core.IMethod;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IParent;
import org.eclipse.dltk.core.IScriptFolder;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.IType;
import org.eclipse.dltk.core.ITypeHierarchy;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.WorkingCopyOwner;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.core.search.SearchEngine;
import org.eclipse.dltk.internal.core.CreateTypeHierarchyOperation;
import org.eclipse.dltk.internal.core.DefaultWorkingCopyOwner;
import org.eclipse.dltk.internal.core.ModelElement;
import org.eclipse.dltk.internal.core.ModelManager;
import org.eclipse.dltk.internal.core.hierarchy.TypeHierarchy;
import org.eclipse.dltk.internal.core.hierarchy.TypeHierarchyBuilders;
import org.eclipse.dltk.internal.core.util.Messages;
import org.eclipse.dltk.utils.CorePrinter;

/**
 * @since 2.0
 */
public class BinaryType extends BinaryMember implements IType, IParent {
	private static final IField[] NO_FIELDS = new IField[0];
	private static final IMethod[] NO_METHODS = new IMethod[0];
	private static final IType[] NO_TYPES = new IType[0];

	public BinaryType(ModelElement parent, String name) {
		super(parent, name);
	}

	public void codeComplete(char[] snippet, int insertion, int position,
			char[][] localVariableTypeNames, char[][] localVariableNames,
			int[] localVariableModifiers, boolean isStatic,
			CompletionRequestor requestor) throws ModelException {
	}

	public void codeComplete(char[] snippet, int insertion, int position,
			char[][] localVariableTypeNames, char[][] localVariableNames,
			int[] localVariableModifiers, boolean isStatic,
			CompletionRequestor requestor, WorkingCopyOwner owner)
			throws ModelException {
	}

	private IDLTKSearchScope createReferencingProjectsScope() {

		IScriptProject scriptProject = getScriptProject();
		IProject project = scriptProject.getProject();
		IProject[] referencingProjects = project.getReferencingProjects();

		List<IScriptProject> scriptProjects = new ArrayList<IScriptProject>(
				referencingProjects.length + 1);
		scriptProjects.add(scriptProject);

		for (int i = 0; i < referencingProjects.length; ++i) {
			IProject p = referencingProjects[i];
			if (p.isAccessible()) {
				scriptProjects.add(DLTKCore.create(p));
			}
		}
		return SearchEngine.createSearchScope(scriptProjects
				.toArray(new IModelElement[scriptProjects.size()]), false,
				DLTKLanguageManager.getLanguageToolkit(this));
	}

	public boolean equals(Object o) {
		if (!(o instanceof BinaryType)) {
			return false;
		}
		return super.equals(o);
	}

	/*
	 * @see IType
	 */
	public IMethod[] findMethods(IMethod method) {
		try {
			return findMethods(method, getMethods());
		} catch (ModelException e) {
			// if type doesn't exist, no matching method can exist
			return null;
		}
	}

	public int getElementType() {
		return TYPE;
	}

	public IField getField(String name) {
		return new BinaryField(this, name);
	}

	public IField[] getFields() throws ModelException {
		List<IModelElement> list = getChildrenOfType(FIELD);
		int size;
		if ((size = list.size()) == 0) {
			return NO_FIELDS;
		} else {
			IField[] array = new IField[size];
			list.toArray(array);
			return array;
		}
	}

	public String getFullyQualifiedName() {
		return getFullyQualifiedName("$"); //$NON-NLS-1$
	}

	public String getFullyQualifiedName(String enclosingTypeSeparator) {
		try {
			/*
			 * don't show parameters
			 */
			return getFullyQualifiedName(enclosingTypeSeparator, false);
		} catch (ModelException e) {
			// exception thrown only when showing parameters
			return null;
		}
	}

	public IMethod getMethod(String name) {
		return new BinaryMethod(this, name);
	}

	public IMethod[] getMethods() throws ModelException {
		List<IModelElement> list = getChildrenOfType(METHOD);
		int size;
		if ((size = list.size()) == 0) {
			return NO_METHODS;
		} else {
			IMethod[] array = new IMethod[size];
			list.toArray(array);
			return array;
		}
	}

	public IScriptFolder getScriptFolder() {
		return (IScriptFolder) getAncestor(IModelElement.SCRIPT_FOLDER);
	}

	public String[] getSuperClasses() throws ModelException {
		return ((BinaryTypeElementInfo) getElementInfo()).getSuperclassNames();
	}

	public IType getType(String name) {
		try {
			IType[] types = getTypes();
			for (IType type : types) {
				if (type.getElementName().equals(name)) {
					return type;
				}
			}
		} catch (ModelException e) {
			if (DLTKCore.DEBUG) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public String getTypeQualifiedName() {
		return this.getTypeQualifiedName("$");
	}

	public String getTypeQualifiedName(String enclosingTypeSeparator) {
		try {
			/*
			 * don't show parameters
			 */
			return getTypeQualifiedName(enclosingTypeSeparator, false);
		} catch (ModelException e) {
			// exception thrown only when showing parameters
			return null;
		}
	}

	public IType[] getTypes() throws ModelException {
		List<IModelElement> list = getChildrenOfType(TYPE);
		int size;
		if ((size = list.size()) == 0) {
			return NO_TYPES;
		} else {
			IType[] array = new IType[size];
			list.toArray(array);
			return array;
		}
	}

	/*
	 * Type hierarchies section
	 */

	/**
	 * @see IType
	 */
	public ITypeHierarchy loadTypeHierachy(InputStream input,
			IProgressMonitor monitor) throws ModelException {
		return loadTypeHierachy(input, DefaultWorkingCopyOwner.PRIMARY, monitor);
	}

	/**
	 * NOTE: This method is not part of the API has it is not clear clients
	 * would easily use it: they would need to first make sure all working
	 * copies for the given owner exist before calling it. This is especially
	 * har at startup time. In case clients want this API, here is how it should
	 * be specified:
	 * <p>
	 * Loads a previously saved ITypeHierarchy from an input stream. A type
	 * hierarchy can be stored using ITypeHierachy#store(OutputStream). A
	 * compilation unit of a loaded type has the given owner if such a working
	 * copy exists, otherwise the type's compilation unit is a primary
	 * compilation unit.
	 * 
	 * Only hierarchies originally created by the following methods can be
	 * loaded:
	 * <ul>
	 * <li>IType#newSupertypeHierarchy(IProgressMonitor)</li>
	 * <li>IType#newSupertypeHierarchy(WorkingCopyOwner, IProgressMonitor)</li>
	 * <li>IType#newTypeHierarchy(IJavaProject, IProgressMonitor)</li>
	 * <li>IType#newTypeHierarchy(IJavaProject, WorkingCopyOwner,
	 * IProgressMonitor)</li>
	 * <li>IType#newTypeHierarchy(IProgressMonitor)</li>
	 * <li>IType#newTypeHierarchy(WorkingCopyOwner, IProgressMonitor)</li> </u>
	 * 
	 * @param input
	 *            stream where hierarchy will be read
	 * @param monitor
	 *            the given progress monitor
	 * @return the stored hierarchy
	 * @exception JavaModelException
	 *                if the hierarchy could not be restored, reasons include: -
	 *                type is not the focus of the hierarchy or - unable to read
	 *                the input stream (wrong format, IOException during
	 *                reading, ...)
	 * @see ITypeHierarchy#store(java.io.OutputStream, IProgressMonitor)
	 * @since 3.0
	 */
	public ITypeHierarchy loadTypeHierachy(InputStream input,
			WorkingCopyOwner owner, IProgressMonitor monitor)
			throws ModelException {
		// TODO monitor should be passed to TypeHierarchy.load(...)
		return TypeHierarchy.load(this, input, owner);
	}

	/**
	 * @see IType
	 */
	public ITypeHierarchy newSupertypeHierarchy(IProgressMonitor monitor)
			throws ModelException {
		return this.newSupertypeHierarchy(DefaultWorkingCopyOwner.PRIMARY,
				monitor);
	}

	/*
	 * @see IType#newSupertypeHierarchy(ICompilationUnit[], IProgressMonitor)
	 */
	public ITypeHierarchy newSupertypeHierarchy(ISourceModule[] workingCopies,
			IProgressMonitor monitor) throws ModelException {

		CreateTypeHierarchyOperation op;
		IScriptProject scriptProject = getScriptProject();
		IDLTKSearchScope scope = SearchEngine.createSearchScope(scriptProject);
		op = new CreateTypeHierarchyOperation(this, workingCopies, scope, false);
		op.runOperation(monitor);
		return op.getResult();
	}

	/**
	 * @see IType#newSupertypeHierarchy(WorkingCopyOwner, IProgressMonitor)
	 */
	public ITypeHierarchy newSupertypeHierarchy(WorkingCopyOwner owner,
			IProgressMonitor monitor) throws ModelException {

		ISourceModule[] workingCopies = ModelManager.getModelManager()
				.getWorkingCopies(owner, true/* add primary working copies */);
		CreateTypeHierarchyOperation op;
		IScriptProject scriptProject = getScriptProject();
		IDLTKSearchScope scope = SearchEngine.createSearchScope(scriptProject);
		op = new CreateTypeHierarchyOperation(this, workingCopies, scope, false);
		op.runOperation(monitor);
		return op.getResult();
	}

	/**
	 * @see IType
	 */
	public ITypeHierarchy newTypeHierarchy(IProgressMonitor monitor)
			throws ModelException {
		final ITypeHierarchy hierarchy = TypeHierarchyBuilders
				.getTypeHierarchy(this, ITypeHierarchy.Mode.HIERARCHY, monitor);
		if (hierarchy != null) {
			return hierarchy;
		}
		CreateTypeHierarchyOperation op;
		op = new CreateTypeHierarchyOperation(this, null,
				createReferencingProjectsScope(), true);
		op.runOperation(monitor);
		return op.getResult();
	}

	/**
	 * @see IType
	 */
	public ITypeHierarchy newTypeHierarchy(IScriptProject project,
			IProgressMonitor monitor) throws ModelException {
		return newTypeHierarchy(project, DefaultWorkingCopyOwner.PRIMARY,
				monitor);
	}

	/**
	 * @see IType#newTypeHierarchy(IJavaProject, WorkingCopyOwner,
	 *      IProgressMonitor)
	 */
	public ITypeHierarchy newTypeHierarchy(IScriptProject project,
			WorkingCopyOwner owner, IProgressMonitor monitor)
			throws ModelException {
		if (project == null) {
			throw new IllegalArgumentException(Messages.hierarchy_nullProject);
		}
		ISourceModule[] workingCopies = ModelManager.getModelManager()
				.getWorkingCopies(owner, true/* add primary working copies */);
		ISourceModule[] projectWCs = null;
		if (workingCopies != null) {
			int length = workingCopies.length;
			projectWCs = new ISourceModule[length];
			int index = 0;
			for (int i = 0; i < length; i++) {
				ISourceModule wc = workingCopies[i];
				if (project.equals(wc.getScriptProject())) {
					projectWCs[index++] = wc;
				}
			}
			if (index != length) {
				System.arraycopy(projectWCs, 0,
						projectWCs = new ISourceModule[index], 0, index);
			}
		}
		CreateTypeHierarchyOperation op = new CreateTypeHierarchyOperation(
				this, projectWCs, project, true);
		op.runOperation(monitor);
		return op.getResult();
	}

	/*
	 * @see IType#newTypeHierarchy(ICompilationUnit[], IProgressMonitor)
	 */
	public ITypeHierarchy newTypeHierarchy(ISourceModule[] workingCopies,
			IProgressMonitor monitor) throws ModelException {

		CreateTypeHierarchyOperation op;
		op = new CreateTypeHierarchyOperation(this, workingCopies,
				createReferencingProjectsScope(), true);
		op.runOperation(monitor);
		return op.getResult();
	}

	/**
	 * @see IType#newTypeHierarchy(WorkingCopyOwner, IProgressMonitor)
	 */
	public ITypeHierarchy newTypeHierarchy(WorkingCopyOwner owner,
			IProgressMonitor monitor) throws ModelException {

		ISourceModule[] workingCopies = ModelManager.getModelManager()
				.getWorkingCopies(owner, true/* add primary working copies */);
		CreateTypeHierarchyOperation op;
		op = new CreateTypeHierarchyOperation(this, workingCopies,
				createReferencingProjectsScope(), true);
		op.runOperation(monitor);
		return op.getResult();
	}

	@Override
	public void printNode(CorePrinter output) {
		output.formatPrint("DLTK Binary Type:" + getElementName()); //$NON-NLS-1$
		output.indent();
		try {
			IModelElement modelElements[] = this.getChildren();
			for (int i = 0; i < modelElements.length; ++i) {
				IModelElement element = modelElements[i];
				if (element instanceof ModelElement) {
					((ModelElement) element).printNode(output);
				} else {
					output.print("Unknown element:" + element); //$NON-NLS-1$
				}
			}
		} catch (ModelException ex) {
			output.formatPrint(ex.getLocalizedMessage());
		}
		output.dedent();
	}
}
