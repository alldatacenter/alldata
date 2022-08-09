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
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasObjectId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public abstract class EntityPreprocessor {
    public static final String TYPE_HIVE_COLUMN         = "hive_column";
    public static final String TYPE_HIVE_COLUMN_LINEAGE = "hive_column_lineage";
    public static final String TYPE_HIVE_PROCESS        = "hive_process";
    public static final String TYPE_HIVE_STORAGEDESC    = "hive_storagedesc";
    public static final String TYPE_HIVE_DB             = "hive_db";
    public static final String TYPE_HIVE_DB_DDL         = "hive_db_ddl";
    public static final String TYPE_HIVE_TABLE_DDL      = "hive_table_ddl";
    public static final String TYPE_HIVE_TABLE          = "hive_table";
    public static final String TYPE_RDBMS_INSTANCE      = "rdbms_instance";
    public static final String TYPE_RDBMS_DB            = "rdbms_db";
    public static final String TYPE_RDBMS_TABLE         = "rdbms_table";
    public static final String TYPE_RDBMS_COLUMN        = "rdbms_column";
    public static final String TYPE_RDBMS_INDEX         = "rdbms_index";
    public static final String TYPE_RDBMS_FOREIGN_KEY   = "rdbms_foreign_key";

    public static final String ATTRIBUTE_COLUMNS        = "columns";
    public static final String ATTRIBUTE_INPUTS         = "inputs";
    public static final String ATTRIBUTE_OUTPUTS        = "outputs";
    public static final String ATTRIBUTE_PARTITION_KEYS = "partitionKeys";
    public static final String ATTRIBUTE_QUALIFIED_NAME = "qualifiedName";
    public static final String ATTRIBUTE_NAME           = "name";
    public static final String ATTRIBUTE_SD             = "sd";
    public static final String ATTRIBUTE_DB             = "db";
    public static final String ATTRIBUTE_DATABASES      = "databases";
    public static final String ATTRIBUTE_QUERY          = "query";
    public static final String ATTRIBUTE_TABLE          = "table";
    public static final String ATTRIBUTE_TABLES         = "tables";
    public static final String ATTRIBUTE_INDEXES        = "indexes";
    public static final String ATTRIBUTE_FOREIGN_KEYS   = "foreign_keys";
    public static final String ATTRIBUTE_INSTANCE       = "instance";
    public static final String ATTRIBUTE_START_TIME     = "startTime";
    public static final String ATTRIBUTE_END_TIME       = "endTime";

    public static final char   QNAME_SEP_CLUSTER_NAME = '@';
    public static final char   QNAME_SEP_ENTITY_NAME  = '.';
    public static final String QNAME_SD_SUFFIX        = "_storage";

    private static final Map<String, EntityPreprocessor> HIVE_PREPROCESSOR_MAP      = new HashMap<>();
    private static final Map<String, EntityPreprocessor> RDBMS_PREPROCESSOR_MAP     = new HashMap<>();
    private static final Map<String, EntityPreprocessor> AWS_S3_V2_PREPROCESSOR_MAP = new HashMap<>();

    private final String typeName;


    static {
        EntityPreprocessor[] hivePreprocessors = new EntityPreprocessor[] {
                                                                    new HivePreprocessor.HiveDbPreprocessor(),
                                                                    new HiveDbDDLPreprocessor(),
                                                                    new HivePreprocessor.HiveTablePreprocessor(),
                                                                    new HivePreprocessor.HiveColumnPreprocessor(),
                                                                    new HivePreprocessor.HiveProcessPreprocessor(),
                                                                    new HivePreprocessor.HiveColumnLineageProcessPreprocessor(),
                                                                    new HivePreprocessor.HiveStorageDescPreprocessor(),
                                                                    new HiveTableDDLPreprocessor()
        };

        EntityPreprocessor[] rdbmsPreprocessors = new EntityPreprocessor[] {
                                                                    new RdbmsPreprocessor.RdbmsInstancePreprocessor(),
                                                                    new RdbmsPreprocessor.RdbmsDbPreprocessor(),
                                                                    new RdbmsPreprocessor.RdbmsTablePreprocessor()
       };

        EntityPreprocessor[] s3V2Preprocessors = new EntityPreprocessor[] {
                new AWSS3V2Preprocessor.AWSS3V2DirectoryPreprocessor()
        };

        for (EntityPreprocessor preprocessor : hivePreprocessors) {
            HIVE_PREPROCESSOR_MAP.put(preprocessor.getTypeName(), preprocessor);
        }

        for (EntityPreprocessor preprocessor : rdbmsPreprocessors) {
            RDBMS_PREPROCESSOR_MAP.put(preprocessor.getTypeName(), preprocessor);
        }

        for (EntityPreprocessor preprocessor : s3V2Preprocessors) {
            AWS_S3_V2_PREPROCESSOR_MAP.put(preprocessor.getTypeName(), preprocessor);
        }
    }

    protected EntityPreprocessor(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    public abstract void preprocess(AtlasEntity entity, PreprocessorContext context);


    public static EntityPreprocessor getHivePreprocessor(String typeName) {
        return typeName != null ? HIVE_PREPROCESSOR_MAP.get(typeName) : null;
    }

    public static EntityPreprocessor getRdbmsPreprocessor(String typeName) {
        return typeName != null ? RDBMS_PREPROCESSOR_MAP.get(typeName) : null;
    }

    public static EntityPreprocessor getS3V2Preprocessor(String typeName) {
        return typeName != null ? AWS_S3_V2_PREPROCESSOR_MAP.get(typeName) : null;
    }

    public static String getQualifiedName(AtlasEntity entity) {
        Object obj = entity != null ? entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME) : null;

        return obj != null ? obj.toString() : null;
    }

    public static String getName(AtlasEntity entity) {
        Object obj = entity != null ? entity.getAttribute(ATTRIBUTE_NAME) : null;

        return obj != null ? obj.toString() : null;
    }

    public String getTypeName(Object obj) {
        Object ret = null;

        if (obj instanceof AtlasObjectId) {
            ret = ((AtlasObjectId) obj).getTypeName();
        } else if (obj instanceof Map) {
            ret = ((Map) obj).get(AtlasObjectId.KEY_TYPENAME);
        } else if (obj instanceof AtlasEntity) {
            ret = ((AtlasEntity) obj).getTypeName();
        } else if (obj instanceof AtlasEntityWithExtInfo) {
            ret = ((AtlasEntityWithExtInfo) obj).getEntity().getTypeName();
        }

        return ret != null ? ret.toString() : null;
    }

    public String getQualifiedName(Object obj) {
        Map<String, Object> attributes = null;

        if (obj instanceof AtlasObjectId) {
            attributes = ((AtlasObjectId) obj).getUniqueAttributes();
        } else if (obj instanceof Map) {
            attributes = (Map) ((Map) obj).get(AtlasObjectId.KEY_UNIQUE_ATTRIBUTES);
        } else if (obj instanceof AtlasEntity) {
            attributes = ((AtlasEntity) obj).getAttributes();
        } else if (obj instanceof AtlasEntityWithExtInfo) {
            attributes = ((AtlasEntityWithExtInfo) obj).getEntity().getAttributes();
        }

        Object ret = attributes != null ? attributes.get(ATTRIBUTE_QUALIFIED_NAME) : null;

        return ret != null ? ret.toString() : null;
    }

    public void setObjectIdWithGuid(Object obj, String guid) {
        if (obj instanceof AtlasObjectId) {
            AtlasObjectId objectId = (AtlasObjectId) obj;
            objectId.setGuid(guid);
        } else if (obj instanceof Map) {
            Map map = (Map) obj;
            map.put("guid", guid);
        }
    }

    protected boolean isEmpty(Object obj) {
        return obj == null || ((obj instanceof Collection) && ((Collection) obj).isEmpty());
    }
}


