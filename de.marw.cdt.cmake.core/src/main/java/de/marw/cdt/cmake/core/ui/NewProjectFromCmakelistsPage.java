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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.ui.newui.CDTPrefUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Page to select existing code location and toolchain.
 *
 * @author Martin Weber
 */
class NewProjectFromCmakelistsPage extends WizardPage {

  private final NewProjectModel model;

  private Button langc;
  private Button langcpp;
  private IWorkspaceRoot root;
  private List tcList;
  private Map<String, IToolChain> tcMap = new HashMap<String, IToolChain>();

  /**
   * True if the user entered a non-empty string in the project name field. In
   * that state, we avoid automatically filling the project name field with the
   * directory name (last segment of the location) he has entered.
   */
  private boolean projectNameSetByUser;

  NewProjectFromCmakelistsPage(NewProjectModel model) {
    super("newProjectFromCmakelistsPage");

    if (model == null) {
      throw new NullPointerException("model");
    }
    this.model = model;

    setDescription("Create a new C/C++ project from existing code, "
        + "configured to use cmake as build script generator");

    setTitle("Import Existing Code");
    root = ResourcesPlugin.getWorkspace().getRoot();
    setPageComplete(false);
  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);

    initializeDialogUnits(parent);

//    PlatformUI.getWorkbench().getHelpSystem().setHelp(composite,
//            IIDEHelpContextIds.NEW_PROJECT_WIZARD_PAGE);

    composite.setLayout(new GridLayout());
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    Button browseButton = addLocationSelector(composite);
    // Scale the browse button based on the rest of the dialog
    setButtonLayoutData(browseButton);

    addProjectNameSelector(composite);
    addLanguageSelector(composite);
    addToolchainSelector(composite);

    validatePage();
    setControl(composite);
    Dialog.applyDialogFont(composite);
  }

  private void addProjectNameSelector(Composite parent) {
    // project specification group
    Composite projectGroup = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    projectGroup.setLayout(layout);
    projectGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    // new project label
    Label projectLabel = new Label(projectGroup, SWT.NONE);
    projectLabel.setText("Project name");
    projectLabel.setFont(parent.getFont());

    // new project name entry field
    model.projectNameField = new Text(projectGroup, SWT.BORDER);
    GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
    data.widthHint = 250;
    model.projectNameField.setLayoutData(data);
    model.projectNameField.setFont(parent.getFont());
    model.projectNameField.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        validatePage();
        if (model.getProjectName().isEmpty()) {
          projectNameSetByUser = false;
        }
      }
    });

    // Note that the modify listener gets called not only when the user enters text but also when we
    // programmatically set the field. This listener only gets called when the user modifies the field
    model.projectNameField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        projectNameSetByUser = true;
      }
    });
  }

  /**
   * Validates whether this page's controls currently all contain valid values,
   * setting the page error message and Finish button state accordingly
   */
  private void validatePage() {
    // Don't generate an error if project name or projectLocationField is empty, but do disable Finish button.
    String msg = null;
    boolean complete = true; // ultimately treated as false if msg != null

    String name = model.getProjectName();
    if (name.isEmpty()) {
      complete = false;
    } else {
      IStatus status =
          ResourcesPlugin.getWorkspace().validateName(name, IResource.PROJECT);
      if (!status.isOK()) {
        msg = status.getMessage();
      } else {
        IProject project = root.getProject(name);
        if (project.exists()) {
          msg = "Project already exists";
        }
      }
    }
    if (msg == null) {
      String loc = model.getProjectLocation();
      if (loc.isEmpty()) {
        complete = false;
      } else {
        final File file = new File(loc);
        if (file.isDirectory()) {
          // Set the project name to the directory name but not if the user has supplied a name.
          // Use a job to ensure proper sequence of activity, as setting the Text
          // will invoke the listener, which will invoke this method.
          if (!projectNameSetByUser && !name.equals(file.getName())) {
            Job wjob = new WorkbenchJob("update project name") { //$NON-NLS-1$
              @Override
              public IStatus runInUIThread(IProgressMonitor monitor) {
                if (!model.projectNameField.isDisposed()) {
                  model.projectNameField.setText(file.getName());
                }
                return Status.OK_STATUS;
              }
            };
            wjob.setSystem(true);
            wjob.schedule();
          }
        } else {
          msg = "Not a valid directory";
        }
      }
    }

    setErrorMessage(msg);
    setPageComplete(msg == null && complete);
  }

  private Button addLocationSelector(Composite parent) {
    Group group = new Group(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    group.setLayout(layout);
    group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    group.setText("Project location");

    model.projectLocationField = new Text(group, SWT.BORDER);
    model.projectLocationField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    model.projectLocationField.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        validatePage();
      }
    });

    Button browse = new Button(group, SWT.NONE);
    browse.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
    browse.setText("Browse...");
    browse.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        DirectoryDialog dialog = new DirectoryDialog(model.projectLocationField.getShell());
        dialog.setText("Select root directory of existing code");
        String dir = dialog.open();
        if (dir != null)
          model.projectLocationField.setText(dir);
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
      }
    });
    return browse;
  }

  private void addLanguageSelector(Composite parent) {
    Group group = new Group(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    group.setLayout(layout);
    group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    group.setText("Languages");

    // TODO, should be a way to dynamically list these
    langc = new Button(group, SWT.CHECK);
    langc.setText("C"); //$NON-NLS-1$
    langc.setSelection(true);

    langcpp = new Button(group, SWT.CHECK);
    langcpp.setText("C++"); //$NON-NLS-1$
    langcpp.setSelection(true);
  }

  private void addToolchainSelector(Composite parent) {
    Group group = new Group(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    group.setLayout(layout);
    group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    group.setText("Toolchain (to satisfy indexer)");

    tcList = new List(group, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);

    // Base the List control size on the number of total toolchains, up to 15 entries, but allocate for no
    // less than five (small list boxes look strange). A vertical scrollbar will appear as needed
    updateTcMap(false);
    gd.heightHint =
        tcList.getItemHeight() * (1 + Math.max(Math.min(tcMap.size(), 15), 5)); // +1 for <none>
    tcList.setLayoutData(gd);

    final Button supportedOnly = new Button(group, SWT.CHECK);
    supportedOnly.setSelection(false);
    supportedOnly
        .setText("Show only available toolchains that support this platform");
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 2;
    supportedOnly.setLayoutData(gd);
    supportedOnly.setSelection(!CDTPrefUtil.getBool(CDTPrefUtil.KEY_NOSUPP));
    supportedOnly.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updateTcWidget(supportedOnly.getSelection());
      }
    });

    updateTcWidget(true);
  }

  /**
   * Load our map and with the suitable toolchains and then populate the List
   * control
   *
   * @param supportedOnly
   *        if true, consider only supported toolchains
   */
  private void updateTcWidget(boolean supportedOnly) {
    updateTcMap(supportedOnly);
    ArrayList<String> names = new ArrayList<String>(tcMap.keySet());
    Collections.sort(names);

    tcList.removeAll();
    tcList.add("<none>"); // <none>
    for (String name : names)
      tcList.add(name);

    tcList.setSelection(0); // select <none>
  }

  /**
   * Load our map with the suitable toolchains.
   *
   * @param supportedOnly
   *        if true, add only toolchains that are available and which support
   *        the host platform
   */
  private void updateTcMap(boolean supportedOnly) {
    tcMap.clear();
    IToolChain[] toolChains = ManagedBuildManager.getRealToolChains();
    for (IToolChain toolChain : toolChains) {
      if (toolChain.isAbstract() || toolChain.isSystemObject())
        continue;
      if (supportedOnly) {
        if (!toolChain.isSupported()
            || !ManagedBuildManager.isPlatformOk(toolChain)) {
          continue;
        }
      }
      tcMap.put(toolChain.getUniqueRealName(), toolChain);
    }
  }

  private IToolChain getToolChain() {
    String[] selection = tcList.getSelection();
    return selection.length != 0 ? tcMap.get(selection[0]) : null;
  }

}
