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

package org.apache.ambari.server.serveraction.kerberos;

/**
 * KerberosConfigDataFile declares the default data file name and the common record column names
 * for the Kerberos configuration data files.
 */
public interface KerberosConfigDataFile extends KerberosDataFile {
  String DATA_FILE_NAME = "configs.dat";

  String CONFIGURATION_TYPE = "config";
  String KEY = "key";
  String VALUE = "value";
  String OPERATION = "operation";

  String OPERATION_TYPE_SET = "SET";
  String OPERATION_TYPE_REMOVE = "REMOVE";
}
