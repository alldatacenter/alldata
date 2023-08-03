/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.impala.hook;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.atlas.impala.model.ImpalaOperationType;
import org.apache.atlas.impala.model.ImpalaQuery;
import org.apache.atlas.impala.model.LineageVertex;
import org.apache.atlas.impala.model.LineageVertexMetadata;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.commons.lang.StringUtils;


/**
 * Contain the info related to an linear record from Impala
 */
public class AtlasImpalaHookContext {
    public static final char QNAME_SEP_METADATA_NAMESPACE = '@';
    public static final char QNAME_SEP_ENTITY_NAME        = '.';
    public static final char QNAME_SEP_PROCESS            = ':';

    private final ImpalaLineageHook        hook;
    private final ImpalaOperationType      impalaOperation;
    private final ImpalaQuery              lineageQuery;
    private final Map<String, AtlasEntity> qNameEntityMap = new HashMap<>();

    public AtlasImpalaHookContext(ImpalaLineageHook hook, ImpalaOperationType operationType,
        ImpalaQuery lineageQuery) throws Exception {
        this.hook          = hook;
        this.impalaOperation = operationType;
        this.lineageQuery   = lineageQuery;

    }

    public ImpalaQuery getLineageQuery() {
        return lineageQuery;
    }
    public String getQueryStr() { return lineageQuery.getQueryText(); }

    public ImpalaOperationType getImpalaOperationType() {
        return impalaOperation;
    }

    public void putEntity(String qualifiedName, AtlasEntity entity) {
        qNameEntityMap.put(qualifiedName, entity);
    }

    public AtlasEntity getEntity(String qualifiedName) {
        return qNameEntityMap.get(qualifiedName);
    }

    public Collection<AtlasEntity> getEntities() { return qNameEntityMap.values(); }

    public String getMetadataNamespace() {
        return hook.getMetadataNamespace();
    }

    public String getHostName() {
        return hook.getHostName();
    }

    public boolean isConvertHdfsPathToLowerCase() {
        return hook.isConvertHdfsPathToLowerCase();
    }

    public String getQualifiedNameForDb(String dbName) {
        return (dbName + QNAME_SEP_METADATA_NAMESPACE).toLowerCase() + getMetadataNamespace();
    }

    public String getQualifiedNameForTable(String fullTableName) throws IllegalArgumentException {
        if (fullTableName == null) {
            throw new IllegalArgumentException("fullTableName is null");
        }

        int sepPos = fullTableName.lastIndexOf(QNAME_SEP_ENTITY_NAME);

        if (!isSeparatorIndexValid(sepPos)) {
            throw new IllegalArgumentException(fullTableName + " does not contain database name");
        }

        return getQualifiedNameForTable(fullTableName.substring(0, sepPos), fullTableName.substring(sepPos+1));
    }

    public String getQualifiedNameForTable(String dbName, String tableName) {
        return (dbName + QNAME_SEP_ENTITY_NAME + tableName + QNAME_SEP_METADATA_NAMESPACE).toLowerCase() + getMetadataNamespace();
    }

    public String getQualifiedNameForColumn(LineageVertex vertex) {
        // get database name and table name
        LineageVertexMetadata metadata = vertex.getMetadata();

        if (metadata == null) {
            return getQualifiedNameForColumn(vertex.getVertexId());
        }

        String fullTableName = metadata.getTableName();

        if (StringUtils.isEmpty(fullTableName)) {
            throw new IllegalArgumentException("fullTableName in column metadata is null");
        }

        int sepPos = fullTableName.lastIndexOf(QNAME_SEP_ENTITY_NAME);

        if (!isSeparatorIndexValid(sepPos)) {
            throw new IllegalArgumentException(fullTableName + "in column metadata does not contain database name");
        }

        // get pure column name
        String columnName = vertex.getVertexId();
        if (StringUtils.isEmpty(columnName)) {
            throw new IllegalArgumentException("column name in vertexId is null");
        }

        int sepPosLast = columnName.lastIndexOf(QNAME_SEP_ENTITY_NAME);
        if (isSeparatorIndexValid(sepPosLast)) {
            columnName = columnName.substring(sepPosLast+1);
        }

        return getQualifiedNameForColumn(
            fullTableName.substring(0, sepPos),
            fullTableName.substring(sepPos+1),
            columnName);
    }

    public String getQualifiedNameForColumn(String fullColumnName) throws IllegalArgumentException {
        if (fullColumnName == null) {
            throw new IllegalArgumentException("fullColumnName is null");
        }

        int sepPosFirst = fullColumnName.indexOf(QNAME_SEP_ENTITY_NAME);
        int sepPosLast = fullColumnName.lastIndexOf(QNAME_SEP_ENTITY_NAME);

        if (!isSeparatorIndexValid(sepPosFirst) || !isSeparatorIndexValid(sepPosLast) ||
            sepPosFirst == sepPosLast) {
            throw new IllegalArgumentException(
                String.format("fullColumnName {} does not contain database name or table name",
                    fullColumnName));
        }

        return getQualifiedNameForColumn(
            fullColumnName.substring(0, sepPosFirst),
            fullColumnName.substring(sepPosFirst+1, sepPosLast),
            fullColumnName.substring(sepPosLast+1));
    }

    public String getColumnNameOnly(String fullColumnName) throws IllegalArgumentException {
        if (fullColumnName == null) {
            throw new IllegalArgumentException("fullColumnName is null");
        }

        int sepPosLast = fullColumnName.lastIndexOf(QNAME_SEP_ENTITY_NAME);

        if (!isSeparatorIndexValid(sepPosLast)) {
            return fullColumnName;
        }

        return fullColumnName.substring(sepPosLast+1);
    }

    public String getQualifiedNameForColumn(String dbName, String tableName, String columnName) {
        return
            (dbName + QNAME_SEP_ENTITY_NAME  + tableName + QNAME_SEP_ENTITY_NAME +
             columnName + QNAME_SEP_METADATA_NAMESPACE).toLowerCase() + getMetadataNamespace();
    }

    public String getUserName() { return lineageQuery.getUser(); }

    public String getDatabaseNameFromTable(String fullTableName) {
        int sepPos = fullTableName.lastIndexOf(QNAME_SEP_ENTITY_NAME);
        if (isSeparatorIndexValid(sepPos)) {
            return fullTableName.substring(0, sepPos);
        }

        return null;
    }

    public String getTableNameFromColumn(String columnName) {
        int sepPos = columnName.lastIndexOf(QNAME_SEP_ENTITY_NAME);
        if (!isSeparatorIndexValid(sepPos)) {
            return null;
        }

        String tableName = columnName.substring(0, sepPos);
        if (!ImpalaIdentifierParser.isTableNameValid(tableName)) {
            return null;
        }

        return tableName;
    }

    public boolean isSeparatorIndexValid(int index) {
        return index > 0;
    }

}
