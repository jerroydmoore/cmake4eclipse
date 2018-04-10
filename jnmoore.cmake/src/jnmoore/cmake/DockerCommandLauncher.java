package jnmoore.cmake;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.eclipse.cdt.core.CommandLauncher;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;

public class DockerCommandLauncher extends CommandLauncher {

  static IPath ROOT_DIR = new Path("/");
  static IPath BASH = new Path("bash");
  protected static String DOCKER_COMMAND = "docker";
  public DockerCommandLauncher() {
    super();
  }

  @Override
  public void setProject(IProject project) {
    super.setProject(project);
  }

  @Override
  public IProject getProject() {
    return super.getProject();
  }

  @Override
  public void showCommand(boolean show) {
    super.showCommand(show);
  }

  @Override
  public String getErrorMessage() {
    return super.getErrorMessage();
  }

  @Override
  public void setErrorMessage(String error) {
    System.out.println("DockerCommandLauncher.setErrorMessage(" + error + ")");
    super.setErrorMessage(error);

  }

  @Override
  public String[] getCommandArgs() {
    return super.getCommandArgs();
  }

  @Override
  public Properties getEnvironment() {
    System.out.println("DockerCommandLauncher.getEnvironment.");
    super.getEnvironment().list(System.out);
    
    return super.getEnvironment();
  }

  @Override
  public String getCommandLine() {
    return super.getCommandLine();
  }

  @Override
  public Process execute(IPath cmd, String[] args, String[] env, IPath cwd, IProgressMonitor mon) throws CoreException {

    SubMonitor subMonitor = SubMonitor.convert(mon, 100);

    // if the cwd in the container doesn't exist, make it.
    IPath dockerCwd = DockerManager.changeLocalWorkspaceToWorkspaceContainer(getProject(), cwd);
    mkdirInContainer(dockerCwd, env, subMonitor.split(20));

    // Transform paths from host paths to container paths!
    cmd = DockerManager.changeLocalWorkspaceToWorkspaceContainer(getProject(), cmd);
    for (int i = args.length - 1; i >= 0; i--) {
      args[i] = DockerManager.changeLocalWorkspaceToWorkspaceContainer(getProject(), args[i]);
    }
    return executeInContainer(cmd, args, env, dockerCwd, subMonitor.split(80));
  }

  protected Process executeInContainer(IPath cmd, String[] args, String[] env, IPath dockerCwd, IProgressMonitor mon)
    throws CoreException {

    // TODO Test this in a new workspace, e.g. no containers exist yet!
    String ContainerId = DockerManager.getInstance().getContainerId(getProject(), true);

    // Prepend "docker exec -i $CONTAINER_ID"
    String[] dockerArgs = new String[args.length + 6];
    IPath dockerCmd = new Path(DOCKER_COMMAND);
    dockerArgs[0] = "exec";
    dockerArgs[1] = "-i";
    dockerArgs[2] = "-w";
    dockerArgs[3] = dockerCwd.toString(); // TODO: test on win host
    dockerArgs[4] = ContainerId;
    dockerArgs[5] = cmd.toString(); // TODO: test on win host

    System.arraycopy(args, 0, dockerArgs, 6, args.length);

    System.out.println("DockerCommandLauncher.executeInContainer");
    System.out.println("  cmd  = " + dockerCmd);
    System.out.println("  args = \"" + String.join("\", \"", dockerArgs) + '"');
    System.out.println("  cwd = \"" + dockerCwd + '"');

    return super.execute(dockerCmd, dockerArgs, env, ROOT_DIR, mon);
  }

  protected MultiStatus mkdirInContainer(IPath path, String[] env, IProgressMonitor monitor) throws CoreException {
    
    SubMonitor subMonitor = SubMonitor.convert(monitor, "Creating " + path.toString() + "directory", 100);

    // docker exec -i -w "/" $CONTAINER_ID "mkdir -p path/to/dir"
    String[] args = new String[2];
    args[0] = "-c";
    args[1] = "mkdir -p " + path.toString();
    Process proc = executeInContainer(BASH, args, env, ROOT_DIR, subMonitor);
    String errMsg;
    if (proc != null) {
      try {
        // Close the input of the process since we will never write to it
        proc.getOutputStream().close();
      } catch (IOException e) {
      }
      int state = super.waitAndRead(System.out, System.err, subMonitor);
      if (state == ICommandLauncher.COMMAND_CANCELED) {
        throw new OperationCanceledException(super.getErrorMessage());
      }

      // check exit status
      final int exitValue = proc.exitValue();
      if (exitValue == 0) {
        // success
        return new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, null, null);
      } else {
        // errors...
        errMsg =
          String.format("%1$s exited with status %2$d. See CDT global build console for details.",
            String.join(" ", args), exitValue);
        return new MultiStatus(Activator.PLUGIN_ID, IStatus.ERROR, errMsg, null);
      }
    } else {
      // process start failed
      errMsg = super.getErrorMessage();

      return new MultiStatus(Activator.PLUGIN_ID, IStatus.ERROR, errMsg, null);
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public int waitAndRead(OutputStream out, OutputStream err) {
    System.out.println("DockerCommandLauncher.waitAndRead(out, err)");
    return super.waitAndRead(out, err);
  }

  @Override
  public int waitAndRead(OutputStream output, OutputStream err, IProgressMonitor monitor) {
    System.out.println("DockerCommandLauncher.waitAndRead(out, err, monitor)");
    return super.waitAndRead(output, err, monitor);
  }

}
