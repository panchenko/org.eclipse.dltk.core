/*******************************************************************************
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html  
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IBuildpathEntry;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IScriptProjectFilenames;
import org.eclipse.dltk.core.environment.EnvironmentManager;
import org.eclipse.dltk.core.environment.IEnvironment;
import org.eclipse.dltk.internal.ui.util.CoreUtility;
import org.eclipse.dltk.internal.ui.wizards.BuildpathDetector;
import org.eclipse.dltk.internal.ui.wizards.NewWizardMessages;
import org.eclipse.dltk.launching.IInterpreterInstall;
import org.eclipse.dltk.launching.ScriptRuntime;
import org.eclipse.dltk.ui.DLTKUILanguageManager;
import org.eclipse.dltk.ui.IDLTKUILanguageToolkit;
import org.eclipse.dltk.ui.PreferenceConstants;
import org.eclipse.dltk.ui.util.ExceptionHandler;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;

/**
 * @since 2.0
 */
public class ProjectCreator {

	private final IProjectWizard owner;
	private final ILocationGroup fLocation;

	private URI fCurrProjectLocation; // null if location is platform location
	private IProject fCurrProject;

	private boolean fKeepContent;

	private ProjectMetadataBackup projectFileBackup = null;
	private Boolean fIsAutobuild;

	public ProjectCreator(IProjectWizard owner, ILocationGroup locationGroup) {
		this.owner = owner;
		this.fLocation = locationGroup;
		fCurrProjectLocation = null;
		fCurrProject = null;
		fKeepContent = false;

		fIsAutobuild = null;
	}

	protected IWizardContainer getContainer() {
		return owner.getContainer();
	}

	protected Shell getShell() {
		return getContainer().getShell();
	}

	private void rememberExistingFiles(URI projectLocation)
			throws CoreException {
		if (projectFileBackup == null) {
			projectFileBackup = new ProjectMetadataBackup();
		}
		projectFileBackup.backup(projectLocation, new String[] {
				IScriptProjectFilenames.PROJECT_FILENAME,
				IScriptProjectFilenames.BUILDPATH_FILENAME });
	}

	private void restoreExistingFiles(IProgressMonitor monitor)
			throws CoreException {
		if (projectFileBackup != null) {
			projectFileBackup.restore(monitor);
		}
	}

	/**
	 * Called from the wizard on cancel.
	 */
	public void removeProject() {
		if (fCurrProject == null || !fCurrProject.exists()) {
			return;
		}

		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				doRemoveProject(monitor);
			}
		};

		try {
			getContainer().run(true, true,
					new WorkspaceModifyDelegatingOperation(op));
		} catch (InvocationTargetException e) {
			final String title = NewWizardMessages.ScriptProjectWizardSecondPage_error_remove_title;
			final String message = NewWizardMessages.ScriptProjectWizardSecondPage_error_remove_message;
			ExceptionHandler.handle(e, getShell(), title, message);
		} catch (InterruptedException e) {
			// cancel pressed
		}
		resetPages();
	}

	final void doRemoveProject(IProgressMonitor monitor)
			throws InvocationTargetException {
		// inside workspace
		final boolean noProgressMonitor = (fCurrProjectLocation == null);
		if (monitor == null || noProgressMonitor) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(
				NewWizardMessages.ScriptProjectWizardSecondPage_operation_remove,
				3);
		try {
			try {
				boolean removeContent = !fKeepContent
						&& fCurrProject
								.isSynchronized(IResource.DEPTH_INFINITE);
				fCurrProject.delete(removeContent, false,
						new SubProgressMonitor(monitor, 2));

				restoreExistingFiles(new SubProgressMonitor(monitor, 1));
			} finally {
				// fIsAutobuild must be set
				CoreUtility.enableAutoBuild(fIsAutobuild.booleanValue());
				fIsAutobuild = null;
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
			resetSteps();
			fCurrProject = null;
			fKeepContent = false;
		}
	}

	public static interface IProjectCreateStep {

		String KIND_INIT = "init"; //$NON-NLS-1$
		String KIND_INIT_UI = "initUI"; //$NON-NLS-1$
		String KIND_FINISH = "finish"; //$NON-NLS-1$

		int BEFORE = -1;

		boolean isRecurrent();

		boolean isCancelable();

		void execute(IProject project, IProgressMonitor monitor)
				throws CoreException, InterruptedException;

	}

	public static abstract class ProjectCreateStep implements
			IProjectCreateStep {

		public boolean isCancelable() {
			return false;
		}

		public boolean isRecurrent() {
			return false;
		}

	}

	private static class StepState {
		final String kind;
		final int priority;
		final IProjectCreateStep step;
		final IWizardPage page;

		public StepState(String kind, int priority, IProjectCreateStep step,
				IWizardPage page) {
			this.kind = kind;
			this.priority = priority;
			this.step = step;
			this.page = page;
		}

	}

	private static interface IStepTracker {
		boolean canExecute(StepState state);

		void executed(StepState state);
	}

	private static class StepTracker implements IStepTracker {

		private final Set<StepState> executed = new HashSet<StepState>();

		public void reset() {
			executed.clear();
		}

		/**
		 * @param state
		 * @return
		 */
		public boolean canExecute(StepState state) {
			return state.step.isRecurrent() || !executed.contains(state);
		}

		/**
		 * @param state
		 */
		public void executed(StepState state) {
			executed.add(state);
		}

	}

	private static abstract class FilteredStepTracker implements IStepTracker {

		private final IStepTracker target;

		public FilteredStepTracker(IStepTracker target) {
			this.target = target;
		}

		public boolean canExecute(StepState state) {
			return select(state) && target.canExecute(state);
		}

		protected abstract boolean select(StepState state);

		public void executed(StepState state) {
			target.executed(state);
		}

	}

	private static class FinishStepTracker extends FilteredStepTracker {

		private final Set<StepState> executed = new HashSet<StepState>();

		public FinishStepTracker(IStepTracker target) {
			super(target);
		}

		@Override
		protected boolean select(StepState state) {
			return !executed.contains(state);
		}

		@Override
		public void executed(StepState state) {
			super.executed(state);
			executed.add(state);
		}

	}

	private class BeforeCurrentPageStepTracker extends FilteredStepTracker {

		public BeforeCurrentPageStepTracker(IStepTracker target) {
			super(target);
		}

		final int currentPageIndex = indexOfPage(owner.getContainer()
				.getCurrentPage());

		@Override
		protected boolean select(StepState state) {
			final int index = indexOfPage(state.page);
			return index < currentPageIndex || index == currentPageIndex
					&& IProjectCreateStep.BEFORE == state.priority;
		}
	}

	private final List<StepState> fSteps = new ArrayList<StepState>();

	private final IStepTracker fStepTracker = new StepTracker();

	/**
	 * Adds the specified step
	 * 
	 * @param kind
	 * @param priority
	 *            the priority of the specified step. steps with greater
	 *            priority are executed later
	 * @param step
	 * @param mode
	 */
	public void addStep(String kind, int priority, IProjectCreateStep step,
			IWizardPage page) {
		for (StepState state : fSteps) {
			Assert.isLegal(step != state.step);
		}
		fSteps.add(new StepState(kind, priority, step, page));
	}

	private static final boolean DEBUG = false;

	/**
	 * @param kind
	 * @throws InterruptedException
	 * @throws CoreException
	 * @throws InvocationTargetException
	 */
	private void executeSteps(IStepTracker stepTracker, String kind,
			IProgressMonitor monitor) throws CoreException,
			InterruptedException {
		final List<StepState> selection = new ArrayList<StepState>();
		for (StepState state : fSteps) {
			if (kind.equals(state.kind) && owner.isEnabledPage(state.page)
					&& stepTracker.canExecute(state)) {
				selection.add(state);
			}
		}
		if (selection.isEmpty()) {
			return;
		}
		Collections.sort(selection, new Comparator<StepState>() {
			public int compare(StepState a, StepState b) {
				final int result = a.priority - b.priority;
				if (result != 0) {
					return result;
				}
				return indexOfPage(a.page) - indexOfPage(b.page);
			}
		});
		for (StepState state : selection) {
			if (DEBUG) {
				System.out.println("execute " + state.step); //$NON-NLS-1$
			}
			state.step.execute(fCurrProject, monitor);
			stepTracker.executed(state);
		}
	}

	/**
	 * @param page
	 * @return
	 */
	protected int indexOfPage(IWizardPage page) {
		final IWizardPage[] pages = owner.getPages();
		for (int i = 0; i < pages.length; ++i) {
			if (page == pages[i]) {
				return i;
			}
		}
		return -1;
	}

	private void resetSteps() {
		((StepTracker) fStepTracker).reset();
	}

	private void resetPages() {
		for (IWizardPage page : owner.getPages()) {
			if (page instanceof IProjectWizardPage) {
				((IProjectWizardPage) page).resetProjectWizardPage();
			}
		}
	}

	public void changeToNewProject() {
		fKeepContent = fLocation.isExistingLocation();

		final BeforeCurrentPageStepTracker stepTracker = new BeforeCurrentPageStepTracker(
				fStepTracker);

		final boolean cancelable = isCancelable(stepTracker);

		final IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				try {
					if (fIsAutobuild == null) {
						fIsAutobuild = Boolean.valueOf(CoreUtility
								.enableAutoBuild(false));
					}
					updateProject(monitor, stepTracker);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} catch (OperationCanceledException e) {
					throw new InterruptedException();
				} finally {
					monitor.done();
				}
			}
		};

		try {
			getContainer().run(true, cancelable,
					new WorkspaceModifyDelegatingOperation(op));
		} catch (InvocationTargetException e) {
			final String title = NewWizardMessages.ScriptProjectWizardSecondPage_error_title;
			final String message = NewWizardMessages.ScriptProjectWizardSecondPage_error_message;
			ExceptionHandler.handle(e, getShell(), title, message);
		} catch (InterruptedException e) {
			// cancel pressed
			if (cancelable)
				throw new OperationCanceledException();
		}
	}

	/**
	 * @param stepTracker
	 * @return
	 */
	private boolean isCancelable(IStepTracker stepTracker) {
		final List<String> kinds = new ArrayList<String>();
		kinds.add(IProjectCreateStep.KIND_INIT);
		kinds.add(IProjectCreateStep.KIND_INIT_UI);
		kinds.add(IProjectCreateStep.KIND_FINISH);
		for (StepState state : fSteps) {
			if (kinds.contains(state.kind) && owner.isEnabledPage(state.page)
					&& stepTracker.canExecute(state)
					&& state.step.isCancelable()) {
				return true;
			}
		}
		return false;
	}

	final void updateProject(IProgressMonitor monitor, IStepTracker stepTracker)
			throws CoreException, InterruptedException {
		fCurrProject = fLocation.getProjectHandle();
		fCurrProjectLocation = getProjectLocationURI();
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		try {
			monitor.beginTask(
					NewWizardMessages.ScriptProjectWizardSecondPage_operation_initialize,
					70);
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			URI realLocation = fCurrProjectLocation;
			if (realLocation == null) { // inside workspace
				try {
					URI rootLocation = ResourcesPlugin.getWorkspace().getRoot()
							.getLocationURI();
					/*
					 * Path.fromPortableString() is required here, because it
					 * handles path in the way expected by URI constructor. (On
					 * windows the path keeps the leading slash, e.g.
					 * "/C:/Users/alex/...")
					 */
					realLocation = new URI(rootLocation.getScheme(), null, Path
							.fromPortableString(rootLocation.getPath())
							.append(fCurrProject.getName()).toString(), null);
				} catch (URISyntaxException e) {
					Assert.isTrue(false, "Can't happen"); //$NON-NLS-1$
				}
			}

			rememberExistingFiles(realLocation);

			createProject(fCurrProject, fCurrProjectLocation,
					new SubProgressMonitor(monitor, 20));

			executeSteps(stepTracker, IProjectCreateStep.KIND_INIT, monitor);
			executeSteps(stepTracker, IProjectCreateStep.KIND_INIT_UI,
					new SubProgressMonitor(monitor, 20));
			executeSteps(stepTracker, IProjectCreateStep.KIND_FINISH,
					new SubProgressMonitor(monitor, 30));

			/*
			 * create the script project to allow the use of the new source
			 * folder page
			 */
		} finally {
			monitor.done();
		}
	}

	protected IDLTKUILanguageToolkit getUILanguageToolkit() {
		return DLTKUILanguageManager.getLanguageToolkit(getScriptNature());
	}

	public String getScriptNature() {
		return ((ProjectWizard) owner).getScriptNature();
	}

	protected IBuildpathDetector createBuildpathDetector() {
		return new BuildpathDetector(fCurrProject, getLanguageToolkit());
	}

	protected IDLTKLanguageToolkit getLanguageToolkit() {
		return DLTKLanguageManager.getLanguageToolkit(getScriptNature());
	}

	private URI getProjectLocationURI() throws CoreException {
		if (fLocation.isInWorkspace()) {
			return null;
		}
		return fLocation.getLocationURI();
	}

	private void reuseInterpreterLibraries(IProgressMonitor monitor)
			throws CoreException {
		IInterpreterInstall projectInterpreter = this.fLocation
				.getInterpreter();
		if (projectInterpreter == null) {
			final String nature = getScriptNature();
			if (nature != null) {
				projectInterpreter = ScriptRuntime
						.getDefaultInterpreterInstall(nature,
								fLocation.getEnvironment());
			}
		}
		if (projectInterpreter != null) {
			// Locate projects with same interpreter.
			ProjectWizardUtils.reuseInterpreterLibraries(fCurrProject,
					projectInterpreter, monitor);
		}
	}

	public IProject getProject() {
		return fCurrProject;
	}

	/**
	 * Called from the wizard on finish.
	 * 
	 * @param monitor
	 * @throws CoreException
	 * @throws InterruptedException
	 */
	public void performFinish(IProgressMonitor monitor) throws CoreException,
			InterruptedException {
		try {
			monitor.beginTask(
					NewWizardMessages.ScriptProjectWizardSecondPage_operation_create,
					4);
			final IStepTracker finishStepTracker = new FinishStepTracker(
					fStepTracker);
			if (fCurrProject == null) {
				updateProject(new SubProgressMonitor(monitor, 1),
						finishStepTracker);
			}
			executeSteps(finishStepTracker, IProjectCreateStep.KIND_INIT,
					new SubProgressMonitor(monitor, 1));
			executeSteps(finishStepTracker, IProjectCreateStep.KIND_INIT_UI,
					new SubProgressMonitor(monitor, 1));
			executeSteps(finishStepTracker, IProjectCreateStep.KIND_FINISH,
					new SubProgressMonitor(monitor, 1));

			if (!fKeepContent) {
				if (DLTKCore.DEBUG) {
					System.err
							.println("Add compiler compilance options here..."); //$NON-NLS-1$
				}
				// String compliance= fFirstPage.getCompilerCompliance();
				// if (compliance != null) {
				// IScriptProject project= DLTKCore.create(fCurrProject);
				// Map options= project.getOptions(false);
				// ModelUtil.setCompilanceOptions(options, compliance);
				// project.setOptions(options);
				// }
			}

			// Don't rebuild external libraries if project with same
			// interpreter exists.
			reuseInterpreterLibraries(monitor);
		} finally {
			monitor.done();
			fCurrProject = null;
			if (fIsAutobuild != null) {
				CoreUtility.enableAutoBuild(fIsAutobuild.booleanValue());
				fIsAutobuild = null;
			}
		}
	}

	/**
	 * Helper method to create and open a IProject. The project location is
	 * configured. No natures are added.
	 * 
	 * @param project
	 *            The handle of the project to create.
	 * @param locationURI
	 *            The location of the project or <code>null</code> to create the
	 *            project in the workspace
	 * @param monitor
	 *            a progress monitor to report progress or <code>null</code> if
	 *            progress reporting is not desired
	 * @throws CoreException
	 *             if the project couldn't be created
	 * @see org.eclipse.core.resources.IProjectDescription#setLocationURI(java.net.URI)
	 */
	protected void createProject(IProject project, URI locationURI,
			IProgressMonitor monitor) throws CoreException {
		BuildpathsBlock.createProject(project, locationURI, monitor);
		final IEnvironment environment = fLocation.getEnvironment();
		final IEnvironment pEnv = EnvironmentManager.detectEnvironment(project);
		if (!environment.equals(pEnv)) {
			EnvironmentManager.setEnvironmentId(project, environment.getId(),
					false);
		} else {
			EnvironmentManager.setEnvironmentId(project, null, false);
		}
	}

	private static final int WORK_INIT_BP = 20;

	protected IBuildpathEntry[] initBuildpath(IProgressMonitor monitor)
			throws CoreException {
		if (fLocation.getDetect()) {
			if (!fCurrProject.getFile(
					IScriptProjectFilenames.BUILDPATH_FILENAME).exists()) {
				final IBuildpathDetector detector = createBuildpathDetector();
				detector.detectBuildpath(new SubProgressMonitor(monitor,
						WORK_INIT_BP));
				return detector.getBuildpath();
			} else {
				monitor.worked(WORK_INIT_BP);
				return null;
			}
		} else if (fLocation.isSrc()) {
			final IDLTKUILanguageToolkit toolkit = getUILanguageToolkit();
			final IPath srcPath = toolkit != null ? new Path(
					toolkit.getString(PreferenceConstants.SRC_SRCNAME))
					: Path.EMPTY;
			if (srcPath.segmentCount() > 0) {
				final IFolder folder = fCurrProject.getFolder(srcPath);
				CoreUtility.createFolder(folder, true, true,
						new SubProgressMonitor(monitor, WORK_INIT_BP));
			} else {
				monitor.worked(WORK_INIT_BP);
			}
			final IPath projectPath = fCurrProject.getFullPath();
			// configure the buildpath entries, including the default
			// InterpreterEnvironment library.
			List<IBuildpathEntry> cpEntries = new ArrayList<IBuildpathEntry>();
			cpEntries.add(DLTKCore.newSourceEntry(projectPath.append(srcPath)));
			cpEntries.addAll(getDefaultBuildpathEntries());
			return cpEntries.toArray(new IBuildpathEntry[cpEntries.size()]);
		} else {
			IPath projectPath = fCurrProject.getFullPath();
			List<IBuildpathEntry> cpEntries = new ArrayList<IBuildpathEntry>();
			cpEntries.add(DLTKCore.newSourceEntry(projectPath));
			cpEntries.addAll(getDefaultBuildpathEntries());
			monitor.worked(WORK_INIT_BP);
			return cpEntries.toArray(new IBuildpathEntry[cpEntries.size()]);
		}
	}

	/**
	 * @since 3.0
	 */
	protected List<IBuildpathEntry> getDefaultBuildpathEntries() {
		return ProjectWizardUtils.getDefaultBuildpathEntry(fLocation);
	}

}
