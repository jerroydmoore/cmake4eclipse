package jnmoore.cmake;

import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator2;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;

import de.marw.cdt.cmake.core.internal.BuildscriptGenerator;

public class DockerizedBuildscriptGenerator extends BuildscriptGenerator implements IManagedBuilderMakefileGenerator2 {

  // Save the project so we can get path and member information
  protected IProject project;

  // Save the monitor reference for reporting back to the user
  protected IProgressMonitor monitor;

  // Config is "Debug" or "Release"
  protected IConfiguration config;

  // Builder gets us the CmdLauncher
  protected IBuilder builder;

  public DockerizedBuildscriptGenerator() {
    super();
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator2#initialize(int, org.eclipse.cdt.managedbuilder.core.IConfiguration, org.eclipse.cdt.managedbuilder.core.IBuilder, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public void initialize(int buildKind, IConfiguration cfg, IBuilder builder, IProgressMonitor monitor) {
    initialize(cfg.getOwner().getProject(), monitor, cfg, builder);
    super.initialize(buildKind, cfg, builder, monitor);
  }

  /*-
   * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#initialize(org.eclipse.core.resources.IProject, org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public void initialize(IProject project, IManagedBuildInfo info, IProgressMonitor monitor) {
    // This is a legacy function. Implement it for backwards compatibility
    initialize(project, monitor, info.getDefaultConfiguration(), config.getEditableBuilder());
    super.initialize(project, info, monitor);
  }

  // set members and set project for commandLauncher.
  void initialize(IProject project, IProgressMonitor monitor, IConfiguration cfg, IBuilder builder) {
    this.project = project;
    this.monitor = monitor;
    this.config = cfg;
    this.builder = builder;
    
    // setProject seems to not be called before buildscript generation.
    this.builder.getCommandLauncher().setProject(project);
  }

  @Override
  public MultiStatus regenerateMakefiles() throws CoreException {
    DockerManager instance = DockerManager.getInstance();
    // This project may not have been created with our project creation wizard
    instance.registerProjectUsingDefaults(project, false);

    instance.copyProjectToRemote(project);

    // Run cmake
    MultiStatus status = super.regenerateMakefiles();

    // Copy items back from the docker container.

    // returns relative path to workspace root
    IPath destination = this.getBuildWorkingDir();
    // convert to abs. path
    destination = new Path(DockerManager.getWorkspacePath()).append(destination);

    String source = DockerManager.changeLocalWorkspaceToWorkspaceContainer(project, destination.toString());

    destination = destination.removeLastSegments(1); // go up one directory.

    instance.copyFromRemote(project, source, destination, true);
    return status;
  }

}
