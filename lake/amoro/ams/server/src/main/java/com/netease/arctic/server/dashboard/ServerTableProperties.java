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

package com.netease.arctic.server.dashboard;

import com.netease.arctic.table.TableProperties;

import java.util.HashSet;
import java.util.Set;

public class ServerTableProperties {

  public static final Set<String> HIDDEN_EXPOSED = new HashSet<>();
  public static final Set<String> HIDDEN_INTERNAL = new HashSet<>();

  static {
    HIDDEN_EXPOSED.add(TableProperties.BASE_TABLE_MAX_TRANSACTION_ID);
    HIDDEN_EXPOSED.add(TableProperties.PARTITION_OPTIMIZED_SEQUENCE);
    HIDDEN_EXPOSED.add(TableProperties.LOCATION);
    HIDDEN_EXPOSED.add(TableProperties.TABLE_CREATE_TIME);
    HIDDEN_EXPOSED.add(TableProperties.TABLE_PARTITION_PROPERTIES);
    HIDDEN_EXPOSED.add(TableProperties.WRITE_DISTRIBUTION_MODE_NONE);
    HIDDEN_EXPOSED.add(TableProperties.WRITE_DISTRIBUTION_MODE_HASH);
    HIDDEN_EXPOSED.add(TableProperties.WRITE_DISTRIBUTION_MODE_RANGE);
    HIDDEN_EXPOSED.add(TableProperties.WRITE_DISTRIBUTION_HASH_PRIMARY_PARTITION);
    HIDDEN_EXPOSED.add(TableProperties.WRITE_DISTRIBUTION_HASH_PRIMARY);
    HIDDEN_EXPOSED.add(TableProperties.WRITE_DISTRIBUTION_HASH_PARTITION);
    HIDDEN_EXPOSED.add(TableProperties.WRITE_PROTECTED_PROPERTIES.toString());
    HIDDEN_EXPOSED.add(TableProperties.TABLE_EVENT_TIME_FIELD);

    HIDDEN_INTERNAL.add("meta_store_site");
    HIDDEN_INTERNAL.add("hdfs_site");
    HIDDEN_INTERNAL.add("core_site");
    HIDDEN_INTERNAL.add("auth_method");
    HIDDEN_INTERNAL.add("hadoop_username");
    HIDDEN_INTERNAL.add("krb_keytab");
    HIDDEN_INTERNAL.add("krb_conf");
    HIDDEN_INTERNAL.add("krb_principal");
  }
}
