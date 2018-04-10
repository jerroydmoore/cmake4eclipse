package jnmoore.cmake;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.cdtvariables.ICdtVariableManager;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.managedbuilder.core.ExternalBuildRunner;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;

import de.marw.cdt.cmake.core.internal.settings.CMakePreferences;
import de.marw.cdt.cmake.core.internal.settings.ConfigurationManager;

public class DockerBuildRunner extends ExternalBuildRunner {

  @Override
  public boolean invokeBuild(int kind, IProject project, IConfiguration configuration,
    IBuilder builder, IConsole console, IMarkerGenerator markerGenerator,
    IncrementalProjectBuilder projectBuilder, IProgressMonitor monitor) throws CoreException {

    SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
    boolean result = invokeExternalBuild(kind, project, configuration, builder, console,
      markerGenerator, projectBuilder, subMonitor.split(95));

    // delete the old contents of the build folder first.
    IFolder output = this.getBuildFolder(project, configuration);
    IResource[] contents = output.members();
    SubMonitor loopMonitor = subMonitor.split(4).setWorkRemaining(contents.length);
    for (int itor = contents.length - 1; itor >= 0; itor--) {
      IResource node = contents[itor];
      int type = node.getType();
      if (type == IResource.FOLDER || type == IResource.FILE) {
        node.delete(true, loopMonitor.split(1));
      }
    }

    // Copy the output from the container into the project
    IPath destination = new Path(DockerManager.getWorkspacePath()).append(output.getFullPath());

    String source = DockerManager.changeLocalWorkspaceToWorkspaceContainer(project, destination.toString());

    destination = destination.removeLastSegments(1); // go up one directory.

    // Consume the remainder of the monitor.
    subMonitor.split(1).subTask("Copying artifacts from the docker container");
    DockerManager.getInstance().copyFromRemote(project, source, destination, true);

    return result; // boolean = running make clean
  }

  // from cmake4eclipse
  private IFolder getBuildFolder(IProject project, IConfiguration config) {
    // set the top build dir path for the current configuration
    String buildDirStr = null;
    final ICConfigurationDescription cfgd = ManagedBuildManager.getDescriptionForConfiguration(config);
    try {
      CMakePreferences prefs = ConfigurationManager.getInstance().getOrLoad(cfgd);
      buildDirStr = prefs.getBuildDirectory();
    } catch (CoreException e) {
      // storage base is null; treat as bug in CDT..
      // log.log(new Status(IStatus.ERROR, CdtPlugin.PLUGIN_ID,
      // "falling back to hard coded build directory", e));
    }
    IPath buildP;
    if (buildDirStr == null) {
      // not configured: fall back to legacy behavior
      buildP = new Path("build").append(config.getName());
    } else {
      buildP = new Path(buildDirStr);
    }

    // Note that IPath from ICBuildSetting#getBuilderCWD() holding variables is
    // mis-constructed,
    // i.e. ${workspace_loc:/path} gets split into 2 path segments.
    // MBS does that and we need to handle that

    // So resolve variables here and return an workspace relative path to not
    // give CTD a chance to garble it up..
    ICdtVariableManager mngr = CCorePlugin.getDefault().getCdtVariableManager();
    try {
      String buildPathString = buildP.toString();
      buildPathString = mngr.resolveValue(buildPathString, "", "",
        ManagedBuildManager.getDescriptionForConfiguration(config));
      buildP = new Path(buildPathString);
    } catch (CdtVariableException e) {
      // log.log(new Status(IStatus.ERROR, CdtPlugin.PLUGIN_ID,
      // "variable expansion for build directory failed", e));
    }

    // buildFolder = project.getFolder(buildP);
    // return buildFolder;
    return project.getFolder(buildP);
  }
}
