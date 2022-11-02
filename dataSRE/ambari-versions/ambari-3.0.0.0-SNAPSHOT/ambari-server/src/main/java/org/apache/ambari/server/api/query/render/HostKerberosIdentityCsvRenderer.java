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

package org.apache.ambari.server.api.query.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.serializers.CsvSerializer;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Renderer which updates a KerberosHostIdentity resource so it may be serialized using a CSV serializer.
 * <p/>
 * This implementation extends the DefaultRenderer to add the header mapping and column order information
 * to the root of TreeNode structure.
 *
 * @see CsvSerializer
 */
public class HostKerberosIdentityCsvRenderer extends DefaultRenderer {

  @Override
  public Result finalizeResult(Result queryResult) {
    TreeNode<Resource> resultTree = queryResult.getResultTree();

    if(resultTree != null) {
      // TODO: Determine which columns/fields are relevant for the query and prune as needed.
      Map<String, String> columnMap = new HashMap<String, String>() {{
        put("KerberosIdentity/host_name", "host");
        put("KerberosIdentity/description", "description");
        put("KerberosIdentity/principal_name", "principal name");
        put("KerberosIdentity/principal_type", "principal type");
        put("KerberosIdentity/principal_local_username", "local username");
        put("KerberosIdentity/keytab_file_path", "keytab file path");
        put("KerberosIdentity/keytab_file_owner", "keytab file owner");
        put("KerberosIdentity/keytab_file_owner_access", "keytab file owner access");
        put("KerberosIdentity/keytab_file_group", "keytab file group");
        put("KerberosIdentity/keytab_file_group_access", "keytab file group access");
        put("KerberosIdentity/keytab_file_mode", "keytab file mode");
        put("KerberosIdentity/keytab_file_installed", "keytab file installed");
      }};

      List<String> columnOrder = new ArrayList<String>() {{
        add("KerberosIdentity/host_name");
        add("KerberosIdentity/description");
        add("KerberosIdentity/principal_name");
        add("KerberosIdentity/principal_type");
        add("KerberosIdentity/principal_local_username");
        add("KerberosIdentity/keytab_file_path");
        add("KerberosIdentity/keytab_file_owner");
        add("KerberosIdentity/keytab_file_owner_access");
        add("KerberosIdentity/keytab_file_group");
        add("KerberosIdentity/keytab_file_group_access");
        add("KerberosIdentity/keytab_file_mode");
        add("KerberosIdentity/keytab_file_installed");
      }};

      resultTree.setProperty(CsvSerializer.PROPERTY_COLUMN_MAP, columnMap);
      resultTree.setProperty(CsvSerializer.PROPERTY_COLUMN_ORDER, columnOrder);
    }

    return queryResult;
  }
}
