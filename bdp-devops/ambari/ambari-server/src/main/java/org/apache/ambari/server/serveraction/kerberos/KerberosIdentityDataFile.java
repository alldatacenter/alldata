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
 * KerberosIdentityDataFile declares the default data file name and the common record column names
 * for the Kerberos action (metadata) data files.
 */
public interface KerberosIdentityDataFile extends KerberosDataFile {
  String DATA_FILE_NAME = "identity.dat";

  String HOSTNAME = "hostname";
  String SERVICE = "service";
  String COMPONENT = "component";
  String PRINCIPAL = "principal";
  String PRINCIPAL_TYPE = "principal_type";
  String KEYTAB_FILE_PATH = "keytab_file_path";
  String KEYTAB_FILE_OWNER_NAME = "keytab_file_owner_name";
  String KEYTAB_FILE_OWNER_ACCESS = "keytab_file_owner_access";
  String KEYTAB_FILE_GROUP_NAME = "keytab_file_group_name";
  String KEYTAB_FILE_GROUP_ACCESS = "keytab_file_group_access";
  String KEYTAB_FILE_IS_CACHABLE = "keytab_file_is_cachable";
}
