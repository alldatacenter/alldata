/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.fast_hdfs_resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.lang.System;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.fs.FileSystem;

/**
 * Used to:
 *   1) copy files/directories from localFS to hadoopFs
 *   2) create empty
 *   3) download files/directories from hadoopFs to localFS
 * files/directories in hadoopFs
 */
public class Resource {
  private String source;
  private String target;
  private String type;
  private String action;
  private String owner;
  private String group;
  private String mode;
  private String nameservice;
  private boolean recursiveChown;
  private boolean recursiveChmod;
  private boolean changePermissionforParents;
  private boolean manageIfExists;

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getNameservice() {
    return nameservice;
  }

  public void setNameservice(String nameservice) {
    this.nameservice = nameservice;
  }

  public boolean isRecursiveChown() {
    return recursiveChown;
  }

  public void setRecursiveChown(boolean recursiveChown) {
    this.recursiveChown = recursiveChown;
  }

  public boolean isRecursiveChmod() {
    return recursiveChmod;
  }

  public void setRecursiveChmod(boolean recursiveChmod) {
    this.recursiveChmod = recursiveChmod;
  }
  
  public boolean isChangePermissionOnParents() {
    return changePermissionforParents;
  }

  public void setChangePermissionOnParents(boolean changePermissionforParents) {
    this.changePermissionforParents = changePermissionforParents;
  }

  public boolean isManageIfExists() {
    return manageIfExists;
  }

  public void setManageIfExists(boolean manageIfExists) {
    this.manageIfExists = manageIfExists;
  }

  @Override
  public String toString() {
    return "Resource [source=" + source + ", target=" + target + ", type="
        + type + ", action=" + action + ", owner=" + owner + ", group=" + group
        + ", mode=" + mode + ", recursiveChown=" + recursiveChown
        + ", recursiveChmod=" + recursiveChmod
        + ", changePermissionforParents=" + changePermissionforParents
        + ", manageIfExists=" + manageIfExists + "]";
  }

  /*
   * Check if parameters are correctly set
   */
  public static void checkResourceParameters(Resource resource,
      FileSystem dfs) throws IllegalArgumentException, IOException {

    ArrayList<String> actionsAvailable = new ArrayList<String>();
    actionsAvailable.add("create");
    actionsAvailable.add("delete");
    actionsAvailable.add("download");
    ArrayList<String> typesAvailable = new ArrayList<String>();
    typesAvailable.add("file");
    typesAvailable.add("directory");

    if (resource.getAction() == null || !actionsAvailable.contains(resource.getAction())) {
      throw new IllegalArgumentException("Action is not supported.");
    }

    String dfsPath = resource.getTarget();
    String localPath = resource.getSource();
    if (resource.getAction().equals("download")) {
      dfsPath = resource.getSource();
      localPath = resource.getTarget();
    }

    if (dfsPath == null) {
      throw new IllegalArgumentException("Path to resource in HadoopFs must be filled.");
    }

    if (resource.getType() == null || !typesAvailable.contains(resource.getType())) {
      throw new IllegalArgumentException("Type is not supported.");
    }

    // Check consistency for ("type":"file" == file in hadoop)
    if (dfs.isFile(new Path(dfsPath)) && !"file".equals(resource.getType())) {
      throw new IllegalArgumentException(
          "Cannot create a directory " + dfsPath +
              " because file is present on the given path.");
    }
    // Check consistency for ("type":"directory" == directory in hadoop)
    else if (dfs.isDirectory(new Path(dfsPath)) && !"directory".equals(resource.getType())) {
      throw new IllegalArgumentException(
          "Cannot create a file " + dfsPath +
              " because directory is present on the given path.");
    }

    if(localPath != null) {
      File local = new File(localPath);
      if(local.isFile() && !"file".equals(resource.getType())) {
        throw new IllegalArgumentException(
            "Cannot create a directory " + dfsPath +
                " because source " + localPath + "is a file");
      }
      else if(local.isDirectory() && !"directory".equals(resource.getType())) {
        throw new IllegalArgumentException(
            "Cannot create a file " + dfsPath +
                " because source " + localPath + "is a directory");
      }
    }
  }

  /*
   * Create/copy resource - {type}
   */
  public static void createResource(Resource resource,
      FileSystem dfs, Path pathHadoop) throws IOException {

    boolean isCreate = (resource.getSource() == null) ? true : false;

    if (isCreate && resource.getType().equals("directory")) {
      dfs.mkdirs(pathHadoop); // empty dir(s)
    } else if (isCreate && resource.getType().equals("file")) {
      dfs.createNewFile(pathHadoop); // empty file
    } else {
      if(dfs.exists(pathHadoop) && dfs.getFileStatus(pathHadoop).isDir()) {
        System.out.println("Skipping copy from local, as target " + pathHadoop + " is an existing directory."); // Copy from local to existing directory is not supported by dfs.
      } else {
        dfs.copyFromLocalFile(new Path(resource.getSource()), pathHadoop);
      }
    }
  }

  /*
   * Set permissions on resource - {mode}
   */
  public static void setMode(Resource resource,
      FileSystem dfs, Path pathHadoop) throws IOException {

    if (resource.getMode() != null) {
      FsPermission permission = new FsPermission((short)Integer.parseInt(resource.getMode(), 8));
      dfs.setPermission(pathHadoop, permission);

      // Recursive
      
        // Get the list of sub-directories and files
        HashSet<String> resultSet = new HashSet<String>();
        
        if (resource.isRecursiveChmod())
          resource.fillDirectoryList(dfs, resource.getTarget(), resultSet);
        
        if(resource.isChangePermissionOnParents())
          resource.fillInParentDirectories(dfs, resource.getTarget(), resultSet);

        for (String path : resultSet) {
          dfs.setPermission(new Path(path), permission);
        }

    }
  }

  /*
   * Set owner on resource - {owner}
   */
  public static void setOwner(Resource resource, FileSystem dfs,
      Path pathHadoop) throws IOException {

    if (!(resource.getOwner() == null && resource.getGroup() == null)) {
      dfs.setOwner(pathHadoop, resource.getOwner(), resource.getGroup());

      // Get the list of sub-directories and files
      HashSet<String> resultSet = new HashSet<String>();
      if (resource.isRecursiveChown())
        resource.fillDirectoryList(dfs, resource.getTarget(), resultSet);
      if(resource.isChangePermissionOnParents())
        resource.fillInParentDirectories(dfs, resource.getTarget(), resultSet);

      for (String path : resultSet) {
        dfs.setOwner(new Path(path), resource.getOwner(), resource.getGroup());
      }
    }
  }
  
  public void fillInParentDirectories(FileSystem dfs, String path, HashSet<String> resultSet) throws IOException {
    Path filePath = new Path(path);
      
    while(true) {
      filePath = filePath.getParent();
      
      // if(filePath.isRoot()) {
      if(filePath.getParent() == null) {
        break;
      }
      resultSet.add(filePath.toString());
    }
  }

  /*
   * List all files and sub-directories recursively
   */
  public void fillDirectoryList(FileSystem dfs, String path,
      HashSet<String> resultSet) throws IOException {

    FileStatus[] fileStatus = dfs.listStatus(new Path(path));
    if (fileStatus != null) {
      // Go through all resources in directory
      for (FileStatus fs : fileStatus) {
        String pathToResource = path + "/" + fs.getPath().getName();

        resultSet.add(pathToResource);

        if (fs.isDir()) {
          // recursive
          fillDirectoryList(dfs, pathToResource, resultSet);
        }
      }
    }
  }
}
