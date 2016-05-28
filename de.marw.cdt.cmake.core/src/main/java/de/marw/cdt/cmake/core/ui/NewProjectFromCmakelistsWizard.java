/*******************************************************************************
 * Copyright (c) 2015 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.ui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.text.MessageFormat;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.Configuration;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedProject;
import org.eclipse.cdt.managedbuilder.templateengine.ProjectCreatedActions;
import org.eclipse.cdt.managedbuilder.ui.wizards.CfgHolder;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.internal.misc.StatusUtil;
import org.eclipse.ui.statushandlers.IStatusAdapterConstants;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import de.marw.cdt.cmake.core.CdtPlugin;

/**
 * @author Martin Weber
 */
public class NewProjectFromCmakelistsWizard extends BasicNewResourceWizard
    implements INewWizard {

  private final NewProjectModel model = new NewProjectModel();
  org.eclipse.cdt.managedbuilder.ui.wizards.NewMakeProjFromExisting a;
  private NewProjectFromCmakelistsPage page1;
  private NewProjectFromCmakelistsPage2 page2;

  /**
   *
   */
  public NewProjectFromCmakelistsWizard() {
    // TODO Auto-generated constructor stub
  }

  /*-
   * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
   */
  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    super.init(workbench, selection);
    setNeedsProgressMonitor(true);
    setWindowTitle("New Project");
  }

  @Override
  public void addPages() {
    page1 = new NewProjectFromCmakelistsPage(model);
    page2 = new NewProjectFromCmakelistsPage2(model);
    addPage(page1);
    addPage(page2);
  }

  /*-
   * @see org.eclipse.jface.wizard.Wizard#performFinish()
   */
  @Override
  public boolean performFinish() {
    return true;
  }

  /**
   * Creates a new project resource with the selected name.
   * <p>
   * In normal usage, this method is invoked after the user has pressed Finish
   * on the wizard; the enablement of the Finish button implies that all
   * controls on the pages currently contain valid values.
   * </p>
   *
   * @return the created project resource, or <code>null</code> if the project
   *         was not created
   */
  private IProject createNewProject() {

    // get a project handle
    final IProject project = ResourcesPlugin.getWorkspace().getRoot()
        .getProject(model.getProjectName());

    // get a project descriptor
    URI location = new File(model.getProjectLocation()).toURI();

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    final IProjectDescription description =
        workspace.newProjectDescription(project.getName());
    description.setLocationURI(location);

    ProjectCreatedActions pca = new ProjectCreatedActions();
    pca.setProject(project);
//    pca.setProjectLocation(locationPath);
    ManagedBuildManager.getConfigurationForDescription(cfgDesc);
    pca.setConfigs(configs.toArray(new IConfiguration[configs.size()]));
    pca.setArtifactExtension("*");
    IManagedBuildInfo info = pca.createProject(monitor, CCorePlugin.DEFAULT_INDEXER, isCProject);
    info.setValid(true);
    ManagedBuildManager.saveBuildInfo(project, true);


    // create the new project operation
    IRunnableWithProgress op = new IRunnableWithProgress() {
      public void run(IProgressMonitor monitor)
          throws InvocationTargetException {
        CreateProjectOperation op =
            new CreateProjectOperation(description, getWindowTitle());
        try {
          // see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=219901
          // directly execute the operation so that the undo state is
          // not preserved.  Making this undoable resulted in too many
          // accidental file deletions.
          op.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(getShell()));
        } catch (ExecutionException e) {
          throw new InvocationTargetException(e);
        }
      }
    };

    // run the new project creation operation
    try {
      getContainer().run(true, true, op);
    } catch (InterruptedException e) {
      return null;
    } catch (InvocationTargetException e) {
      Throwable t = e.getTargetException();
      if (t instanceof ExecutionException
          && t.getCause() instanceof CoreException) {
        CoreException cause = (CoreException) t.getCause();
        StatusAdapter status;
        if (cause.getStatus()
            .getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
          status = new StatusAdapter(
              new Status(IStatus.WARNING, CdtPlugin.PLUGIN_ID, IStatus.WARNING,
                  MessageFormat.format(
                      "The underlying file system is case insensitive."
                          + " There is an existing project or directory that conflicts with ''{0}''.",
                      project.getName()),
                  cause));
        } else {
          status = new StatusAdapter(
              new Status(cause.getStatus().getSeverity(), CdtPlugin.PLUGIN_ID,
                  cause.getStatus().getSeverity(), "Creation Problems", cause));
        }
        status.setProperty(IStatusAdapterConstants.TITLE_PROPERTY,
            "Creation Problems");
        StatusManager.getManager().handle(status, StatusManager.BLOCK);
      } else {
        StatusAdapter status = new StatusAdapter(new Status(IStatus.WARNING,
            CdtPlugin.PLUGIN_ID, 0,
            MessageFormat.format("Internal error: {0}", t.getMessage()), t));
        status.setProperty(IStatusAdapterConstants.TITLE_PROPERTY,
            "Creation Problems");
        StatusManager.getManager().handle(status,
            StatusManager.LOG | StatusManager.BLOCK);
      }
      return null;
    }

    return project;
  }

  private boolean performFinish2(IProject project) {
    final String projectName = model.getProjectName();
    final String locationStr = model.getProjectLocation();
    final boolean isCPP = page.isCPP();
    final IToolChain toolChain = page.getToolChain();

    IRunnableWithProgress op = new WorkspaceModifyOperation() {
      @Override
      protected void execute(IProgressMonitor monitor) throws CoreException,
          InvocationTargetException, InterruptedException {
        monitor.beginTask("Messages.NewMakeProjFromExisting_1", 10);

        // Create Project
        try {
          IWorkspace workspace = ResourcesPlugin.getWorkspace();
          IProject project = workspace.getRoot().getProject(projectName);

          CCorePlugin.getDefault().createCDTProject(project.getDescription(),
              project, monitor);

          // Optionally C++ natures
          if (isCPP)
            CCProjectNature.addCCNature(project,
                new SubProgressMonitor(monitor, 1));

          // Set up build information
          final ICProjectDescriptionManager pdMgr =
              CoreModel.getDefault().getProjectDescriptionManager();
          final ICProjectDescription projDesc =
              pdMgr.createProjectDescription(project, false);
          monitor.worked(10);
          IProjectType type = ManagedBuildManager.getProjectType(projTypeId);
          IManagedProject mProj =
              ManagedBuildManager.createManagedProject(project, type);

          IManagedBuildInfo info = ManagedBuildManager.createBuildInfo(project);
          info.setManagedProject(mProj);
          monitor.worked(1);
          // von tests
          IConfiguration cfgs[] = type.getConfigurations();
          for (int i = 0; i < cfgs.length; i++) {
            String id =
                ManagedBuildManager.calculateChildId(cfgs[i].getId(), null);
            IConfiguration config = new Configuration((ManagedProject) mProj,
                (Configuration) cfgs[i], id, false, true, false);
            CConfigurationData data = config.getConfigurationData();
            Assert.assertNotNull("data is null for created configuration",
                data);
            des.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID,
                data);
          }
          // XXX altes
          CfgHolder cfgHolder = new CfgHolder(toolChain, null);
          String s = toolChain == null ? "0" : toolChain.getId(); //$NON-NLS-1$
          IConfiguration config =
              new Configuration(mProj, (ToolChain) toolChain,
                  ManagedBuildManager.calculateChildId(s, null),
                  cfgHolder.getName());
          IBuilder builder = config.getEditableBuilder();
          builder.setManagedBuildOn(false);
          CConfigurationData data = config.getConfigurationData();
          ICConfigurationDescription cfgDes = projDesc.createConfiguration(
              ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);

          ConfigurationDataProvider.setDefaultLanguageSettingsProviders(project,
              config, cfgDes);

          monitor.worked(1);

          pdMgr.setProjectDescription(project, projDesc);
        } catch (Throwable e) {
          ManagedBuilderUIPlugin.log(e);
        }
        monitor.done();
      }
    };

    try {
      getContainer().run(true, true, op);
    } catch (InvocationTargetException e) {
      e.printStackTrace();
      return false;
    } catch (InterruptedException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

}
