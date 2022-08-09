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

public interface Constants {
  String STATUS_FAILED = "failed";
  String STATUS_OK = "ok";
  String STATUS_KEY = "status";
  String MESSAGE_KEY = "message";
  String WF_DRAFT_EXTENSION = ".wfdraft";
  String WF_EXTENSION = ".xml";
  String DEFAULT_WORKFLOW_FILENAME="workflow";
  String DEFAULT_COORDINATOR_FILENAME="coordinator";
  String DEFAULT_BUNDLE_FILENAME="bundle";
  String DEFAULT_DRAFT_FILENAME="workflow"+WF_DRAFT_EXTENSION;
  String WF_ASSET_EXTENSION = ".wfasset";
  String DEFAULT_WORKFLOW_ASSET_FILENAME="asset"+WF_ASSET_EXTENSION;
}
