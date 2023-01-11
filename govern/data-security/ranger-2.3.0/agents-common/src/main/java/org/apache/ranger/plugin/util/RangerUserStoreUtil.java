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

package org.apache.ranger.plugin.util;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class RangerUserStoreUtil {
    public static final String CLOUD_IDENTITY_NAME = "cloud_id";

    public static String getPrintableOptions(Map<String, String> otherAttributes) {
        if (MapUtils.isEmpty(otherAttributes)) return "{}";
        StringBuilder ret = new StringBuilder();
        ret.append("{");
        for (Map.Entry<String, String> entry : otherAttributes.entrySet()) {
            ret.append(entry.getKey()).append(", ").append("[").append(entry.getValue()).append("]").append(",");
        }
        ret.append("}");
        return ret.toString();
    }

    public static String getAttrVal(Map<String, Map<String, String>> attrMap, String name, String attrName) {
        String ret = null;

        if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(attrName)) {
            Map<String, String> attrs = attrMap.get(name);
            if (MapUtils.isNotEmpty(attrs)) {
                ret = attrs.get(attrName);
            }
        }
        return ret;
    }

    public String getCloudId(Map<String, Map<String, String>> attrMap, String name) {
        String cloudId = null;
        cloudId = getAttrVal(attrMap, name, CLOUD_IDENTITY_NAME);
        return cloudId;
    }
}


