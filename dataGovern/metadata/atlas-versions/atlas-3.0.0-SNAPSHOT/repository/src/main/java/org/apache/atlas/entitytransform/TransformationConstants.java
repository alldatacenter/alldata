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

public final class TransformationConstants {
    public static final String HDFS_PATH               = "hdfs_path";
    public static final String HIVE_DATABASE           = "hive_db";
    public static final String HIVE_TABLE              = "hive_table";
    public static final String HIVE_COLUMN             = "hive_column";
    public static final String HIVE_STORAGE_DESCRIPTOR = "hive_storagedesc";

    public static final String NAME_ATTRIBUTE           = "name";
    public static final String QUALIFIED_NAME_ATTRIBUTE = "qualifiedName";
    public static final String CLUSTER_NAME_ATTRIBUTE   = "clusterName";
    public static final String LOCATION_ATTRIBUTE       = "location";
    public static final String PATH_ATTRIBUTE           = "path";

    public static final String HIVE_DB_NAME_ATTRIBUTE         = "hive_db.name";
    public static final String HIVE_DB_CLUSTER_NAME_ATTRIBUTE = "hive_db.clusterName";
    public static final String HIVE_TABLE_NAME_ATTRIBUTE      = "hive_table.name";
    public static final String HIVE_COLUMN_NAME_ATTRIBUTE     = "hive_column.name";
    public static final String HDFS_PATH_NAME_ATTRIBUTE       = "hdfs_path.name";
    public static final String HDFS_PATH_PATH_ATTRIBUTE       = "hdfs_path.path";
    public static final String HDFS_CLUSTER_NAME_ATTRIBUTE    = "hdfs_path.clusterName";
    public static final String HIVE_STORAGE_DESC_LOCATION_ATTRIBUTE = "hive_storagedesc.location";

    public static final char   TYPE_NAME_ATTRIBUTE_NAME_SEP = '.';
    public static final char   CLUSTER_DELIMITER            = '@';
    public static final char   DATABASE_DELIMITER           = '.';
    public static final char   PATH_DELIMITER               = '/';
    public static final String HIVE_STORAGEDESC_SUFFIX      = "_storage";


}
