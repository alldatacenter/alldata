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

public class AmsHAProperties {
  private static final String ROOT_PATH = "/arctic/ams";
  private static final String LEADER_PATH = "/leader";
  private static final String MASTER_PATH = "/master";
  private static final String NAMESPACE_DEFAULT = "/master";

  public static String getBasePath(String namespace) {
    if (namespace.isEmpty()) {
      namespace = getNamespaceDefault();
    }
    return "/" + namespace + ROOT_PATH;
  }

  public static String getMasterPath(String namespace) {
    if (namespace.isEmpty()) {
      namespace = getNamespaceDefault();
    }
    return "/" + namespace + ROOT_PATH + MASTER_PATH;
  }

  public static String getLeaderPath(String namespace) {
    if (namespace.isEmpty()) {
      namespace = getNamespaceDefault();
    }
    return "/" + namespace + ROOT_PATH + LEADER_PATH;
  }

  public static String getNamespaceDefault() {
    return NAMESPACE_DEFAULT;
  }
}
