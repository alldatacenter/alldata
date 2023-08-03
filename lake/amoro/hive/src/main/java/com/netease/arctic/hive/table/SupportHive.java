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

package com.netease.arctic.hive.table;

import com.netease.arctic.hive.HMSClientPool;
import com.netease.arctic.io.ArcticHadoopFileIO;
import com.netease.arctic.table.ArcticTable;

/**
 * Mix-in interface to mark task use hive as base store
 */
public interface SupportHive extends ArcticTable {

  ArcticHadoopFileIO io();

  /**
   * Base path to store hive data files
   *
   * @return path to store hive file
   */
  String hiveLocation();

  /**
   * the client to operate hive table
   *
   * @return hive metastore client
   */
  HMSClientPool getHMSClient();

  boolean enableSyncHiveDataToArctic();

  boolean enableSyncHiveSchemaToArctic();

  void syncHiveDataToArctic(boolean force);

  void syncHiveSchemaToArctic();
}
