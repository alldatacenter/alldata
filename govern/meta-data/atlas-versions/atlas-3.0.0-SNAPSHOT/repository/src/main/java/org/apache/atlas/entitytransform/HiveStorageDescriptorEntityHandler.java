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
package org.apache.atlas.entitytransform;

import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;

import static org.apache.atlas.entitytransform.TransformationConstants.*;

public class HiveStorageDescriptorEntityHandler extends BaseEntityHandler {
    static final List<String> CUSTOM_TRANSFORM_ATTRIBUTES = Arrays.asList(HIVE_DB_NAME_ATTRIBUTE, HIVE_TABLE_NAME_ATTRIBUTE, HIVE_DB_CLUSTER_NAME_ATTRIBUTE, HIVE_STORAGE_DESC_LOCATION_ATTRIBUTE);


    public HiveStorageDescriptorEntityHandler(List<AtlasEntityTransformer> transformers) {
        super(transformers);
    }

    @Override
    public AtlasTransformableEntity getTransformableEntity(AtlasEntity entity) {
        return isHiveStorageDescEntity(entity) ? new HiveStorageDescriptorEntity(entity) : null;
    }

    private boolean isHiveStorageDescEntity(AtlasEntity entity) {
        return StringUtils.equals(entity.getTypeName(), HIVE_STORAGE_DESCRIPTOR);
    }

    public static class HiveStorageDescriptorEntity extends AtlasTransformableEntity {
        private String  databaseName;
        private String  tableName;
        private String  clusterName;
        private String  location;
        private boolean isCustomAttributeUpdated = false;


        public HiveStorageDescriptorEntity(AtlasEntity entity) {
            super(entity);

            this.location = (String) entity.getAttribute(LOCATION_ATTRIBUTE);

            String qualifiedName = (String) entity.getAttribute(QUALIFIED_NAME_ATTRIBUTE);

            if (qualifiedName != null) {
                int    databaseSeparatorIdx  = qualifiedName.indexOf(DATABASE_DELIMITER);
                int    clusterSeparatorIdx   = qualifiedName.lastIndexOf(CLUSTER_DELIMITER);
                String clusterNameWithSuffix = clusterSeparatorIdx != -1 ? qualifiedName.substring(clusterSeparatorIdx + 1) : "";

                this.databaseName = (databaseSeparatorIdx != -1) ? qualifiedName.substring(0, databaseSeparatorIdx) : "";
                this.tableName    = (databaseSeparatorIdx != -1 && clusterSeparatorIdx != -1) ? qualifiedName.substring(databaseSeparatorIdx + 1, clusterSeparatorIdx) : "";

                if (StringUtils.isNotEmpty(clusterNameWithSuffix)) {
                    int idx = clusterNameWithSuffix.lastIndexOf(HIVE_STORAGEDESC_SUFFIX);

                    this.clusterName = (idx != -1) ? clusterNameWithSuffix.substring(0, idx) : "";
                } else {
                    this.clusterName = "";
                }
            } else {
                this.databaseName = "";
                this.tableName    = "";
                this.clusterName  = "";
            }
        }

        @Override
        public Object getAttribute(EntityAttribute attribute) {
            switch (attribute.getAttributeKey()) {
                case HIVE_DB_NAME_ATTRIBUTE:
                    return databaseName;

                case HIVE_TABLE_NAME_ATTRIBUTE:
                    return tableName;

                case HIVE_DB_CLUSTER_NAME_ATTRIBUTE:
                    return clusterName;

                case HIVE_STORAGE_DESC_LOCATION_ATTRIBUTE:
                    return location;
            }

            return super.getAttribute(attribute);
        }

        @Override
        public void setAttribute(EntityAttribute attribute, String attributeValue) {
            switch (attribute.getAttributeKey()) {
                case HIVE_DB_NAME_ATTRIBUTE:
                    databaseName = attributeValue;

                    isCustomAttributeUpdated = true;
                break;

                case HIVE_TABLE_NAME_ATTRIBUTE:
                    tableName = attributeValue;

                    isCustomAttributeUpdated = true;
                break;

                case HIVE_DB_CLUSTER_NAME_ATTRIBUTE:
                    clusterName = attributeValue;

                    isCustomAttributeUpdated = true;
                break;

                case HIVE_STORAGE_DESC_LOCATION_ATTRIBUTE:
                    location = attributeValue;
                break;

                default:
                    super.setAttribute(attribute, attributeValue);
                break;
            }
        }

        @Override
        public void transformComplete() {
            if (isCustomAttributeUpdated) {
                entity.setAttribute(LOCATION_ATTRIBUTE, toLocation());
                entity.setAttribute(QUALIFIED_NAME_ATTRIBUTE, toQualifiedName());
            }
        }

        private String toLocation() {
            int lastPathIndex = location != null ? location.lastIndexOf(PATH_DELIMITER) : -1;

            return lastPathIndex != -1 ? location.substring(0, lastPathIndex) + PATH_DELIMITER + tableName : location;
        }

        private String toQualifiedName() {
            return String.format("%s.%s@%s_storage", databaseName, tableName, clusterName);
        }
    }
}