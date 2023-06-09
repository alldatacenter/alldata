/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.yarn.core;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.UnsupportedFileSystemException;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.api.records.URL;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.util.ConverterUtils;

import com.typesafe.config.Config;

/**
 * Facade to the distributed file system (DFS) system that implements
 * Drill-on-YARN related operations. Some operations are used by both the client
 * and AM applications.
 */

public class DfsFacade {
  public static class DfsFacadeException extends Exception {
    private static final long serialVersionUID = 1L;

    public DfsFacadeException(String msg) {
      super(msg);
    }

    public DfsFacadeException(String msg, Exception e) {
      super(msg, e);
    }
  }

  private FileSystem fs;
  private Configuration yarnConf;
  private Config config;
  private boolean localize;

  public DfsFacade(Config config) {
    this.config = config;
    localize = config.getBoolean(DrillOnYarnConfig.LOCALIZE_DRILL);
  }

  public boolean isLocalized() {
    return localize;
  }

  public void connect() throws DfsFacadeException {
    loadYarnConfig();
    String dfsConnection = config.getString(DrillOnYarnConfig.DFS_CONNECTION);
    try {
      if (DoYUtil.isBlank(dfsConnection)) {
        fs = FileSystem.get(yarnConf);
      } else {
        URI uri;
        try {
          uri = new URI(dfsConnection);
        } catch (URISyntaxException e) {
          throw new DfsFacadeException(
              "Illformed DFS connection: " + dfsConnection, e);
        }
        fs = FileSystem.get(uri, yarnConf);
      }
    } catch (IOException e) {
      throw new DfsFacadeException("Failed to create the DFS", e);
    }
  }

  /**
   * Lazy loading of YARN configuration since it takes a long time to load.
   * (YARN provides no caching, sadly.)
   */

  private void loadYarnConfig() {
    if (yarnConf == null) {
      yarnConf = new YarnConfiguration();
      // On some distributions, lack of proper configuration causes
      // DFS to default to the local file system. So, a local file
      // system generally means that the config is wrong, or running
      // the wrong build of Drill for the user's environment.
      URI fsUri = FileSystem.getDefaultUri( yarnConf );
      if(fsUri.toString().startsWith("file:/")) {
        System.err.println("Warning: Default DFS URI is for a local file system: " + fsUri);
      }
    }
  }

  public static class Localizer {
    private final DfsFacade dfs;
    protected File localArchivePath;
    protected Path dfsArchivePath;
    FileStatus fileStatus;
    private String label;

    /**
     * Resources to be localized (downloaded) to each AM or drillbit node.
     */

    public Localizer(DfsFacade dfs, File archivePath, String label) {
      this(dfs, archivePath, dfs.getUploadPath(archivePath), label);
    }

    public Localizer(DfsFacade dfs, File archivePath, String destName,
        String label) {
      this(dfs, archivePath, dfs.getUploadPath(destName), label);
    }

    public Localizer(DfsFacade dfs, String destPath) {
      this( dfs, null, new Path(destPath), null );
    }

    public Localizer(DfsFacade dfs, File archivePath, Path destPath, String label) {
      this.dfs = dfs;
      dfsArchivePath = destPath;
      this.label = label;
      localArchivePath = archivePath;
    }

    public String getBaseName() {
      return localArchivePath.getName();
    }

    public String getDestPath() {
      return dfsArchivePath.toString();
    }

    public void upload() throws DfsFacadeException {
      dfs.uploadArchive(localArchivePath, dfsArchivePath, label);
      fileStatus = null;
    }

    /**
     * The client may check file status multiple times. Cache it here so we
     * only retrieve the status once. Cache it here so that the client
     * doen't have to do the caching.
     *
     * @return file status
     * @throws DfsFacadeException
     */

    private FileStatus getStatus() throws DfsFacadeException {
      if (fileStatus == null) {
        fileStatus = dfs.getFileStatus(dfsArchivePath);
      }
      return fileStatus;
    }

    public void defineResources(Map<String, LocalResource> resources,
        String key) throws DfsFacadeException {
      // Put the application archive, visible to only the application.
      // Because it is an archive, it will be expanded by YARN prior to launch
      // of the AM.

      LocalResource drillResource = dfs.makeResource(dfsArchivePath,
          getStatus(), LocalResourceType.ARCHIVE,
          LocalResourceVisibility.APPLICATION);
      resources.put(key, drillResource);
    }

    public boolean filesMatch() {
      FileStatus status;
      try {
        status = getStatus();
      } catch (DfsFacadeException e) {

        // An exception is DFS's way of tell us the file does
        // not exist.

        return false;
      }
      return status.getLen() == localArchivePath.length();
    }

    public String getLabel() {
      return label;
    }

    public boolean destExists() throws IOException {
      return dfs.exists(dfsArchivePath);
    }
  }

  public boolean exists(Path path) throws IOException {
    return fs.exists(path);
  }

  public Path getUploadPath(File localArchiveFile) {
    return getUploadPath(localArchiveFile.getName());
  }

  public Path getUploadPath(String baseName) {
    String dfsDirStr = config.getString(DrillOnYarnConfig.DFS_APP_DIR);

    Path appDir;
    if (dfsDirStr.startsWith("/")) {
      appDir = new Path(dfsDirStr);
    } else {
      Path home = fs.getHomeDirectory();
      appDir = new Path(home, dfsDirStr);
    }
    return new Path(appDir, baseName);
  }

  public void uploadArchive(File localArchiveFile, Path destPath, String label)
      throws DfsFacadeException {
    // Create the application upload directory if it does not yet exist.

    String dfsDirStr = config.getString(DrillOnYarnConfig.DFS_APP_DIR);
    Path appDir = new Path(dfsDirStr);
    try {
      // If the directory does not exist, create it, giving this user
      // (only) read and write access.

      if (!fs.isDirectory(appDir)) {
        fs.mkdirs(appDir, new FsPermission(FsAction.READ_WRITE, FsAction.NONE, FsAction.NONE));
      }
    } catch (IOException e) {
      throw new DfsFacadeException(
          "Failed to create DFS directory: " + dfsDirStr, e);
    }

    // The file must be an archive type so YARN knows to extract its contents.

    String baseName = localArchiveFile.getName();
    if (DrillOnYarnConfig.findSuffix(baseName) == null) {
      throw new DfsFacadeException(
          label + " archive must be .tar.gz, .tgz or .zip: " + baseName);
    }

    Path srcPath = new Path(localArchiveFile.getAbsolutePath());

    // Do the upload, replacing the old archive.

    try {
      // TODO: Specify file permissions and owner.

      fs.copyFromLocalFile(false, true, srcPath, destPath);
    } catch (IOException e) {
      throw new DfsFacadeException(
          "Failed to upload " + label + " archive to DFS: "
              + localArchiveFile.getAbsolutePath() + " --> " + destPath,
          e);
    }
  }

  private FileStatus getFileStatus(Path dfsPath) throws DfsFacadeException {
    try {
      return fs.getFileStatus(dfsPath);
    } catch (IOException e) {
      throw new DfsFacadeException(
          "Failed to get DFS status for file: " + dfsPath, e);
    }
  }

  /**
   * Create a local resource definition for YARN. A local resource is one that
   * must be localized onto the remote node prior to running a command on that
   * node.
   * <p>
   * YARN uses the size and timestamp to check if the file has changed
   * on HDFS and to check if YARN can use an existing copy, if any.
   * <p>
   * Resources are made public.
   *
   * @param dfsPath
   *          the path (relative or absolute) to the file on the configured file
   *          system (usually HDFS).
   * @param dfsFileStatus the file status of the configured file system
   * @param type local resource type (archive, file, or pattern)
   * @param visibility local resource visibility (public, private, or application)
   * @return a YARN local resource records that contains information about path,
   *         size, type, resource and so on that YARN requires.
   * @throws IOException
   *           if the resource does not exist on the configured file system
   */

  public LocalResource makeResource(Path dfsPath, FileStatus dfsFileStatus,
      LocalResourceType type, LocalResourceVisibility visibility)
      throws DfsFacadeException {
    URL destUrl;
    try {
      destUrl = ConverterUtils.getYarnUrlFromPath(
          FileContext.getFileContext().makeQualified(dfsPath));
    } catch (UnsupportedFileSystemException e) {
      throw new DfsFacadeException(
          "Unable to convert dfs file to a URL: " + dfsPath.toString(), e);
    }
    LocalResource resource = LocalResource.newInstance(destUrl, type,
        visibility, dfsFileStatus.getLen(),
        dfsFileStatus.getModificationTime());
    return resource;
  }

  public void removeDrillFile(String fileName) throws DfsFacadeException {
    Path destPath = getUploadPath(fileName);
    try {
      fs.delete(destPath, false);
    } catch (IOException e) {
      throw new DfsFacadeException(
          "Failed to delete file: " + destPath.toString(), e);
    }

    // Remove the Drill directory, but only if it is now empty.

    Path dir = destPath.getParent();
    try {
      RemoteIterator<FileStatus> iter = fs.listStatusIterator(dir);
      if (!iter.hasNext()) {
        fs.delete(dir, false);
      }
    } catch (IOException e) {
      throw new DfsFacadeException(
          "Failed to delete directory: " + dir.toString(), e);
    }
  }
}
