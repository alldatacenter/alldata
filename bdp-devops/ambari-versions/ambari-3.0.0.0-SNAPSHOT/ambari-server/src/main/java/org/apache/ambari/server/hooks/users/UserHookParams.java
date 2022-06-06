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

package org.apache.ambari.server.hooks.users;

/**
 * Command parameter identifier list for the post user creation hook.
 */
public enum UserHookParams {

  SCRIPT("hook-script"),
  // the payload the hook operates on
  PAYLOAD("cmd-payload"),

  CLUSTER_ID("cluster-id"),
  CLUSTER_NAME("cluster-name"),
  CMD_TIME_FRAME("cmd-timeframe"),
  CMD_INPUT_FILE("cmd-input-file"),
  // identify security related values
  CLUSTER_SECURITY_TYPE("cluster-security-type"),
  CMD_HDFS_PRINCIPAL("cmd-hdfs-principal"),
  CMD_HDFS_KEYTAB("cmd-hdfs-keytab"),
  CMD_HDFS_USER("cmd-hdfs-user");


  private String param;

  UserHookParams(String param) {
    this.param = param;
  }

  public String param() {
    return param;
  }
}
