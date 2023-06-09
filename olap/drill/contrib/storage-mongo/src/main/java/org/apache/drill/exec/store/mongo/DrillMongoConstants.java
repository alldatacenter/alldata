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
package org.apache.drill.exec.store.mongo;

public interface DrillMongoConstants {

  String SYS_STORE_PROVIDER_MONGO_URL = "drill.exec.sys.store.provider.mongo.url";

  String ID = "_id";

  String SHARDS = "shards";

  String NS = "ns";

  String SHARD = "shard";

  String HOST = "host";

  String CHUNKS = "chunks";

  String SIZE = "size";

  String COUNT = "count";

  String CONFIG = "config";

  String MIN = "min";

  String MAX = "max";

  String PARTITIONED = "partitioned";

  String PRIMARY = "primary";

  String DATABASES = "databases";

  String STORE_CONFIG_PREFIX = "drill.exec.store.";

  String USERNAME_CONFIG_SUFFIX = ".username";

  String PASSWORD_CONFIG_SUFFIX = ".password";
}
