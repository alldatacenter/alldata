/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.utils.hdfs;

import com.google.common.base.Strings;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.FsStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.Trash;
import org.apache.hadoop.fs.TrashPolicy;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.protocol.HdfsFileStatus;
import org.apache.hadoop.security.UserGroupInformation;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Hdfs Business Delegate
 */
public class HdfsApi {
  private final static Logger LOG =
      LoggerFactory.getLogger(HdfsApi.class);

  public static String KeyIsErasureCoded = "isErasureCoded";
  public static String KeyIsEncrypted = "isEncrypted";
  public static String KeyErasureCodingPolicyName = "erasureCodingPolicyName";
  private final Configuration conf;
  private Map<String, String> authParams;

  private FileSystem fs;
  private UserGroupInformation ugi;

  /**
   * Constructor
   *
   * @param configurationBuilder hdfs configuration builder
   * @throws IOException
   * @throws InterruptedException
   */
  public HdfsApi(ConfigurationBuilder configurationBuilder, String username) throws IOException,
      InterruptedException, HdfsApiException {
    this.authParams = configurationBuilder.buildAuthenticationConfig();
    conf = configurationBuilder.buildConfig();
    UserGroupInformation.setConfiguration(conf);
    ugi = UserGroupInformation.createProxyUser(username, getProxyUser());

    fs = execute(new PrivilegedExceptionAction<FileSystem>() {
      public FileSystem run() throws IOException {
        return FileSystem.get(conf);
      }
    });
  }

  /**
   * for testing
   * @throws IOException
   * @throws InterruptedException
   * @throws HdfsApiException
   */
  HdfsApi(Configuration configuration, FileSystem fs, UserGroupInformation ugi) throws IOException,
      InterruptedException, HdfsApiException {
    if(null != configuration){
      conf = configuration;
    }else {
      conf = new Configuration();
    }

    UserGroupInformation.setConfiguration(conf);
    if(null != ugi){
      this.ugi = ugi;
    }else {
      this.ugi = UserGroupInformation.getCurrentUser();
    }

    if(null != fs){
      this.fs = fs;
    }else {
      this.fs = execute(new PrivilegedExceptionAction<FileSystem>() {
        public FileSystem run() throws IOException {
          return FileSystem.get(conf);
        }
      });
    }
  }

  private UserGroupInformation getProxyUser() throws IOException {
    UserGroupInformation proxyuser;
    if (authParams.containsKey("proxyuser")) {
      proxyuser = UserGroupInformation.createRemoteUser(authParams.get("proxyuser"));
    } else {
      proxyuser = UserGroupInformation.getCurrentUser();
    }

    proxyuser.setAuthenticationMethod(getAuthenticationMethod());
    return proxyuser;
  }

  private UserGroupInformation.AuthenticationMethod getAuthenticationMethod() {
    UserGroupInformation.AuthenticationMethod authMethod;
    if (authParams.containsKey("auth")) {
      String authName = authParams.get("auth");
      authMethod = UserGroupInformation.AuthenticationMethod.valueOf(authName.toUpperCase());
    } else {
      authMethod = UserGroupInformation.AuthenticationMethod.SIMPLE;
    }
    return authMethod;
  }

  /**
   * List dir operation
   *
   * @param path path
   * @return array of FileStatus objects
   * @throws FileNotFoundException
   * @throws IOException
   * @throws InterruptedException
   */
  public FileStatus[] listdir(final String path) throws FileNotFoundException,
      IOException, InterruptedException {
    return execute(new PrivilegedExceptionAction<FileStatus[]>() {
      public FileStatus[] run() throws FileNotFoundException, Exception {
        return fs.listStatus(new Path(path));
      }
    });
  }

  /**
   *
   * @param path : list files and dirs in this path
   * @param nameFilter : if not empty or null, then file names that contain this are only sent.
   * @param maxAllowedSize : maximum number of files sent in output. -1 means infinite.
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   * @throws InterruptedException
   */
  public DirStatus listdir(final String path, final String nameFilter, int maxAllowedSize) throws FileNotFoundException,
      IOException, InterruptedException {
    FileStatus[] fileStatuses = this.listdir(path);
    return filterAndTruncateDirStatus(nameFilter, maxAllowedSize, fileStatuses);
  }

  public DirStatus filterAndTruncateDirStatus(String nameFilter, int maxAllowedSize, FileStatus[] fileStatuses) {
    if(null == fileStatuses){
      return new DirStatus(null, new DirListInfo(0, false, 0, nameFilter));
    }

    int originalSize = fileStatuses.length;
    boolean truncated = false;

    if (!Strings.isNullOrEmpty(nameFilter)) {
      List<FileStatus> filteredList = new LinkedList<>();
      for(FileStatus fileStatus : fileStatuses){
        if(maxAllowedSize >=0 && maxAllowedSize <= filteredList.size()){
          truncated = true;
          break;
        }
        if(fileStatus.getPath().getName().contains(nameFilter)){
          filteredList.add(fileStatus);
        }
      }
      fileStatuses = filteredList.toArray(new FileStatus[0]);
    }

    if(maxAllowedSize >=0 && fileStatuses.length > maxAllowedSize) { // in cases where name filter loop is not executed.
      truncated = true;
      fileStatuses = Arrays.copyOf(fileStatuses, maxAllowedSize);
    }

    int finalSize = fileStatuses.length;

    return new DirStatus(fileStatuses, new DirListInfo(originalSize, truncated, finalSize, nameFilter));
  }

  /**
   * Get file status
   *
   * @param path path
   * @return file status
   * @throws IOException
   * @throws FileNotFoundException
   * @throws InterruptedException
   */
  public FileStatus getFileStatus(final String path) throws IOException,
      FileNotFoundException, InterruptedException {
    return execute(new PrivilegedExceptionAction<FileStatus>() {
      public FileStatus run() throws FileNotFoundException, IOException {
        return fs.getFileStatus(new Path(path));
      }
    });
  }

  /**
   * Make directory
   *
   * @param path path
   * @return success
   * @throws IOException
   * @throws InterruptedException
   */
  public boolean mkdir(final String path) throws IOException,
      InterruptedException {
    return execute(new PrivilegedExceptionAction<Boolean>() {
      public Boolean run() throws Exception {
        return fs.mkdirs(new Path(path));
      }
    });
  }

  /**
   * Rename
   *
   * @param src source path
   * @param dst destination path
   * @return success
   * @throws IOException
   * @throws InterruptedException
   */
  public boolean rename(final String src, final String dst) throws IOException,
      InterruptedException {
    return execute(new PrivilegedExceptionAction<Boolean>() {
      public Boolean run() throws Exception {
        return fs.rename(new Path(src), new Path(dst));
      }
    });
  }

  /**
   * Check is trash enabled
   *
   * @return true if trash is enabled
   * @throws Exception
   */
  public boolean trashEnabled() throws Exception {
    return execute(new PrivilegedExceptionAction<Boolean>() {
      public Boolean run() throws IOException {
        Trash tr = new Trash(fs, conf);
        return tr.isEnabled();
      }
    });
  }

  /**
   * Home directory
   *
   * @return home directory
   * @throws Exception
   */
  public Path getHomeDir() throws Exception {
    return execute(new PrivilegedExceptionAction<Path>() {
      public Path run() throws IOException {
        return fs.getHomeDirectory();
      }
    });
  }

  /**
   * Hdfs Status
   *
   * @return home directory
   * @throws Exception
   */
  public synchronized FsStatus getStatus() throws Exception {
    return execute(new PrivilegedExceptionAction<FsStatus>() {
      public FsStatus run() throws IOException {
        return fs.getStatus();
      }
    });
  }

  /**
   * Trash directory
   *
   * @return trash directory
   * @throws Exception
   */
  public Path getTrashDir() throws Exception {
    return execute(new PrivilegedExceptionAction<Path>() {
      public Path run() throws IOException {
        TrashPolicy trashPolicy = TrashPolicy.getInstance(conf, fs,
            fs.getHomeDirectory());
        return trashPolicy.getCurrentTrashDir().getParent();
      }
    });
  }

  /**
   * Trash directory path.
   *
   * @return trash directory path
   * @throws Exception
   */
  public String getTrashDirPath() throws Exception {
    Path trashDir = getTrashDir();

    return trashDir.toUri().getRawPath();
  }

  /**
   * Trash directory path.
   *
   * @param filePath the path to the file
   * @return trash directory path for the file
   * @throws Exception
   */
  public String getTrashDirPath(String filePath) throws Exception {
    String trashDirPath = getTrashDirPath();

    Path path = new Path(filePath);
    trashDirPath = trashDirPath + "/" + path.getName();

    return trashDirPath;
  }

  /**
   * Empty trash
   *
   * @return
   * @throws Exception
   */
  public Void emptyTrash() throws Exception {
    return execute(new PrivilegedExceptionAction<Void>() {
      public Void run() throws IOException {
        Trash tr = new Trash(fs, conf);
        tr.expunge();
        return null;
      }
    });
  }

  /**
   * Move to trash
   *
   * @param path path
   * @return success
   * @throws IOException
   * @throws InterruptedException
   */
  public boolean moveToTrash(final String path) throws IOException,
      InterruptedException {
    return execute(new PrivilegedExceptionAction<Boolean>() {
      public Boolean run() throws Exception {
        return Trash.moveToAppropriateTrash(fs, new Path(path), conf);
      }
    });
  }

  /**
   * Delete
   *
   * @param path      path
   * @param recursive delete recursive
   * @return success
   * @throws IOException
   * @throws InterruptedException
   */
  public boolean delete(final String path, final boolean recursive)
      throws IOException, InterruptedException {
    return execute(new PrivilegedExceptionAction<Boolean>() {
      public Boolean run() throws Exception {
        return fs.delete(new Path(path), recursive);
      }
    });
  }

  /**
   * Create file
   *
   * @param path      path
   * @param overwrite overwrite existent file
   * @return output stream
   * @throws IOException
   * @throws InterruptedException
   */
  public FSDataOutputStream create(final String path, final boolean overwrite)
      throws IOException, InterruptedException {
    return execute(new PrivilegedExceptionAction<FSDataOutputStream>() {
      public FSDataOutputStream run() throws Exception {
        return fs.create(new Path(path), overwrite);
      }
    });
  }

  /**
   * Open file
   *
   * @param path path
   * @return input stream
   * @throws IOException
   * @throws InterruptedException
   */
  public FSDataInputStream open(final String path) throws IOException,
      InterruptedException {
    return execute(new PrivilegedExceptionAction<FSDataInputStream>() {
      public FSDataInputStream run() throws Exception {
        return fs.open(new Path(path));
      }
    });
  }

  /**
   * Change permissions
   *
   * @param path        path
   * @param permissions permissions in format rwxrwxrwx
   * @throws IOException
   * @throws InterruptedException
   */
  public boolean chmod(final String path, final String permissions) throws IOException,
      InterruptedException {
    return execute(new PrivilegedExceptionAction<Boolean>() {
      public Boolean run() throws Exception {
        try {
          fs.setPermission(new Path(path), FsPermission.valueOf(permissions));
        } catch (Exception ex) {
          return false;
        }
        return true;
      }
    });
  }

  /**
   * Copy file
   *
   * @param src  source path
   * @param dest destination path
   * @throws java.io.IOException
   * @throws InterruptedException
   */
  public void copy(final String src, final String dest) throws IOException, InterruptedException, HdfsApiException {
    boolean result = execute(new PrivilegedExceptionAction<Boolean>() {
      public Boolean run() throws Exception {
        return FileUtil.copy(fs, new Path(src), fs, new Path(dest), false, conf);
      }
    });

    if (!result) {
      throw new HdfsApiException("HDFS010 Can't copy source file from \" + src + \" to \" + dest");
    }
  }

  public boolean exists(final String newFilePath) throws IOException, InterruptedException {
    return execute(new PrivilegedExceptionAction<Boolean>() {
      public Boolean run() throws Exception {
        return fs.exists(new Path(newFilePath));
      }
    });
  }

  /**
   * Executes action on HDFS using doAs
   *
   * @param action strategy object
   * @param <T>    result type
   * @return result of operation
   * @throws IOException
   * @throws InterruptedException
   */
  public <T> T execute(PrivilegedExceptionAction<T> action) throws IOException, InterruptedException {
    return this.execute(action, false);
  }


  /**
   * Executes action on HDFS using doAs
   * @param action strategy object
   * @param <T> result type
   * @return result of operation
   * @throws IOException
   * @throws InterruptedException
   */
  public <T> T execute(PrivilegedExceptionAction<T> action, boolean alwaysRetry)
      throws IOException, InterruptedException {
    T result = null;

    // Retry strategy applied here due to HDFS-1058. HDFS can throw random
    // IOException about retrieving block from DN if concurrent read/write
    // on specific file is performed (see details on HDFS-1058).
    int tryNumber = 0;
    boolean succeeded = false;
    do {
      tryNumber += 1;
      try {
        result = ugi.doAs(action);
        succeeded = true;
      } catch (IOException ex) {
        if (!Strings.isNullOrEmpty(ex.getMessage()) && !ex.getMessage().contains("Cannot obtain block length for")) {
          throw ex;
        }
        if (tryNumber >= 3) {
          throw ex;
        }
        LOG.info("HDFS threw 'IOException: Cannot obtain block length' exception. " +
            "Retrying... Try #" + (tryNumber + 1));
        LOG.error("Retrying: " + ex.getMessage(),ex);
        Thread.sleep(1000);  //retry after 1 second
      }
    } while (!succeeded);
    return result;
  }

  /**
   * Converts a Hadoop permission into a Unix permission symbolic representation
   * (i.e. -rwxr--r--) or default if the permission is NULL.
   *
   * @param p Hadoop permission.
   * @return the Unix permission symbolic representation or default if the
   * permission is NULL.
   */
  private static String permissionToString(FsPermission p) {
    return (p == null) ? "default" : "-" + p.getUserAction().SYMBOL
        + p.getGroupAction().SYMBOL + p.getOtherAction().SYMBOL;
  }

  /**
   * Converts a Hadoop <code>FileStatus</code> object into a JSON array object.
   * It replaces the <code>SCHEME://HOST:PORT</code> of the path with the
   * specified URL.
   * <p/>
   *
   * @param status Hadoop file status.
   * @return The JSON representation of the file status.
   */
  public Map<String, Object> fileStatusToJSON(FileStatus status) {
    Map<String, Object> json = new LinkedHashMap<String, Object>();
    json.put("path", Path.getPathWithoutSchemeAndAuthority(status.getPath())
        .toString());
    json.put("isDirectory", status.isDirectory());
    json.put("len", status.getLen());
    json.put("owner", status.getOwner());
    json.put("group", status.getGroup());
    json.put("permission", permissionToString(status.getPermission()));
    json.put("accessTime", status.getAccessTime());
    json.put("modificationTime", status.getModificationTime());
    json.put("blockSize", status.getBlockSize());
    json.put("replication", status.getReplication());
    json.put("readAccess", checkAccessPermissions(status, FsAction.READ, ugi));
    json.put("writeAccess", checkAccessPermissions(status, FsAction.WRITE, ugi));
    json.put("executeAccess", checkAccessPermissions(status, FsAction.EXECUTE, ugi));
    json.put(KeyIsErasureCoded, status.isErasureCoded());
    json.put(KeyIsEncrypted, status.isEncrypted());

    if( status instanceof HdfsFileStatus){
      HdfsFileStatus hdfsFileStatus = (HdfsFileStatus) status;
      if(null != hdfsFileStatus.getErasureCodingPolicy()) {
        json.put(KeyErasureCodingPolicyName, hdfsFileStatus.getErasureCodingPolicy().getName());
      }
    }
    return json;
  }

  /**
   * Converts a Hadoop <code>FileStatus</code> array into a JSON array object.
   * It replaces the <code>SCHEME://HOST:PORT</code> of the path with the
   * specified URL.
   * <p/>
   *
   * @param status Hadoop file status array.
   * @return The JSON representation of the file status array.
   */
  @SuppressWarnings("unchecked")
  public JSONArray fileStatusToJSON(FileStatus[] status) {
    JSONArray json = new JSONArray();
    if (status != null) {
      for (FileStatus s : status) {
        json.add(fileStatusToJSON(s));
      }
    }
    return json;
  }

  public static boolean checkAccessPermissions(FileStatus stat, FsAction mode, UserGroupInformation ugi) {
    FsPermission perm = stat.getPermission();
    String user = ugi.getShortUserName();
    List<String> groups = Arrays.asList(ugi.getGroupNames());
    if (user.equals(stat.getOwner())) {
      if (perm.getUserAction().implies(mode)) {
        return true;
      }
    } else if (groups.contains(stat.getGroup())) {
      if (perm.getGroupAction().implies(mode)) {
        return true;
      }
    } else {
      if (perm.getOtherAction().implies(mode)) {
        return true;
      }
    }
    return false;
  }
}
