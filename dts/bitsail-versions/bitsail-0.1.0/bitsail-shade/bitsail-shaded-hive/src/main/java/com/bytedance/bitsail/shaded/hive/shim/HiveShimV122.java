/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.shaded.hive.shim;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.hive.metastore.RetryingMetaStoreClient;
import org.apache.hadoop.hive.ql.metadata.SessionHiveMetaStoreClient;
import org.apache.hadoop.hive.ql.session.SessionState;

public class HiveShimV122 extends HiveShimV121 {
  @Override
  public IMetaStoreClient getHiveMetastoreClient(HiveConf hiveConf) {
    try {
      IMetaStoreClient client;
      SessionState.start(hiveConf);

      client = RetryingMetaStoreClient.getProxy(hiveConf, new Class[] {HiveConf.class},
          new Object[] {hiveConf}, SessionHiveMetaStoreClient.class.getName());

      return client;
    } catch (Exception e) {
      throw new RuntimeException("Error while calling HiveMetaClientUtil::getMetastoreClient. " + e.getMessage(), e);
    }
  }

  @Override
  public String getVersion() {
    return "1.2.2";
  }
}
