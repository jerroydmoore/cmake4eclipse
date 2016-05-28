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

import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.swt.widgets.Text;

/**
 * Data of a new project being created.
 *
 * @author Martin Weber
 */
/* package */class NewProjectModel {
  /** absolute file system path to the project directory */
  Text projectLocationField;
  /** name of the project to create */
  Text projectNameField;
  /** path of the CMakeLists.txt file, relative to the project directory */
  Text cmakelistsLocationField;

  boolean hasC = true;
  boolean hasCPP = true;
  IToolChain toolChain;

  String getProjectName() {
    return this.projectNameField.getText().trim();
  }

  String getProjectLocation() {
    return this.projectLocationField.getText().trim();
  }

  String getCmakelistsLocation() {
    return this.cmakelistsLocationField.getText().trim();
  }

}
