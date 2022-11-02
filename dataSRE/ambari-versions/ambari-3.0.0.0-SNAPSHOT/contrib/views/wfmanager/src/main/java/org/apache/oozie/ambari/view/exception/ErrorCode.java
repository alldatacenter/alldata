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
package org.apache.oozie.ambari.view.exception;

public enum ErrorCode {
  OOZIE_SUBMIT_ERROR("error.oozie.submit", "Submitting job to Oozie failed. Please check your definition/configuration.",true),
  FILE_ACCESS_ACL_ERROR("error.file.access.control", "Access Error to file due to access control", true),
  FILE_ACCESS_UNKNOWN_ERROR("error.file.access", "Error accessing file"),
  WORKFLOW_PATH_EXISTS("error.workflow.path.exists", "File exists", true),
  WORKFLOW_XML_DOES_NOT_EXIST("error.workflow.xml.not.exists", "File does not exist", true),
  INVALID_ASSET_INPUT("error.invalid.asset.input", "Invalid asset definition", true),
  INVALID_EMPTY_INPUT("error.invalid.empty.input", "Input path cannot be empty", true),
  ASSET_NOT_EXIST("error.asset.not.exist","Asset doesn’t exist",true),
  PERMISSION_ERROR("error.permission","Don’t have permission",true),
  ASSET_INVALID_FROM_OOZIE("error.oozie.asset.invalid","Invalid Asset Definition",true);

  private String errorCode;
  private String description;
  private boolean isInputError = false;

  ErrorCode(String errorCode, String description) {
    this.errorCode = errorCode;
    this.description = description;
  }

  ErrorCode(String errorCode, String description, boolean isInputError) {
    this.errorCode = errorCode;
    this.description = description;
    this.isInputError = isInputError;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public String getDescription() {
    return description;
  }

  public boolean isInputError() {
    return isInputError;
  }
}