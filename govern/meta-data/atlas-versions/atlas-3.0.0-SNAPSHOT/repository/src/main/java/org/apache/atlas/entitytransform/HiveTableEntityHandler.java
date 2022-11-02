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

public class HiveTableEntityHandler extends BaseEntityHandler {
    static final List<String> CUSTOM_TRANSFORM_ATTRIBUTES = Arrays.asList(HIVE_DB_NAME_ATTRIBUTE, HIVE_TABLE_NAME_ATTRIBUTE, HIVE_DB_CLUSTER_NAME_ATTRIBUTE);


    public HiveTableEntityHandler(List<AtlasEntityTransformer> transformers) {
        super(transformers);
    }

    @Override
    public AtlasTransformableEntity getTransformableEntity(AtlasEntity entity) {
        return isHiveTableEntity(entity) ? new HiveTableEntity(entity) : null;
    }

    private boolean isHiveTableEntity(AtlasEntity entity) {
        return StringUtils.equals(entity.getTypeName(), HIVE_TABLE);
    }

    private static class HiveTableEntity extends AtlasTransformableEntity {
        private final String qualifiedName;
        private String       databaseName;
        private String       tableName;
        private String       clusterName;
        private boolean      isCustomAttributeUpdated = false;

        private final boolean tableNameFromQualifiedNameDifferent;
        private final String  tableNameFromQualifiedName;

        public HiveTableEntity(AtlasEntity entity) {
            super(entity);

            this.tableName = (String) entity.getAttribute(NAME_ATTRIBUTE);

            this.qualifiedName = (String) entity.getAttribute(QUALIFIED_NAME_ATTRIBUTE);

            if (qualifiedName != null) {
                int databaseSeparatorIdx = qualifiedName.indexOf(DATABASE_DELIMITER);
                int clusterSeparatorIdx  = qualifiedName.lastIndexOf(CLUSTER_DELIMITER);

                this.databaseName = databaseSeparatorIdx != -1 ? qualifiedName.substring(0, databaseSeparatorIdx) : "";
                this.clusterName  = clusterSeparatorIdx != -1  ? qualifiedName.substring(clusterSeparatorIdx + 1) : "";
                this.tableNameFromQualifiedName = clusterSeparatorIdx != -1 ? this.qualifiedName.substring(databaseSeparatorIdx + 1, clusterSeparatorIdx) : "";
                this.tableNameFromQualifiedNameDifferent = !this.tableNameFromQualifiedName.equals(this.tableName);
            } else {
                this.databaseName = "";
                this.clusterName  = "";
                this.tableNameFromQualifiedName = "";
                this.tableNameFromQualifiedNameDifferent = false;
            }
        }

        @Override
        public Object getAttribute(EntityAttribute attribute) {
            switch (attribute.getAttributeKey()) {
                case HIVE_TABLE_NAME_ATTRIBUTE:
                    return tableName;

                case HIVE_DB_NAME_ATTRIBUTE:
                    return databaseName;

                case HIVE_DB_CLUSTER_NAME_ATTRIBUTE:
                    return clusterName;
            }

            return super.getAttribute(attribute);
        }

        @Override
        public void setAttribute(EntityAttribute attribute, String attributeValue) {
            switch (attribute.getAttributeKey()) {
                case HIVE_TABLE_NAME_ATTRIBUTE:
                    tableName = attributeValue;

                    isCustomAttributeUpdated = true;
                break;

                case HIVE_DB_NAME_ATTRIBUTE:
                    databaseName = attributeValue;

                    isCustomAttributeUpdated = true;
                break;

                case HIVE_DB_CLUSTER_NAME_ATTRIBUTE:
                    clusterName = attributeValue;

                    isCustomAttributeUpdated = true;
                break;

                default:
                    super.setAttribute(attribute, attributeValue);
                break;
            }
        }

        @Override
        public void transformComplete() {
            if (isCustomAttributeUpdated) {
                entity.setAttribute(NAME_ATTRIBUTE, tableName);
                entity.setAttribute(QUALIFIED_NAME_ATTRIBUTE, toQualifiedName());
            }
        }


        private String toQualifiedName() {
            String tableName = tableNameFromQualifiedNameDifferent ? this.tableNameFromQualifiedName : this.tableName;

            return String.format("%s.%s@%s", databaseName, tableName, clusterName);
        }
    }
}