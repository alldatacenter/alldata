/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.api.properties;

import org.apache.curator.shaded.com.google.common.base.Strings;

public class AmsHAProperties {
  private static final String ROOT_PATH = "/arctic/ams";
  private static final String LEADER_PATH = "/leader";
  private static final String TABLE_SERVICE_MASTER_PATH = "/master";
  private static final String OPTIMIZING_SERVICE_MASTER_PATH = "/optimizing-service-master";
  private static final String NAMESPACE_DEFAULT = "default";

  private static String getBasePath(String namespace) {
    if (Strings.isNullOrEmpty(namespace)) {
      namespace = NAMESPACE_DEFAULT;
    }
    return "/" + namespace + ROOT_PATH;
  }

  public static String getTableServiceMasterPath(String namespace) {
    return getBasePath(namespace) + TABLE_SERVICE_MASTER_PATH;
  }

  public static String getOptimizingServiceMasterPath(String namespace) {
    return getBasePath(namespace) + OPTIMIZING_SERVICE_MASTER_PATH;
  }

  public static String getLeaderPath(String namespace) {
    return getBasePath(namespace) + LEADER_PATH;
  }
}
