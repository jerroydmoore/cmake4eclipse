package jnmoore.cmake;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

public class DockerManager {
	/** The singleton instance of the docker manager **/
	private static DockerManager instance = null;
	  
    /**
     * @return the singleton instance of the docker manager.
     * @throws CoreException if pinging the docker client failed
     */
	public static DockerManager getInstance() {
		if (instance == null) {
		    instance = new DockerManager();
		  }
		  return instance;
	}
	
    /**
     * Return a path where the local workspace has been replaced by the remote
     * container workspace path.
     *
     * @param the project for which the path should be replaced
     * @param path the path to replace the local workspace location
     * @return the path with replaced local workspace location by the container
     *         workspace location, or null if the provided path is null.
     * @throws CoreException 
     */
	public static IPath changeLocalWorkspaceToWorkspaceContainer(IProject project, IPath path) throws CoreException {
	    String p = changeLocalWorkspaceToWorkspaceContainer(project, path.toString());
	    return new Path(p);
	}

	/**
	 * Return a path where the local workspace has been replaced by the remote
	 * container workspace path.
	 *
	 * @param the project for which the path should be replaced
	 * @param path the path to replace the local workspace location
	 * @return the path with replaced local workspace location by the container
	 *         workspace location, or null if the provided path is null.
	 */
	public static String changeLocalWorkspaceToWorkspaceContainer(
	  IProject project, String path) {
	  return null;
	}

	/**
	 * Copy file or directory from remote to local destination.
	 *
	 * @param project the project
	 * @param source the remote source
	 * @param destination the local destination
	 * @param isDirectory specifies if the destination is a directory
	 * @throws CoreException if the OS could not be retrieved or
	 *         the copying process failed
	 * @throws RuntimeException if the project, the source or the destination is
	 *         null
	 */
	public void copyFromRemote(IProject project, String source, IPath destination, boolean b) {
		// TODO Auto-generated method stub
		
	}
	
    /**
     * Copy file or directory from remote to local destination.
     *
     * @param project the project
     * @param source the remote source
     * @param destination the local destination
     * @param isDirectory specifies if the destination is a directory
     * @throws CoreException if the OS could not be retrieved or
     *         the copying process failed
     * @throws RuntimeException if the project, the source or the destination is
     *         null
     */
	public void copyProjectToRemote(IProject project) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Get the container id from the given project. Optionally, creates the
	 * container
	 *
	 * @param project get the container based on the project
	 * @param create true if the container should be created, otherwise it should
	 *        be false
	 * @return the container id or null if it could not be found or created.
	 */
	public String getContainerId(IProject project, boolean create) {
		// TODO Auto-generated method stub
		return null;
	}

    /**
     * @return the current workspace path, never null.
     */
	public static String getWorkspacePath() {
		// TODO Auto-generated method stub
		return null;
	}

	  /**
	   * Register the project using Linux Container defaults
	   *
	   * Warning: this will create the container if it does not exist.
	   *
	   * @param project The project
	   * @param overwrite If true and the project is already registered, overwrite
	   *        the saved values with the default values. If false and the project
	   *        is already registered, nothing happens.
	   * @throws CoreException when the platformConfiguration could not be saved
	   */
	public void registerProjectUsingDefaults(IProject project, boolean overwrite) {
		// TODO Auto-generated method stub
	}
}
