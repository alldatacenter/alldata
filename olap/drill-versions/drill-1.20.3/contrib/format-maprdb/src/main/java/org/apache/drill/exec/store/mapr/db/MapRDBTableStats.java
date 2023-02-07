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
package org.apache.drill.exec.store.mapr.db;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.mapr.TableMappingRulesFactory;

import com.mapr.fs.hbase.HBaseAdminImpl;

public class MapRDBTableStats {
  private static volatile HBaseAdminImpl admin = null;

  private long numRows;

  public MapRDBTableStats(Configuration conf, String tablePath) throws Exception {
    if (admin == null) {
      synchronized (MapRDBTableStats.class) {
        if (admin == null) {
          Configuration config = conf;
          admin = new HBaseAdminImpl(config, TableMappingRulesFactory.create(conf));
        }
      }
    }
    numRows = admin.getNumRows(tablePath);
  }

  public long getNumRows() {
    return numRows;
  }

}
