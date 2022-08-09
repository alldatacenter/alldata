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

package org.apache.atlas.impala.hook.events;

import static org.apache.atlas.impala.hook.AtlasImpalaHookContext.QNAME_SEP_PROCESS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.atlas.impala.hook.AtlasImpalaHookContext;
import org.apache.atlas.impala.hook.ImpalaOperationParser;
import org.apache.atlas.impala.model.ImpalaDataType;
import org.apache.atlas.impala.model.ImpalaNode;
import org.apache.atlas.impala.model.ImpalaOperationType;
import org.apache.atlas.impala.model.ImpalaVertexType;
import org.apache.atlas.impala.model.LineageVertex;
import org.apache.atlas.impala.model.LineageVertexMetadata;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.type.AtlasTypeUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The base class for generating notification event to Atlas server
 * Most code is copied from BaseHiveEvent to avoid depending on org.apache.atlas.hive.hook
 */
public abstract class BaseImpalaEvent {
    private static final Logger LOG = LoggerFactory.getLogger(BaseImpalaEvent.class);

    // Impala should re-use the same entity type as hive. So Hive and Impala can operate on same
    // database or table
    public static final String HIVE_TYPE_DB                        = "hive_db";
    public static final String HIVE_TYPE_TABLE                     = "hive_table";
    public static final String HIVE_TYPE_COLUMN                    = "hive_column";

    public static final String ATTRIBUTE_QUALIFIED_NAME            = "qualifiedName";
    public static final String ATTRIBUTE_NAME                      = "name";
    public static final String ATTRIBUTE_OWNER                     = "owner";
    public static final String ATTRIBUTE_CLUSTER_NAME              = "clusterName";
    public static final String ATTRIBUTE_CREATE_TIME               = "createTime";
    public static final String ATTRIBUTE_LAST_ACCESS_TIME          = "lastAccessTime";
    public static final String ATTRIBUTE_DB                        = "db";
    public static final String ATTRIBUTE_COLUMNS                   = "columns";
    public static final String ATTRIBUTE_TABLE                     = "table";
    public static final String ATTRIBUTE_INPUTS                    = "inputs";
    public static final String ATTRIBUTE_OUTPUTS                   = "outputs";
    public static final String ATTRIBUTE_OPERATION_TYPE            = "operationType";
    public static final String ATTRIBUTE_START_TIME                = "startTime";
    public static final String ATTRIBUTE_USER_NAME                 = "userName";
    public static final String ATTRIBUTE_QUERY_TEXT                = "queryText";
    public static final String ATTRIBUTE_PROCESS                   = "process";
    public static final String ATTRIBUTE_PROCESS_EXECUTIONS        = "processExecutions";
    public static final String ATTRIBUTE_QUERY_ID                  = "queryId";
    public static final String ATTRIBUTE_QUERY_PLAN                = "queryPlan";
    public static final String ATTRIBUTE_END_TIME                  = "endTime";
    public static final String ATTRIBUTE_RECENT_QUERIES            = "recentQueries";
    public static final String ATTRIBUTE_QUERY                     = "query";
    public static final String ATTRIBUTE_DEPENDENCY_TYPE           = "dependencyType";
    public static final String ATTRIBUTE_HOSTNAME                  = "hostName";
    public static final String EMPTY_ATTRIBUTE_VALUE               = "";
    public static final String ATTRIBUTE_EXEC_TIME                 = "execTime";
    public static final String ATTRIBUTE_DDL_QUERIES               = "ddlQueries";
    public static final String ATTRIBUTE_SERVICE_TYPE              = "serviceType";
    public static final long   MILLIS_CONVERT_FACTOR               = 1000;

    protected final AtlasImpalaHookContext context;
    protected final Map<String, ImpalaNode> vertexNameMap;
    protected final Map<Long, LineageVertex> verticesMap;

    public BaseImpalaEvent(AtlasImpalaHookContext context) {

        this.context   = context;
        vertexNameMap  = new HashMap<>();
        verticesMap    = new HashMap<>();
    }

    public AtlasImpalaHookContext getContext() {
        return context;
    }

    public abstract List<HookNotification> getNotificationMessages() throws Exception;

    public String getUserName() { return context.getUserName(); }

    public String getTableNameFromVertex(LineageVertex vertex) {
        if (vertex.getVertexType() == ImpalaVertexType.COLUMN) {
            LineageVertexMetadata metadata = vertex.getMetadata();

            if (metadata != null) {
                return metadata.getTableName();
            }
        }

        return getTableNameFromColumn(vertex.getVertexId());
    }

    public String getTableNameFromColumn(String columnName) {
        return context.getTableNameFromColumn(columnName);
    }

    public String getQualifiedName(ImpalaNode node) throws IllegalArgumentException {

        return getQualifiedName(node.getOwnVertex());
    }

    public String getQualifiedName(LineageVertex node) throws IllegalArgumentException {
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }

        ImpalaVertexType nodeType = node.getVertexType();

        if (nodeType == null) {
            if (node.getVertexId() != null) {
                LOG.warn("null qualified name for type: null and name: {}", node.getVertexId());
            }
            return null;
        }

        if (node.getVertexId() == null) {
            LOG.warn("null qualified name for type: {} and name: null", nodeType);
            return null;
        }

        switch (nodeType) {
            case DATABASE:
                return context.getQualifiedNameForDb(node.getVertexId());

            case TABLE:
                return context.getQualifiedNameForTable(node.getVertexId());

            case COLUMN:
                return context.getQualifiedNameForColumn(node);

            default:
                LOG.warn("null qualified name for type: {} and name: {}", nodeType, node.getVertexId());
                return null;
        }
    }

    static final class AtlasEntityComparator implements Comparator<AtlasEntity> {
        @Override
        public int compare(AtlasEntity entity1, AtlasEntity entity2) {
            String name1 = (String)entity1.getAttribute(ATTRIBUTE_QUALIFIED_NAME);
            String name2 = (String)entity2.getAttribute(ATTRIBUTE_QUALIFIED_NAME);

            if (name1 == null) {
                return -1;
            }

            if (name2 == null) {
                return 1;
            }

            return name1.toLowerCase().compareTo(name2.toLowerCase());
        }
    }

    static final Comparator<AtlasEntity> entityComparator = new AtlasEntityComparator();

    protected String getQualifiedName(List<AtlasEntity> inputs, List<AtlasEntity> outputs) throws Exception {
        ImpalaOperationType operation = context.getImpalaOperationType();

        if (operation == ImpalaOperationType.CREATEVIEW ||
            operation == ImpalaOperationType.CREATETABLE_AS_SELECT ||
            operation == ImpalaOperationType.ALTERVIEW_AS) {
            List<? extends AtlasEntity> sortedEntities = new ArrayList<>(outputs);

            Collections.sort(sortedEntities, entityComparator);

            for (AtlasEntity entity : sortedEntities) {
                if (entity.getTypeName().equalsIgnoreCase(HIVE_TYPE_TABLE)) {
                    Long createTime = (Long)entity.getAttribute(ATTRIBUTE_CREATE_TIME);

                    return (String)entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME) + QNAME_SEP_PROCESS + createTime;
                }
            }
        }

        if (operation != ImpalaOperationType.QUERY) {
            String errorMessage = String.format("Expect operation to be QUERY, but get unexpected operation type {}", operation.name());
            LOG.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        // construct qualified name for QUERY
        String qualifiedName = null;
        String operationName = operation.toString();

        if (operationName != null) {
            StringBuilder sb = new StringBuilder(operationName);

            addToProcessQualifiedName(sb, inputs, false);
            sb.append("->");
            addToProcessQualifiedName(sb, outputs, true);

            qualifiedName = sb.toString();
        }


        return qualifiedName;
    }

    protected void addToProcessQualifiedName(StringBuilder processQualifiedName, Collection<? extends AtlasEntity> entities, boolean isOutput) {
        if (entities == null) {
            return;
        }

        ImpalaOperationType         operation      = context.getImpalaOperationType();
        String                      queryText      = context.getQueryStr();
        List<? extends AtlasEntity> sortedEntities = new ArrayList<>(entities);

        Collections.sort(sortedEntities, entityComparator);

        Set<String> dataSetsProcessed = new HashSet<>();

        for (AtlasEntity entity : sortedEntities) {
            String qualifiedName = null;
            long   createTime    = 0;

            qualifiedName = (String)entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME);

            if (entity.getTypeName().equalsIgnoreCase(HIVE_TYPE_TABLE)) {
                Long createTimeObj = (Long)entity.getAttribute(ATTRIBUTE_CREATE_TIME);
                if (createTimeObj != null) {
                    createTime = createTimeObj;
                }
            }

            if (qualifiedName == null || !dataSetsProcessed.add(qualifiedName)) {
                continue;
            }

            if (isOutput) {
                boolean             addWriteType = false;
                ImpalaOperationType subType      = ImpalaOperationParser.getImpalaOperationSubType(operation, queryText);

                    switch (subType) {
                        // Impala does not generate lineage for UPDATE and DELETE
                        case INSERT:
                        case INSERT_OVERWRITE:
                            addWriteType = true;
                            break;
                    }

                    if (addWriteType) {
                        processQualifiedName.append(QNAME_SEP_PROCESS).append(subType.name());
                    }
            }

            processQualifiedName.append(QNAME_SEP_PROCESS).append(qualifiedName.toLowerCase().replaceAll("/", ""));

            if (createTime != 0) {
                processQualifiedName.append(QNAME_SEP_PROCESS).append(createTime);
            }
        }
    }

    protected AtlasEntity getInputOutputEntity(ImpalaNode node, AtlasEntityExtInfo entityExtInfo) throws Exception {
        AtlasEntity ret = null;

        switch(node.getNodeType()) {
            case TABLE:
            case PARTITION:
            case DFS_DIR: {
                ret = toAtlasEntity(node, entityExtInfo);
            }
            break;
        }

        return ret;
    }

    protected AtlasEntity toAtlasEntity(ImpalaNode node, AtlasEntityExtInfo entityExtInfo) throws Exception {
        AtlasEntity ret = null;

        switch (node.getNodeType()) {
            case DATABASE:
                ret = toDbEntity(node);
                break;

            case TABLE:
            case PARTITION:
                ret = toTableEntity(node, entityExtInfo);
                break;

            default:
                break;
        }

        return ret;
    }

    protected AtlasEntity toDbEntity(ImpalaNode db) throws Exception {
        return toDbEntity(db.getNodeName());
    }

    protected AtlasEntity toDbEntity(String dbName) throws Exception {
        String dbQualifiedName = context.getQualifiedNameForDb(dbName);
        AtlasEntity ret = context.getEntity(dbQualifiedName);

        if (ret == null) {
            ret = new AtlasEntity(HIVE_TYPE_DB);

            // Impala hook should not send metadata entities. set 'guid' to null - which will:
            //  - result in this entity to be not included in 'referredEntities'
            //  - cause Atlas server to resolve the entity by its qualifiedName
            ret.setGuid(null);

            ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, dbQualifiedName);
            ret.setAttribute(ATTRIBUTE_NAME, dbName.toLowerCase());
            ret.setAttribute(ATTRIBUTE_CLUSTER_NAME, context.getMetadataNamespace());

            context.putEntity(dbQualifiedName, ret);
        }

        return ret;
    }

    protected AtlasEntityWithExtInfo toTableEntity(ImpalaNode table) throws Exception {
        AtlasEntityWithExtInfo ret = new AtlasEntityWithExtInfo();

        AtlasEntity entity = toTableEntity(table, ret);

        if (entity != null) {
            ret.setEntity(entity);
        } else {
            ret = null;
        }

        return ret;
    }

    protected AtlasEntity toTableEntity(ImpalaNode table, AtlasEntitiesWithExtInfo entities) throws Exception {
        AtlasEntity ret = toTableEntity(table, (AtlasEntityExtInfo) entities);

        if (ret != null) {
            entities.addEntity(ret);
        }

        return ret;
    }

    protected AtlasEntity toTableEntity(ImpalaNode table, AtlasEntityExtInfo entityExtInfo) throws Exception {
        if ((table == null) || (table.getNodeName() == null)) {
            throw new IllegalArgumentException("table is null or its name is null");
        }

        String dbName = context.getDatabaseNameFromTable(table.getNodeName());
        if (dbName == null) {
            throw new IllegalArgumentException(String.format("db name is null for table: {}", table.getNodeName()));
        }

        AtlasEntity dbEntity = toDbEntity(dbName);

        if (entityExtInfo != null) {
            if (dbEntity != null) {
                entityExtInfo.addReferredEntity(dbEntity);
            }
        }

        AtlasEntity ret = toTableEntity(getObjectId(dbEntity), table, entityExtInfo);

        return ret;
    }

    protected AtlasEntity toTableEntity(AtlasObjectId dbId, ImpalaNode table, AtlasEntityExtInfo entityExtInfo) throws Exception {
        String  tblQualifiedName = getQualifiedName(table);
        AtlasEntity ret = context.getEntity(tblQualifiedName);

        if (ret != null) {
            return ret;
        }

        // a table created in Impala still uses HIVE_TYPE_TABLE to allow both Impala and Hive operate
        // on the same table
        ret = new AtlasEntity(HIVE_TYPE_TABLE);

        // Impala hook should not send meta data entity to Atlas. set 'guid' to null - which will:
        //  - result in this entity to be not included in 'referredEntities'
        //  - cause Atlas server to resolve the entity by its qualifiedName
        // TODO: enable this once HMS hook is in. Disable this before that.
        ret.setGuid(null);

        long createTime     = getTableCreateTime(table);
        long lastAccessTime = createTime;

        ret.setAttribute(ATTRIBUTE_DB, dbId);
        ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, tblQualifiedName);
        ret.setAttribute(ATTRIBUTE_NAME, table.getNodeName().toLowerCase());

        // just fake it. It should not be sent to Atlas once HMS hook is in
        ret.setAttribute(ATTRIBUTE_OWNER, getUserName());

        ret.setAttribute(ATTRIBUTE_CREATE_TIME, createTime);
        ret.setAttribute(ATTRIBUTE_LAST_ACCESS_TIME, lastAccessTime);

        AtlasObjectId     tableId       = getObjectId(ret);
        List<AtlasEntity> columns       = getColumnEntities(tableId, table);

        if (entityExtInfo != null) {
            if (columns != null) {
                for (AtlasEntity column : columns) {
                    entityExtInfo.addReferredEntity(column);
                }
            }
        }

        ret.setAttribute(ATTRIBUTE_COLUMNS, getObjectIds(columns));


        context.putEntity(tblQualifiedName, ret);

        return ret;
    }

    public static AtlasObjectId getObjectId(AtlasEntity entity) {
        String        qualifiedName = (String) entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME);
        AtlasObjectId ret           = new AtlasObjectId(entity.getGuid(), entity.getTypeName(), Collections
            .singletonMap(ATTRIBUTE_QUALIFIED_NAME, qualifiedName));

        return ret;
    }

    public static List<AtlasObjectId> getObjectIds(List<AtlasEntity> entities) {
        final List<AtlasObjectId> ret;

        if (CollectionUtils.isNotEmpty(entities)) {
            ret = new ArrayList<>(entities.size());

            for (AtlasEntity entity : entities) {
                ret.add(getObjectId(entity));
            }
        } else {
            ret = Collections.emptyList();
        }

        return ret;
    }

    /**
     * return the createTime of the table.
     * @param table
     * @return the createTime of the table. Its unit is in milliseconds.
     */
    public static long getTableCreateTime(ImpalaNode table) {
        return getTableCreateTime(table.getOwnVertex());
    }

    public static long getTableCreateTime(LineageVertex tableVertex) {
        Long createTime = tableVertex.getCreateTime();
        if (createTime != null) {
            // the time unit of vertex is in seconds. Convert to milliseconds before sending to Atlas.
            return createTime.longValue() * MILLIS_CONVERT_FACTOR;
        } else {
            return System.currentTimeMillis();
        }
    }

    protected List<AtlasEntity> getColumnEntities(AtlasObjectId tableId, ImpalaNode table) {
        List<AtlasEntity> ret          = new ArrayList<>();

        for (ImpalaNode childNode : table.getChildren().values()) {
            String      colQualifiedName = getQualifiedName(childNode);
            AtlasEntity column           = context.getEntity(colQualifiedName);

            if (column == null) {
                column = new AtlasEntity(HIVE_TYPE_COLUMN);

                // if column's table was sent in an earlier notification, set 'guid' to null - which will:
                //  - result in this entity to be not included in 'referredEntities'
                //  - cause Atlas server to resolve the entity by its qualifiedName
                // TODO: enable this once HMS hook is in. Disable this before that.
                column.setGuid(null);

                column.setAttribute(ATTRIBUTE_TABLE, tableId);
                column.setAttribute(ATTRIBUTE_QUALIFIED_NAME, colQualifiedName);
                column.setAttribute(ATTRIBUTE_NAME, context.getColumnNameOnly(childNode.getNodeName()));

                // just fake it. It should not be sent to Atlas once HMS hook is in
                column.setAttribute(ATTRIBUTE_OWNER, getUserName());

                context.putEntity(colQualifiedName, column);
            }

            ret.add(column);
        }

        return ret;
    }

    protected AtlasEntity getImpalaProcessEntity(List<AtlasEntity> inputs, List<AtlasEntity> outputs) throws Exception {
        AtlasEntity         ret           = new AtlasEntity(ImpalaDataType.IMPALA_PROCESS.getName());
        String              queryStr      = context.getQueryStr();

        if (queryStr != null) {
            queryStr = queryStr.toLowerCase().trim();
        }

        Long startTime = getQueryStartTime();
        Long endTime   = getQueryEndTime();

        ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, getQualifiedName(inputs, outputs));
        ret.setAttribute(ATTRIBUTE_INPUTS, getObjectIds(inputs));
        ret.setAttribute(ATTRIBUTE_OUTPUTS,  getObjectIds(outputs));
        ret.setAttribute(ATTRIBUTE_NAME, queryStr);
        ret.setAttribute(ATTRIBUTE_OPERATION_TYPE, context.getImpalaOperationType());

        // We are setting an empty value to these attributes, since now we have a new entity type called impala process
        // execution which captures these values. We have to set empty values here because these attributes are
        // mandatory attributes for impala process entity type.
        ret.setAttribute(ATTRIBUTE_START_TIME, startTime);
        ret.setAttribute(ATTRIBUTE_END_TIME, endTime);
        ret.setAttribute(ATTRIBUTE_USER_NAME, EMPTY_ATTRIBUTE_VALUE);
        ret.setAttribute(ATTRIBUTE_QUERY_TEXT, EMPTY_ATTRIBUTE_VALUE);
        ret.setAttribute(ATTRIBUTE_QUERY_ID, EMPTY_ATTRIBUTE_VALUE);
        ret.setAttribute(ATTRIBUTE_QUERY_PLAN, "Not Supported");
        ret.setAttribute(ATTRIBUTE_RECENT_QUERIES, Collections.singletonList(queryStr));

        return ret;
    }

    protected AtlasEntity getImpalaProcessExecutionEntity(AtlasEntity impalaProcess) throws Exception {
        AtlasEntity ret         = new AtlasEntity(ImpalaDataType.IMPALA_PROCESS_EXECUTION.getName());
        String      queryStr    = context.getQueryStr();

        if (queryStr != null) {
            queryStr = queryStr.toLowerCase().trim();
        }

        Long startTime = getQueryStartTime();
        Long endTime = getQueryEndTime();

        ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, impalaProcess.getAttribute(ATTRIBUTE_QUALIFIED_NAME).toString() +
            QNAME_SEP_PROCESS + startTime.toString() +
            QNAME_SEP_PROCESS + endTime.toString());
        ret.setAttribute(ATTRIBUTE_NAME, queryStr + QNAME_SEP_PROCESS + startTime);
        ret.setAttribute(ATTRIBUTE_START_TIME, startTime);
        ret.setAttribute(ATTRIBUTE_END_TIME, endTime);
        ret.setAttribute(ATTRIBUTE_USER_NAME, getUserName());
        ret.setAttribute(ATTRIBUTE_QUERY_TEXT, queryStr);
        ret.setAttribute(ATTRIBUTE_QUERY_ID, context.getLineageQuery().getQueryId());
        ret.setAttribute(ATTRIBUTE_QUERY_PLAN, "Not Supported");
        ret.setAttribute(ATTRIBUTE_HOSTNAME, context.getHostName());
        ret.setRelationshipAttribute(ATTRIBUTE_PROCESS, AtlasTypeUtil.toAtlasRelatedObjectId(impalaProcess));

        return ret;
    }

    protected Long getQueryStartTime() {
        return context.getLineageQuery().getTimestamp() * BaseImpalaEvent.MILLIS_CONVERT_FACTOR;
    }

    protected Long getQueryEndTime() {
        return context.getLineageQuery().getEndTime() * BaseImpalaEvent.MILLIS_CONVERT_FACTOR;
    }

    protected void addProcessedEntities(AtlasEntitiesWithExtInfo entitiesWithExtInfo) {
        for (AtlasEntity entity : context.getEntities()) {
            entitiesWithExtInfo.addReferredEntity(entity);
        }

        entitiesWithExtInfo.compact();
    }

    // The unit of createTime in vertex is in seconds. So the returned value is
    // time in seconds.
    protected Long getCreateTimeInVertex(LineageVertex vertex) {
        if (vertex == null) {
            return System.currentTimeMillis() / MILLIS_CONVERT_FACTOR;
        }

        Long createTime = vertex.getCreateTime();

        if (createTime != null) {
            return createTime;
        }

        if (vertex.getVertexType() == ImpalaVertexType.COLUMN) {
            LineageVertexMetadata metadata = vertex.getMetadata();

            if (metadata != null) {
                return metadata.getTableCreateTime();
            }
        }

        return System.currentTimeMillis() / MILLIS_CONVERT_FACTOR;
    }

    protected ImpalaNode createTableNode(String tableName, Long createTime) {
        // the created table vertex does not have its Id set as it is not referred in edge
        LineageVertex tableVertex = new LineageVertex();
        tableVertex.setVertexType(ImpalaVertexType.TABLE);
        tableVertex.setVertexId(tableName);
        tableVertex.setCreateTime(createTime);
        return new ImpalaNode(tableVertex);
    }

    protected AtlasEntity createHiveDDLEntity(AtlasEntity dbOrTable) {
        return createHiveDDLEntity(dbOrTable, true);
    }

    protected AtlasEntity createHiveDDLEntity(AtlasEntity dbOrTable, boolean excludeEntityGuid) {
        AtlasObjectId objId   = BaseImpalaEvent.getObjectId(dbOrTable);
        AtlasEntity   hiveDDL = null;

        if (excludeEntityGuid) {
            objId.setGuid(null);
        }

        if (StringUtils.equals(objId.getTypeName(), HIVE_TYPE_DB)) {
            hiveDDL = new AtlasEntity(ImpalaDataType.HIVE_DB_DDL.getName(), ATTRIBUTE_DB, objId);
        } else if (StringUtils.equals(objId.getTypeName(), HIVE_TYPE_TABLE)) {
            hiveDDL = new AtlasEntity(ImpalaDataType.HIVE_TABLE_DDL.getName(), ATTRIBUTE_TABLE, objId);
        }

        if (hiveDDL != null) {
            hiveDDL.setAttribute(ATTRIBUTE_SERVICE_TYPE, "impala");
            hiveDDL.setAttribute(ATTRIBUTE_EXEC_TIME, getQueryStartTime());
            hiveDDL.setAttribute(ATTRIBUTE_QUERY_TEXT, context.getQueryStr());
            hiveDDL.setAttribute(ATTRIBUTE_USER_NAME, getUserName());
            hiveDDL.setAttribute(ATTRIBUTE_NAME, context.getQueryStr() + QNAME_SEP_PROCESS + getQueryStartTime().toString());
            hiveDDL.setAttribute(ATTRIBUTE_QUALIFIED_NAME, hiveDDL.getAttribute(ATTRIBUTE_NAME));
        }

        return hiveDDL;
    }

    protected boolean isDdlOperation() {
        return (context.getImpalaOperationType().equals(ImpalaOperationType.CREATEVIEW)
            || context.getImpalaOperationType().equals(ImpalaOperationType.ALTERVIEW_AS)
            || context.getImpalaOperationType().equals(ImpalaOperationType.CREATETABLE_AS_SELECT));
    }
}
