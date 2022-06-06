/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.ambari.view;

import java.io.IOException;
import java.io.InputStream;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.hadoop.fs.FileStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowFilesService {
  private final static Logger LOGGER = LoggerFactory
          .getLogger(WorkflowFilesService.class);
  private HDFSFileUtils hdfsFileUtils;
  private String currentDraftVersion="v1";

  public WorkflowFilesService(HDFSFileUtils hdfsFileUtils) {
    super();
    this.hdfsFileUtils = hdfsFileUtils;
  }

  public String createFile(String appPath, String content,
                           boolean overwrite) throws IOException {
    return hdfsFileUtils.writeToFile(appPath, content,
            overwrite);
  }

  public String createAssetFile(String appPath, String content,
                                boolean overwrite) throws IOException {
    return hdfsFileUtils.writeToFile(appPath, content,
            overwrite);
  }

  public InputStream readDraft(String appPath) throws IOException {
    return hdfsFileUtils.read(getWorkflowDraftFileName(appPath,null));
  }

  public InputStream readWorkflowXml(String appPath) throws IOException {
    return hdfsFileUtils.read(appPath);
  }

  public InputStream readAssset(String assetPath) throws IOException {
    return hdfsFileUtils.read(getAssetFileName(assetPath));
  }

  public String getWorkflowDraftFileName(String appPath,JobType jobType) {
    if (appPath.endsWith(Constants.WF_DRAFT_EXTENSION)) {
      return appPath;
    } else if (appPath.endsWith(Constants.WF_EXTENSION)) {
      String folderPath = appPath.substring(0, appPath.lastIndexOf(Constants.WF_EXTENSION));
      return folderPath + Constants.WF_DRAFT_EXTENSION;
    }
    if (jobType==null){
      throw new RuntimeException("Could not determine jobType(Workflow/Coordniator/Bundle");
    }
    if (appPath.endsWith("/")) {
      return appPath + getDefaultDraftFileName(jobType);
    } else {
      return appPath + "/" + getDefaultDraftFileName(jobType);
    }
  }

  public String getWorkflowFileName(String appPath, JobType jobType) {
    if (appPath.endsWith(Constants.WF_EXTENSION)) {
      return appPath;
    } else if (appPath.endsWith(Constants.WF_DRAFT_EXTENSION)) {
      String folderPath = appPath.substring(0, appPath.lastIndexOf(Constants.WF_DRAFT_EXTENSION));
      return folderPath + Constants.WF_EXTENSION;
    } else if (appPath.endsWith("/")) {
      return appPath + getDefaultFileName(jobType);
    } else {
      return appPath + "/" + getDefaultFileName(jobType);
    }
  }

  private String getDefaultFileName(JobType jobType){
    switch (jobType){
      case BUNDLE:
        return Constants.DEFAULT_BUNDLE_FILENAME+Constants.WF_EXTENSION;
      case COORDINATOR:
        return Constants.DEFAULT_COORDINATOR_FILENAME+Constants.WF_EXTENSION;
      case WORKFLOW:
        return Constants.DEFAULT_WORKFLOW_FILENAME+Constants.WF_EXTENSION;
      default:
        return null;
    }
  }
  private String getDefaultDraftFileName(JobType jobType) {
    switch (jobType){
      case BUNDLE:
        return Constants.DEFAULT_BUNDLE_FILENAME+Constants.WF_DRAFT_EXTENSION;
      case COORDINATOR:
        return Constants.DEFAULT_COORDINATOR_FILENAME+Constants.WF_DRAFT_EXTENSION;
      case WORKFLOW:
        return Constants.DEFAULT_WORKFLOW_FILENAME+Constants.WF_DRAFT_EXTENSION;
      default:
        return null;
    }
  }

  public String getAssetFileName(String appPath) {
    String assetFile = null;
    if (appPath.endsWith(Constants.WF_ASSET_EXTENSION)) {
      assetFile = appPath;
    } else {
      String[] paths=appPath.split("/");
      if (paths[paths.length-1].contains(".")){
        return appPath;
      }else{
        assetFile = appPath + (appPath.endsWith("/") ? "" : "/")
          + Constants.DEFAULT_WORKFLOW_ASSET_FILENAME;
      }
    }
    return assetFile;
  }

  public void discardDraft(String workflowPath) throws IOException {
    hdfsFileUtils.deleteFile(getWorkflowDraftFileName(workflowPath,null));
  }

  public WorkflowFileInfo getWorkflowDetails(String appPath, JobType jobType) {
    appPath=appPath.trim();
    WorkflowFileInfo workflowInfo = new WorkflowFileInfo();
    workflowInfo.setWorkflowPath(appPath);
    boolean draftExists = hdfsFileUtils
            .fileExists(getWorkflowDraftFileName(appPath,jobType
            ));
    workflowInfo.setDraftExists(draftExists);
    boolean workflowExists = hdfsFileUtils.fileExists(appPath);
    workflowInfo.setWorkflowDefinitionExists(workflowExists);
    FileStatus workflowFileStatus = null;
    if (workflowExists) {
      workflowFileStatus = hdfsFileUtils
              .getFileStatus(appPath);
      workflowInfo.setWorkflowModificationTime(workflowFileStatus
              .getModificationTime());

    }
    if (draftExists) {
      FileStatus draftFileStatus = hdfsFileUtils
              .getFileStatus(getWorkflowDraftFileName(appPath,jobType));
      workflowInfo.setDraftModificationTime(draftFileStatus
              .getModificationTime());
      if (!workflowExists) {
        workflowInfo.setIsDraftCurrent(true);
      } else {
        workflowInfo.setIsDraftCurrent(draftFileStatus.getModificationTime()
                - workflowFileStatus.getModificationTime() > 0);
      }
    }
    return workflowInfo;
  }
  public void deleteWorkflowFile(String fullWorkflowFilePath){
    try {
      hdfsFileUtils.deleteFile(fullWorkflowFilePath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean isDraftFormatCurrent(String json) {
    JsonElement jsonElement = new JsonParser().parse(json);
    JsonElement draftVersion = jsonElement.getAsJsonObject().get("draftVersion");
    if (draftVersion != null && currentDraftVersion.equals(draftVersion.getAsString().trim())) {
      return true;
    } else {
      return false;
    }
  }
}