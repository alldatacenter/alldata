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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.authorization.hbase;


import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class RangerHBaseResource extends RangerAccessResourceImpl {
    public static final String KEY_TABLE              = "table";
    public static final String KEY_COLUMN_FAMILY      = "column-family";
    public static final String KEY_COLUMN             = "column";
    public static final String WILDCARD               = "*";
    public static final String NAMESPACE_SEPARATOR    = ":";
    public static final String DEFAULT_NAMESPACE      = "default" + NAMESPACE_SEPARATOR;

    public RangerHBaseResource() {
    }

    public RangerHBaseResource(Map<String, Object> elements) {
        super(elements);
        setValue(KEY_TABLE, getValue(KEY_TABLE));
    }

    public RangerHBaseResource(Map<String, Object> elements, String ownerUser) {
        super(elements, ownerUser);
        setValue(KEY_TABLE, getValue(KEY_TABLE));
    }

    @Override
    public void setValue(String key, Object value) {
        // special handling for tables in 'default' namespace
        if (StringUtils.equals(key, KEY_TABLE)) {
            if (value instanceof String) {
                String tableName = (String) value;

                if (!tableName.contains(NAMESPACE_SEPARATOR)) {
                    List<String> tableNames = new ArrayList<>(2);

                    tableNames.add(tableName);
                    tableNames.add(DEFAULT_NAMESPACE + tableName);

                    value = tableNames;
                } else if (StringUtils.startsWith(tableName, DEFAULT_NAMESPACE)) {
                    List<String> tableNames = new ArrayList<>(2);

                    tableNames.add(tableName);
                    tableNames.add(tableName.substring(DEFAULT_NAMESPACE.length()));

                    value = tableNames;
                }
            }
        }
        super.setValue(key, value);
    }

    void resetValue(String key) {
        // Undo special handling for tables in 'default' namespace
        if (StringUtils.equals(key, KEY_TABLE)) {
            Object value = getValue(key);
            if (value instanceof List) {
                List<?> tableNames = (List<?>) value;
                if (!tableNames.isEmpty()) {
                    super.setValue(key, tableNames.get(0));
                }
            }
        }
    }
}
