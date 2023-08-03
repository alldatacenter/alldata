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
package org.apache.atlas.notification.preprocessor;

import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class RdbmsPreprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(RdbmsPreprocessor.class);

    private static final String RELATIONSHIP_TYPE_RDBMS_INSTANCE_DATABASES = "rdbms_instance_databases";
    private static final String RELATIONSHIP_TYPE_RDBMS_DB_TABLES          = "rdbms_db_tables";
    private static final String RELATIONSHIP_TYPE_RDBMS_TABLE_COLUMNS      = "rdbms_table_columns";
    private static final String RELATIONSHIP_TYPE_RDBMS_TABLE_INDEXES      = "rdbms_table_indexes";
    private static final String RELATIONSHIP_TYPE_RDBMS_TABLE_FOREIGN_KEYS = "rdbms_table_foreign_key";

    static class RdbmsInstancePreprocessor extends RdbmsTypePreprocessor {
        public RdbmsInstancePreprocessor() {
            super(TYPE_RDBMS_INSTANCE);
        }
    }

    static class RdbmsDbPreprocessor extends RdbmsTypePreprocessor {
        public RdbmsDbPreprocessor() {
            super(TYPE_RDBMS_DB);
        }
    }

    static class RdbmsTablePreprocessor extends RdbmsTypePreprocessor {
        public RdbmsTablePreprocessor() {
            super(TYPE_RDBMS_TABLE);
        }
        @Override
        public void preprocess(AtlasEntity entity, PreprocessorContext context) {
            super.preprocess(entity, context);

            // try auto-fix when 'db' attribute is not present in relationshipAttribute & attributes
            Object db = entity.getRelationshipAttribute(ATTRIBUTE_DB);

            if (db == null) {
                db = entity.getAttribute(ATTRIBUTE_DB);
            }

            if (db == null) {
                String dbQualifiedName = getDbQualifiedName(entity);

                if (dbQualifiedName != null) {
                    AtlasObjectId dbId = new AtlasObjectId(TYPE_RDBMS_DB, Collections.singletonMap(ATTRIBUTE_QUALIFIED_NAME, dbQualifiedName));

                    LOG.info("missing attribute {}.{} is set to {}", TYPE_RDBMS_TABLE, ATTRIBUTE_DB, dbId);

                    entity.setRelationshipAttribute(ATTRIBUTE_DB, dbId);
                }
            }
        }

        private String getDbQualifiedName(AtlasEntity tableEntity) {
            String ret              = null;
            Object tblQualifiedName = tableEntity.getAttribute(ATTRIBUTE_QUALIFIED_NAME);  // dbName.tblName@clusterName
            Object tblName          = tableEntity.getAttribute(ATTRIBUTE_NAME);  // tblName

            if (tblQualifiedName != null && tblName != null) {
                ret = tblQualifiedName.toString().replace("." + tblName.toString() + "@", "@"); // dbName@clusterName
            }

            return ret;
        }

    }

    static class RdbmsTypePreprocessor extends EntityPreprocessor {
        private static final Set<String> entityTypesToMove = new HashSet<>();

        static {
            entityTypesToMove.add(TYPE_RDBMS_DB);
            entityTypesToMove.add(TYPE_RDBMS_TABLE);
            entityTypesToMove.add(TYPE_RDBMS_COLUMN);
            entityTypesToMove.add(TYPE_RDBMS_INDEX);
            entityTypesToMove.add(TYPE_RDBMS_FOREIGN_KEY);
        }

        protected RdbmsTypePreprocessor(String typeName) {
            super(typeName);
        }

        @Override
        public void preprocess(AtlasEntity entity, PreprocessorContext context) {
            if (context.getRdbmsTypesRemoveOwnedRefAttrs()) {
                clearRefAttributesAndMove(entity, context);

                Map<String, AtlasEntity> referredEntities = context.getReferredEntities();

                if (MapUtils.isNotEmpty(referredEntities)) {
                    for (AtlasEntity referredEntity : referredEntities.values()) {
                        if (entityTypesToMove.contains(referredEntity.getTypeName())) {
                            clearRefAttributesAndMove(referredEntity, context);
                        }
                    }
                }
            }
        }

        private void clearRefAttributesAndMove(AtlasEntity entity, PreprocessorContext context) {
            switch (entity.getTypeName()) {
                case TYPE_RDBMS_INSTANCE:
                    context.removeRefAttributeAndRegisterToMove(entity, ATTRIBUTE_DATABASES, RELATIONSHIP_TYPE_RDBMS_INSTANCE_DATABASES, ATTRIBUTE_INSTANCE);
                break;

                case TYPE_RDBMS_DB:
                    context.removeRefAttributeAndRegisterToMove(entity, ATTRIBUTE_TABLES, RELATIONSHIP_TYPE_RDBMS_DB_TABLES, ATTRIBUTE_DB);
                break;

                case TYPE_RDBMS_TABLE:
                    context.removeRefAttributeAndRegisterToMove(entity, ATTRIBUTE_COLUMNS, RELATIONSHIP_TYPE_RDBMS_TABLE_COLUMNS, ATTRIBUTE_TABLE);
                    context.removeRefAttributeAndRegisterToMove(entity, ATTRIBUTE_INDEXES, RELATIONSHIP_TYPE_RDBMS_TABLE_INDEXES, ATTRIBUTE_TABLE);
                    context.removeRefAttributeAndRegisterToMove(entity, ATTRIBUTE_FOREIGN_KEYS, RELATIONSHIP_TYPE_RDBMS_TABLE_FOREIGN_KEYS, ATTRIBUTE_TABLE);
                break;
            }
        }
    }
}
