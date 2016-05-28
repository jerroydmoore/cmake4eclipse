/*******************************************************************************
 * Copyright (c) 2016 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.ui;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

/**
 * @author Martin Weber
 */
class NewProjectFromCmakelistsPage2 extends WizardPage {

  private final NewProjectModel model;

  /**
   * @param model
   */
  NewProjectFromCmakelistsPage2(NewProjectModel model) {
    super("Import Existing Code", "Import Existing Code", null);

    if (model == null) {
      throw new NullPointerException("model");
    }
    this.model = model;

    setDescription("Select CMakeLists.txt file");
  }

  /*-
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);

    initializeDialogUnits(parent);
    composite.setLayout(new GridLayout());
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    Button browseButton = addCMakeListsSelector(composite);

    // Scale the browse button based on the rest of the dialog
    setButtonLayoutData(browseButton);

    validatePage();
    setControl(composite);
    Dialog.applyDialogFont(composite);
  }

  private Button addCMakeListsSelector(Composite parent) {
    Group group = new Group(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    group.setLayout(layout);
    group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    group.setText("CMakeLists.txt location");

    model.cmakelistsLocationField = new Text(group, SWT.BORDER);
    model.cmakelistsLocationField
        .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    model.cmakelistsLocationField.addModifyListener(new ModifyListener() {
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
        FileDialog dialog =
            new FileDialog(model.cmakelistsLocationField.getShell(),
                SWT.APPLICATION_MODAL | SWT.OPEN);
        dialog.setText("Select top level CMakeLists.txt file");
        dialog.setFilterNames(new String[] { "CMakeLists.txt" });
        dialog.setFilterExtensions(new String[] { "CMakeLists.txt" });
        String projectDirectory = model.projectLocationField.getText();
        dialog.setFilterPath(projectDirectory);
        String file = dialog.open();
        if (file != null) {
          // make relative to project dir
          final IPath projPath = new Path(projectDirectory);
          IPath cmPath = new Path(file).makeRelativeTo(projPath);
          model.cmakelistsLocationField.setText(cmPath.toOSString());
        }
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
      }
    });
    return browse;
  }

  private void validatePage() {
    String msg = null;

    final String cmName = model.getCmakelistsLocation();
    if (cmName.isEmpty()) {
      msg = "Please specify CMakeLists.txt location";
    } else {
      IPath cmPath = new Path(cmName);
      final IPath projPath = new Path(model.projectLocationField.getText());
      if (cmPath.isAbsolute()) {
        if (!projPath.isPrefixOf(cmPath)) {
          msg = "CMakeLists.txt location must be below project location";
        } else {
          // make relative to project dir
          cmPath = cmPath.makeRelativeTo(projPath);
        }
      }
      System.out.println("# in NewProjectFromCmakelistsPage2.validatePage()");
      final IPath path = projPath.append(cmPath);
      if (!path.toFile().isFile()) {
        msg = "File " + path.toOSString() + " does not exist";
      }
    }
    setErrorMessage(msg);
    setPageComplete(msg == null);
  }

}
