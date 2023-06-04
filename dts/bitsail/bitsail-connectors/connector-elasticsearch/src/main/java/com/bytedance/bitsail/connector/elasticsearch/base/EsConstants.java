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

package com.bytedance.bitsail.connector.elasticsearch.base;

public class EsConstants {

  public static final int DEFAULT_ES_CONNECTION_REQUEST_TIMEOUT = 10000;
  public static final int DEFAULT_ES_CONNECTION_TIMEOUT = 10000;
  public static final int DEFAULT_ES_SOCKET_TIMEOUT = 60000;

  public static final int ILLEGAL_REST_STATUS_CODE = -1;

  public static final String DEFAULT_BULK_FLUSH_MAX_ACTIONS = "300";
  public static final String DEFAULT_BULK_FLUSH_MAX_SIZE_MB = "10";
  public static final String DEFAULT_BULK_FLUSH_INTERVAL = "1000";

  public static final String BACKOFF_POLICY_CONSTANT = "CONSTANT";
  public static final String BACKOFF_POLICY_EXPONENTIAL = "EXPONENTIAL";
  public static final String BACKOFF_POLICY_NONE = "NONE";

  public static final String KEY_NULL_LITERAL = "";

  public static final String FIELD_NAME_INDEX = "_index";
  public static final String FIELD_NAME_TYPE = "_type";
  public static final String FIELD_NAME_OP_TYPE = "_op_type";
  public static final String FIELD_NAME_ID = "_id";
  public static final String FIELD_NAME_ROUTING = "_routing";
  public static final String FIELD_NAME_VERSION = "_version";
  public static final String FIELD_NAME_SOURCE = "_source";
  public static final String FIELD_NAME_SCRIPT = "_script";

  public static final long DEFAULT_VERSION = -1;

  public static final String OPERATION_TYPE_DELETE = "delete";
  public static final String OPERATION_TYPE_INDEX = "index";
  public static final String OPERATION_TYPE_CREATE = "create";
  public static final String OPERATION_TYPE_UPDATE = "update";
  public static final String OPERATION_TYPE_UPSERT = "upsert";
  public static final String DEFAULT_OPERATION_TYPE = OPERATION_TYPE_INDEX;

  public static final String ES_CONNECTOR_NAME = "elasticsearch";
}
