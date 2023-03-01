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

package org.apache.celeborn.common.protocol;

public class RpcNameConstants {
  // For Master
  public static String MASTER_SYS = "MasterSys";

  // Master Endpoint Name
  public static String MASTER_EP = "MasterEndpoint";

  // For Worker
  public static String WORKER_SYS = "WorkerSys";

  // Worker Endpoint Name
  public static String WORKER_EP = "WorkerEndpoint";

  // For Driver(RssShuffleManager)
  public static String RSS_METASERVICE_EP = "RssMetaServiceEndpoint";
  public static String RSS_METASERVICE_SYS = "RssMetaSys";
}
