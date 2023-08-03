/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.repository.graphdb.janus.migration;

import org.apache.atlas.model.typedef.AtlasRelationshipDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef.PropagateTags;
import org.apache.atlas.model.typedef.AtlasRelationshipEndDef;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.type.AtlasRelationshipType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.v1.typesystem.types.utils.TypesUtil;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class RelationshipCacheGenerator {

    public static class TypeInfo extends TypesUtil.Pair<String, PropagateTags> {

        public TypeInfo(String typeName, PropagateTags propagateTags) {
            super(typeName, propagateTags);
        }

        public String getTypeName() {
            return left;
        }

        public PropagateTags getPropagateTags() {
            return right;
        }
    }


    public static Map<String, TypeInfo> get(AtlasTypeRegistry typeRegistry) {
        Map<String, TypeInfo> ret = new HashMap<>();

        for (AtlasRelationshipType relType : typeRegistry.getAllRelationshipTypes()) {
            AtlasRelationshipDef relDef      = relType.getRelationshipDef();
            String               relTypeName = relType.getTypeName();

            add(ret, getKey(relDef.getEndDef1()), relTypeName, relDef.getPropagateTags());
            add(ret, getKey(relDef.getEndDef2()), relTypeName, getEnd2PropagateTag(relDef.getPropagateTags()));
        }

        return ret;
    }

    private static String getKey(AtlasRelationshipEndDef endDef) {
        return getKey(endDef.getIsLegacyAttribute(), endDef.getType(), endDef.getName());
    }

    private static String getKey(String lhs, String rhs) {
        return String.format("%s%s.%s", Constants.INTERNAL_PROPERTY_KEY_PREFIX, lhs, rhs);
    }

    private static String getKey(boolean isLegacy, String entityTypeName, String relEndName) {
        if (!isLegacy) {
            return "";
        }

        return getKey(entityTypeName, relEndName);
    }

    private static void add(Map<String, TypeInfo> map, String key, String relationTypeName, PropagateTags propagateTags) {
        if (StringUtils.isEmpty(key) || map.containsKey(key)) {
            return;
        }

        map.put(key, new TypeInfo(relationTypeName, propagateTags));
    }

    private static PropagateTags getEnd2PropagateTag(PropagateTags end1PropagateTags) {
        if (end1PropagateTags == PropagateTags.ONE_TO_TWO) {
            return PropagateTags.TWO_TO_ONE;
        } else if (end1PropagateTags == PropagateTags.TWO_TO_ONE) {
            return PropagateTags.ONE_TO_TWO;
        } else {
            return end1PropagateTags;
        }
    }
}
