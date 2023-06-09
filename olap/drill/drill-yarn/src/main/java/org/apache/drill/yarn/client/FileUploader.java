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
package org.apache.drill.yarn.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.drill.yarn.core.DfsFacade;
import org.apache.drill.yarn.core.DoYUtil;
import org.apache.drill.yarn.core.DoyConfigException;
import org.apache.drill.yarn.core.DrillOnYarnConfig;
import org.apache.drill.yarn.core.DfsFacade.DfsFacadeException;
import org.apache.drill.yarn.core.DfsFacade.Localizer;
import org.apache.hadoop.yarn.api.records.LocalResource;

import com.typesafe.config.Config;

/**
 * Performs the file upload portion of the operation by uploading an archive to
 * the target DFS system and directory. Records the uploaded archive so it may
 * be used for localizing Drill in the launch step.
 * <p>
 * Some of the code is a bit of a dance so we can get information early to
 * display in status messages.
 * <p>
 * This class handles x cases:
 * <ol>
 * <li>Non-localized, config in $DRILL_HOME/conf.</li>
 * <li>Non-localized, config in a site directory.</li>
 * <li>Localized, config in $DRILL_HOME.</li>
 * <li>Localized, config in a site directory.</li>
 * </ol>
 * <p>
 * The non-localized case adds complexity, but is very handy when doing
 * development as it avoids the wait for the archives to up- and down-load. The
 * non-localized mode is not advertised to users as it defeats one of the main
 * benefits of YARN.
 * <p>
 * In the localized case, YARN is incomplete; there is no API to inform the AM
 * of the set of localized files, so we pass the information along in
 * environment variables. Also, tar is a bit annoying because it includes the
 * root directory name when unpacking, so that the drill.tar.gz archive unpacks
 * to, say, apache-drill.x.y.z. So, we must pass along the directory name as
 * well.
 * <p>
 * All of this is further complicated by the way YARN needs detailed information
 * to localize resources, and that YARN uses a "key" to identify localized
 * resources, which becomes the directory name in the task's working folder.
 * Thus, Drill becomes, say<br>
 * $PWD/drill/apache-drill.x.y.z/bin, conf, ...<br>
 * YARN provides PWD. The Drillbit launch script needs to know the next two
 * directory names.
 * <p>
 * For efficiency, we omit uploading the Drill archive if one already exists in
 * dfs and is the same size as the one on the client. We always upload the
 * config archive (if needed) because config changes are likely to be one reason
 * that someone (re)starts the Drill cluster.
 */

public abstract class FileUploader {
  protected DrillOnYarnConfig doyConfig;
  protected Config config;
  protected DfsFacade dfs;
  protected boolean dryRun;
  protected boolean verbose;
  protected File localDrillHome;
  protected File localSiteDir;
  protected File localDrillArchivePath;

  public Map<String, LocalResource> resources = new HashMap<>();
  public String drillArchivePath;
  public String siteArchivePath;
  public String remoteDrillHome;
  public String remoteSiteDir;

  public static class NonLocalized extends FileUploader {
    public NonLocalized(boolean dryRun, boolean verbose) {
      super(dryRun, verbose);
    }

    @Override
    public void run() throws ClientException {
      setup();
      prepareDrillHome();
      if (hasSiteDir()) {
        prepareSiteDir();
      }
      if (verbose || dryRun) {
        dump(System.out);
      }
    }

    private void prepareDrillHome() throws ClientException {
      // We need the drill home property. The client can figure out the
      // Drill home, but the AM must be told.

      String drillHomeProp = config.getString(DrillOnYarnConfig.DRILL_HOME);
      if (DoYUtil.isBlank(drillHomeProp)) {
        System.out.println("Warning: non-localized run "
            + DrillOnYarnConfig.DRILL_HOME + " is not set.");
        System.out.println(
            "Assuming remote Drill home is the same as the local location: "
                + localDrillHome.getAbsolutePath());
      }
    }

    private void prepareSiteDir() throws ClientException {
      String siteDirProp = config.getString(DrillOnYarnConfig.SITE_DIR);
      if (DoYUtil.isBlank(siteDirProp)) {
        System.out.println("Warning: non-localized run "
            + DrillOnYarnConfig.SITE_DIR + " is not set.");
        System.out.println(
            "Assuming remote Drill site is the same as the local location: "
                + localSiteDir.getAbsolutePath());
      }
    }
  }

  public static class ReuseFiles extends FileUploader {
    public ReuseFiles(boolean dryRun, boolean verbose) {
      super(dryRun, verbose);
    }

    @Override
    public void run() throws ClientException {
      setup();
      checkDrillArchive();
      if (hasSiteDir()) {
        checkSiteArchive();
      }
      if (verbose || dryRun) {
        dump(System.out);
      }
    }

    /**
     * Upload the Drill archive if desired. Skip the upload if the file already
     * exists in dfs and is the same size as the local file. However using the
     * force option can force an upload even if the sizes match.
     * <p>
     * Prepares the information needed to tell YARN and the AM about the
     * localized archive.
     * <p>
     * Note that the Drill archive is not created by this client; it must
     * already exist on disk. Typically, it is just the archive downloaded from
     * Apache or some other distribution. The uploaded archive retains the name
     * of the archive in the client, which may be useful to check the version of
     * the uploaded code based on the file name.
     *
     * @throws ClientException
     */

    private void checkDrillArchive() throws ClientException {
      // Print the progress message here because doing the connect takes
      // a while and the message makes it look like we're doing something.

      DfsFacade.Localizer localizer = makeDrillLocalizer();
      connectToDfs();
      try {
        if (!localizer.destExists()) {
          throw new ClientException(
              "Drill archive not found in DFS: " + drillArchivePath);
        }
      } catch (IOException e) {
        throw new ClientException(
            "Failed to check existence of " + drillArchivePath, e);
      }
      if (!localDrillArchivePath.exists()) {
        return;
      }
      if (!localizer.filesMatch()) {
        System.out.println(
            "Warning: Drill archive on DFS does not match the local version.");
      }
      defineResources(localizer, DrillOnYarnConfig.DRILL_ARCHIVE_KEY);
    }

    private void checkSiteArchive() throws ClientException {
      // Print the progress message here because doing the connect takes
      // a while and the message makes it look like we're doing something.

      DfsFacade.Localizer localizer = makeSiteLocalizer(null);
      try {
        if (!localizer.destExists()) {
          throw new ClientException(
              "Drill archive not found in DFS: " + drillArchivePath);
        }
      } catch (IOException e) {
        throw new ClientException(
            "Failed to check existence of " + drillArchivePath, e);
      }
      defineResources(localizer, DrillOnYarnConfig.SITE_ARCHIVE_KEY);
    }
  }

  public static class UploadFiles extends FileUploader {
    private boolean force;

    public UploadFiles(boolean force, boolean dryRun, boolean verbose) {
      super(dryRun, verbose);
      this.force = force;
    }

    @Override
    public void run() throws ClientException {
      setup();
      uploadDrillArchive();
      if (hasSiteDir()) {
        uploadSite();
      }
      if (verbose || dryRun) {
        dump(System.out);
      }
    }

    /**
     * Create a temporary archive of the site directory and upload it to DFS. We
     * always upload the site; we never reuse an existing one.
     *
     * @throws ClientException
     */

    private void uploadSite() throws ClientException {
      File siteArchive = createSiteArchive();
      try {
        uploadSiteArchive(siteArchive);
      } finally {
        siteArchive.delete();
      }
    }

    /**
     * Upload the Drill archive if desired. Skip the upload if the file already
     * exists in dfs and is the same size as the local file. However using the
     * force option can force an upload even if the sizes match.
     * <p>
     * Prepares the information needed to tell YARN and the AM about the
     * localized archive.
     * <p>
     * Note that the Drill archive is not created by this client; it must
     * already exist on disk. Typically, it is just the archive downloaded from
     * Apache or some other distribution. The uploaded archive retains the name
     * of the archive in the client, which may be useful to check the version of
     * the uploaded code based on the file name.
     *
     * @throws ClientException
     */

    private void uploadDrillArchive() throws ClientException {
      // Print the progress message here because doing the connect takes
      // a while and the message makes it look like we're doing something.

      connectToDfs();
      DfsFacade.Localizer localizer = makeDrillLocalizer();
      boolean needsUpload = force || !localizer.filesMatch();

      if (needsUpload) {
        // Thoroughly check the Drill archive. Errors with the archive seem a
        // likely source of confusion, so provide detailed error messages for
        // common cases. Don't bother with these checks if no upload is needed.

        if (!localDrillArchivePath.exists()) {
          throw new ClientException(
              "Drill archive not found: " + localDrillArchivePath.getAbsolutePath());
        }
        if (!localDrillArchivePath.canRead()) {
          throw new ClientException(
              "Drill archive is not readable: " + localDrillArchivePath.getAbsolutePath());
        }
        if (localDrillArchivePath.isDirectory()) {
          throw new ClientException(
              "Drill archive cannot be a directory: " + localDrillArchivePath.getAbsolutePath());
        }
      }

      drillArchivePath = localizer.getDestPath();
      if (needsUpload) {
        if (dryRun) {
          System.out.print(
              "Upload " + localDrillArchivePath.getAbsolutePath() + " to " + drillArchivePath);
        } else {
          System.out.print("Uploading " + localDrillArchivePath.getAbsolutePath() + " to "
              + drillArchivePath + " ... ");
          upload(localizer);
        }
      } else {
        System.out.println(
            "Using existing Drill archive in DFS: " + drillArchivePath);
      }

      defineResources(localizer, DrillOnYarnConfig.DRILL_ARCHIVE_KEY);
    }

    /**
     * Run the tar command to archive the site directory into a temporary
     * archive which is then uploaded to DFS using a standardized name. The site
     * directory is always uploaded since configuration is subject to frequent
     * changes.
     *
     * @return
     * @throws ClientException
     */

    private File createSiteArchive() throws ClientException {
      File siteArchiveFile;
      try {
        siteArchiveFile = File.createTempFile("drill-site-", ".tar.gz");
      } catch (IOException e) {
        throw new ClientException("Failed to create site archive temp file", e);
      }
      String cmd[] = new String[] { "tar", "-C", localSiteDir.getAbsolutePath(),
          "-czf", siteArchiveFile.getAbsolutePath(), "." };
      List<String> cmdList = Arrays.asList(cmd);
      String cmdLine = DoYUtil.join(" ", cmdList);
      if (dryRun) {
        System.out.print("Site archive command: ");
        System.out.println(cmdLine);
        return siteArchiveFile;
      }

      ProcessBuilder builder = new ProcessBuilder(cmdList);
      builder.redirectErrorStream(true);
      Process proc;
      try {
        proc = builder.start();
      } catch (IOException e) {
        throw new ClientException("Failed to launch tar process: " + cmdLine,
            e);
      }

      // Should not be much output. But, we have to read it anyway to avoid
      // blocking. We'll use the output if we encounter an error.

      BufferedReader br = new BufferedReader(
          new InputStreamReader(proc.getInputStream()));
      StringBuilder buf = new StringBuilder();
      try {
        String line;
        while ((line = br.readLine()) != null) {
          buf.append(line);
          buf.append("\n");
        }
        br.close();
      } catch (IOException e) {
        throw new ClientException("Failed to read output from tar command", e);
      }
      try {
        proc.waitFor();
      } catch (InterruptedException e) {
        // Won't occur.
      }
      if (proc.exitValue() != 0) {
        String msg = buf.toString().trim();
        throw new ClientException("Tar of site directory failed: " + msg);
      }
      return siteArchiveFile;
    }

    /**
     * Upload the site archive. For debugging, the client provides the option to
     * use existing files, which users should not do in production.
     *
     * @param siteArchive
     * @throws ClientException
     */

    private void uploadSiteArchive(File siteArchive) throws ClientException {
      DfsFacade.Localizer localizer = makeSiteLocalizer(siteArchive);

      if (dryRun) {
        System.out.println("Upload site archive to " + siteArchivePath);
      } else {
        System.out
        .print("Uploading site directory " + localSiteDir.getAbsolutePath() +
               " to " + siteArchivePath + " ... ");
        upload(localizer);
      }
      defineResources(localizer, DrillOnYarnConfig.SITE_ARCHIVE_KEY);
    }
  }

  public FileUploader(boolean dryRun, boolean verbose) {
    doyConfig = DrillOnYarnConfig.instance();
    this.config = doyConfig.getConfig();
    this.dryRun = dryRun;
    this.verbose = verbose;
  }

  public abstract void run() throws ClientException;

  /**
   * Common setup of the Drill and site directories.
   *
   * @throws ClientException
   */

  protected void setup() throws ClientException {

    // Local and remote Drill home locations.

    localDrillHome = doyConfig.getLocalDrillHome();
    try {
      remoteDrillHome = doyConfig.getRemoteDrillHome();
    } catch (DoyConfigException e) {
      throw new ClientException(e);
    }

    // Site directory is optional. Local and remote locations, if provided.
    // Check that the site directory is an existing directory.

    localSiteDir = doyConfig.getLocalSiteDir();
    if (hasSiteDir()) {
      if (!localSiteDir.isDirectory()) {
        throw new ClientException(
            "Drill site dir not a directory: " + localSiteDir);
      }
      remoteSiteDir = doyConfig.getRemoteSiteDir();
    }

    // Disclaimer that this is just a dry run when that option is selected.

    if (dryRun) {
      System.out.println("Dry run only.");
    }
  }

  public boolean hasSiteDir() {
    return localSiteDir != null;
  }

  /**
   * Report whether the user wants to localize (upload) Drill files, or just use
   * files already on the worker nodes.
   *
   * @return
   */

  public boolean isLocalized() {
    return config.getBoolean(DrillOnYarnConfig.LOCALIZE_DRILL);
  }

  protected void connectToDfs() throws ClientException {
    try {
      System.out.print("Connecting to DFS...");
      dfs = new DfsFacade(config);
      dfs.connect();
      System.out.println(" Connected.");
    } catch (DfsFacadeException e) {
      System.out.println("Failed.");
      throw new ClientException("Failed to connect to DFS", e);
    }
  }

  protected Localizer makeDrillLocalizer() throws ClientException {
    String localArchivePath = config
        .getString(DrillOnYarnConfig.DRILL_ARCHIVE_PATH);
    if (DoYUtil.isBlank(localArchivePath)) {
      throw new ClientException("Drill archive path ("
          + DrillOnYarnConfig.DRILL_ARCHIVE_PATH + ") is not set.");
    }

    // Archive is either absolute, or relative to $DRILL_HOME.

    localDrillArchivePath = new File(localArchivePath);
    if (!localDrillArchivePath.isAbsolute()) {
      localDrillArchivePath = new File(
          DrillOnYarnConfig.instance().getLocalDrillHome(), localArchivePath);
    }
    DfsFacade.Localizer localizer = new DfsFacade.Localizer(dfs,
        localDrillArchivePath, "Drill");
    drillArchivePath = localizer.getDestPath();
    return localizer;
  }

  protected Localizer makeSiteLocalizer(File siteArchive) {
    DfsFacade.Localizer localizer = new DfsFacade.Localizer(dfs, siteArchive,
        DrillOnYarnConfig.SITE_ARCHIVE_NAME, "Site");
    siteArchivePath = localizer.getDestPath();
    return localizer;
  }

  protected void upload(Localizer localizer) throws ClientException {
    try {
      localizer.upload();
    } catch (DfsFacadeException e) {
      System.out.println("Failed.");
      throw new ClientException(
          "Failed to upload " + localizer.getLabel() + " archive", e);
    }
    System.out.println("Uploaded.");
  }

  protected void defineResources(Localizer localizer, String keyProp)
      throws ClientException {
    String key = config.getString(keyProp);
    try {
      localizer.defineResources(resources, key);
    } catch (DfsFacadeException e) {
      throw new ClientException(
          "Failed to get DFS status for " + localizer.getLabel() + " archive",
          e);
    }
  }

  protected void dump(PrintStream out) {
    out.print("Localized: ");
    out.println((isLocalized()) ? "Yes" : "No");
    out.print("Has Site Dir: ");
    out.println((hasSiteDir()) ? "Yes" : "No");
    out.print("Local Drill home: ");
    out.println(localDrillHome.getAbsolutePath());
    out.print("Remote Drill home: ");
    out.println(remoteDrillHome);
    if (hasSiteDir()) {
      out.print("Local Site dir: ");
      out.println(localSiteDir.getAbsolutePath());
      out.print("Remote Site dir: ");
      out.println(remoteSiteDir);
    }
    if (isLocalized()) {
      out.print("Drill archive DFS path: ");
      out.println(drillArchivePath);
      if (hasSiteDir()) {
        out.print("Site archive DFS path: ");
        out.println(siteArchivePath);
      }
    }
  }
}