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

package org.apache.atlas.hbase.bridge;

import org.apache.atlas.AtlasConstants;
import org.apache.atlas.hbase.model.HBaseOperationContext;
import org.apache.atlas.hbase.model.HBaseDataTypes;
import org.apache.atlas.hook.AtlasHook;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.notification.HookNotification.EntityCreateRequestV2;
import org.apache.atlas.model.notification.HookNotification.EntityDeleteRequestV2;
import org.apache.atlas.model.notification.HookNotification.EntityUpdateRequestV2;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.coprocessor.MasterCoprocessorEnvironment;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.security.User;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.repository.Constants.HBASE_SOURCE;

// This will register Hbase entities into Atlas
public class HBaseAtlasHook extends AtlasHook {
    private static final Logger LOG = LoggerFactory.getLogger(HBaseAtlasHook.class);

    
    public static final String ATTR_DESCRIPTION           = "description";
    public static final String ATTR_ATLAS_ENDPOINT        = "atlas.rest.address";
    public static final String ATTR_PARAMETERS            = "parameters";
    public static final String ATTR_URI                   = "uri";
    public static final String ATTR_NAMESPACE             = "namespace";
    public static final String ATTR_TABLE                 = "table";
    public static final String ATTR_COLUMNFAMILIES        = "column_families";
    public static final String ATTR_CREATE_TIME           = "createTime";
    public static final String ATTR_MODIFIED_TIME         = "modifiedTime";
    public static final String ATTR_OWNER                 = "owner";
    public static final String ATTR_NAME                  = "name";

    // column addition metadata
    public static final String ATTR_TABLE_MAX_FILESIZE              = "maxFileSize";
    public static final String ATTR_TABLE_ISREADONLY                = "isReadOnly";
    public static final String ATTR_TABLE_ISCOMPACTION_ENABLED      = "isCompactionEnabled";
    public static final String ATTR_TABLE_ISNORMALIZATION_ENABLED   = "isNormalizationEnabled";
    public static final String ATTR_TABLE_REPLICATION_PER_REGION    = "replicasPerRegion";
    public static final String ATTR_TABLE_DURABLILITY               = "durability";
    public static final String ATTR_TABLE_NORMALIZATION_ENABLED     = "isNormalizationEnabled";

    // column family additional metadata
    public static final String ATTR_CF_BLOOMFILTER_TYPE             = "bloomFilterType";
    public static final String ATTR_CF_COMPRESSION_TYPE             = "compressionType";
    public static final String ATTR_CF_COMPACTION_COMPRESSION_TYPE  = "compactionCompressionType";
    public static final String ATTR_CF_ENCRYPTION_TYPE              = "encryptionType";
    public static final String ATTR_CF_INMEMORY_COMPACTION_POLICY   = "inMemoryCompactionPolicy";
    public static final String ATTR_CF_KEEP_DELETE_CELLS            = "keepDeletedCells";
    public static final String ATTR_CF_MAX_VERSIONS                 = "maxVersions";
    public static final String ATTR_CF_MIN_VERSIONS                 = "minVersions";
    public static final String ATTR_CF_DATA_BLOCK_ENCODING          = "dataBlockEncoding";
    public static final String ATTR_CF_STORAGE_POLICY               = "StoragePolicy";
    public static final String ATTR_CF_TTL                          = "ttl";
    public static final String ATTR_CF_BLOCK_CACHE_ENABLED          = "blockCacheEnabled";
    public static final String ATTR_CF_CACHED_BLOOM_ON_WRITE        = "cacheBloomsOnWrite";
    public static final String ATTR_CF_CACHED_DATA_ON_WRITE         = "cacheDataOnWrite";
    public static final String ATTR_CF_CACHED_INDEXES_ON_WRITE      = "cacheIndexesOnWrite";
    public static final String ATTR_CF_EVICT_BLOCK_ONCLOSE          = "evictBlocksOnClose";
    public static final String ATTR_CF_PREFETCH_BLOCK_ONOPEN        = "prefetchBlocksOnOpen";
    public static final String ATTR_CF_NEW_VERSION_BEHAVIOR         = "newVersionBehavior";
    public static final String ATTR_CF_MOB_ENABLED                  = "isMobEnabled";
    public static final String ATTR_CF_MOB_COMPATCTPARTITION_POLICY = "mobCompactPartitionPolicy";

    public static final String HBASE_NAMESPACE_QUALIFIED_NAME            = "%s@%s";
    public static final String HBASE_TABLE_QUALIFIED_NAME_FORMAT         = "%s:%s@%s";
    public static final String HBASE_COLUMN_FAMILY_QUALIFIED_NAME_FORMAT = "%s:%s.%s@%s";

    private static final String REFERENCEABLE_ATTRIBUTE_NAME = "qualifiedName";

    public static final String RELATIONSHIP_HBASE_TABLE_COLUMN_FAMILIES = "hbase_table_column_families";
    public static final String RELATIONSHIP_HBASE_TABLE_NAMESPACE = "hbase_table_namespace";

    private static volatile HBaseAtlasHook me;

    public enum OPERATION {
        CREATE_NAMESPACE("create_namespace"),
        ALTER_NAMESPACE("alter_namespace"),
        DELETE_NAMESPACE("delete_namespace"),
        CREATE_TABLE("create_table"),
        ALTER_TABLE("alter_table"),
        DELETE_TABLE("delete_table"),
        CREATE_COLUMN_FAMILY("create_column_Family"),
        ALTER_COLUMN_FAMILY("alter_column_Family"),
        DELETE_COLUMN_FAMILY("delete_column_Family");

        private final String name;

        OPERATION(String s) {
            name = s;
        }

        public String getName() {
            return name;
        }
    }

    public static HBaseAtlasHook getInstance() {
        HBaseAtlasHook ret = me;

        if (ret == null) {
            try {
                synchronized (HBaseAtlasHook.class) {
                    ret = me;

                    if (ret == null) {
                        me = ret = new HBaseAtlasHook();
                    }
                }
            } catch (Exception e) {
                LOG.error("Caught exception instantiating the Atlas HBase hook.", e);
            }
        }

        return ret;
    }

    public HBaseAtlasHook() {
    }

    public void createAtlasInstances(HBaseOperationContext hbaseOperationContext) {
        OPERATION operation = hbaseOperationContext.getOperation();

        LOG.info("HBaseAtlasHook(operation={})", operation);

        switch (operation) {
            case CREATE_NAMESPACE:
            case ALTER_NAMESPACE:
                createOrUpdateNamespaceInstance(hbaseOperationContext);
                break;
            case DELETE_NAMESPACE:
                deleteNameSpaceInstance(hbaseOperationContext);
                break;
            case CREATE_TABLE:
            case ALTER_TABLE:
                createOrUpdateTableInstance(hbaseOperationContext);
                break;
            case DELETE_TABLE:
                deleteTableInstance(hbaseOperationContext);
                break;
            case CREATE_COLUMN_FAMILY:
            case ALTER_COLUMN_FAMILY:
                createOrUpdateColumnFamilyInstance(hbaseOperationContext);
                break;
            case DELETE_COLUMN_FAMILY:
                deleteColumnFamilyInstance(hbaseOperationContext);
                break;
        }
    }

    private void createOrUpdateNamespaceInstance(HBaseOperationContext hbaseOperationContext) {
        AtlasEntity nameSpace = buildNameSpace(hbaseOperationContext);

        switch (hbaseOperationContext.getOperation()) {
            case CREATE_NAMESPACE:
                LOG.info("Create NameSpace {}", nameSpace.getAttribute(REFERENCEABLE_ATTRIBUTE_NAME));

                hbaseOperationContext.addMessage(new EntityCreateRequestV2(hbaseOperationContext.getUser(), new AtlasEntitiesWithExtInfo(nameSpace)));
                break;

            case ALTER_NAMESPACE:
                LOG.info("Modify NameSpace {}", nameSpace.getAttribute(REFERENCEABLE_ATTRIBUTE_NAME));

                hbaseOperationContext.addMessage(new EntityUpdateRequestV2(hbaseOperationContext.getUser(), new AtlasEntitiesWithExtInfo(nameSpace)));
                break;
        }
    }

    private void deleteNameSpaceInstance(HBaseOperationContext hbaseOperationContext) {
        String        nameSpaceQName = getNameSpaceQualifiedName(getMetadataNamespace(), hbaseOperationContext.getNameSpace());
        AtlasObjectId nameSpaceId    = new AtlasObjectId(HBaseDataTypes.HBASE_NAMESPACE.getName(), REFERENCEABLE_ATTRIBUTE_NAME, nameSpaceQName);

        LOG.info("Delete NameSpace {}", nameSpaceQName);

        hbaseOperationContext.addMessage(new EntityDeleteRequestV2(hbaseOperationContext.getUser(), Collections.singletonList(nameSpaceId)));
    }

    private void createOrUpdateTableInstance(HBaseOperationContext hbaseOperationContext) {
        AtlasEntity       nameSpace      = buildNameSpace(hbaseOperationContext);
        AtlasEntity       table          = buildTable(hbaseOperationContext, nameSpace);
        List<AtlasEntity> columnFamilies = buildColumnFamilies(hbaseOperationContext, nameSpace, table);

        table.setRelationshipAttribute(ATTR_COLUMNFAMILIES, AtlasTypeUtil.getAtlasRelatedObjectIds(columnFamilies, RELATIONSHIP_HBASE_TABLE_COLUMN_FAMILIES));

        AtlasEntitiesWithExtInfo entities = new AtlasEntitiesWithExtInfo(table);

        entities.addReferredEntity(nameSpace);

        if (CollectionUtils.isNotEmpty(columnFamilies)) {
            for (AtlasEntity columnFamily : columnFamilies) {
                entities.addReferredEntity(columnFamily);
            }
        }

        switch (hbaseOperationContext.getOperation()) {
            case CREATE_TABLE:
                LOG.info("Create Table {}", table.getAttribute(REFERENCEABLE_ATTRIBUTE_NAME));

                hbaseOperationContext.addMessage(new EntityCreateRequestV2(hbaseOperationContext.getUser(), entities));
                break;

            case ALTER_TABLE:
                LOG.info("Modify Table {}", table.getAttribute(REFERENCEABLE_ATTRIBUTE_NAME));

                hbaseOperationContext.addMessage(new EntityUpdateRequestV2(hbaseOperationContext.getUser(), entities));
                break;
        }
    }

    private void deleteTableInstance(HBaseOperationContext hbaseOperationContext) {
        TableName tableName     = hbaseOperationContext.getTableName();
        String    nameSpaceName = tableName.getNamespaceAsString();

        if (nameSpaceName == null) {
            nameSpaceName = tableName.getNameWithNamespaceInclAsString();
        }

        String        tableNameStr = tableName.getNameAsString();
        String        tableQName   = getTableQualifiedName(getMetadataNamespace(), nameSpaceName, tableNameStr);
        AtlasObjectId tableId      = new AtlasObjectId(HBaseDataTypes.HBASE_TABLE.getName(), REFERENCEABLE_ATTRIBUTE_NAME, tableQName);

        LOG.info("Delete Table {}", tableQName);

        hbaseOperationContext.addMessage(new EntityDeleteRequestV2(hbaseOperationContext.getUser(), Collections.singletonList(tableId)));
    }

    private void createOrUpdateColumnFamilyInstance(HBaseOperationContext hbaseOperationContext) {
        AtlasEntity nameSpace    = buildNameSpace(hbaseOperationContext);
        AtlasEntity table        = buildTable(hbaseOperationContext, nameSpace);
        AtlasEntity columnFamily = buildColumnFamily(hbaseOperationContext, hbaseOperationContext.gethColumnDescriptor(), nameSpace, table);

        AtlasEntitiesWithExtInfo entities = new AtlasEntitiesWithExtInfo(columnFamily);

        entities.addReferredEntity(nameSpace);
        entities.addReferredEntity(table);

        switch (hbaseOperationContext.getOperation()) {
            case CREATE_COLUMN_FAMILY:
                LOG.info("Create ColumnFamily {}", columnFamily.getAttribute(REFERENCEABLE_ATTRIBUTE_NAME));

                hbaseOperationContext.addMessage(new EntityCreateRequestV2(hbaseOperationContext.getUser(), entities));
                break;

            case ALTER_COLUMN_FAMILY:
                LOG.info("Alter ColumnFamily {}", columnFamily.getAttribute(REFERENCEABLE_ATTRIBUTE_NAME));

                hbaseOperationContext.addMessage(new EntityUpdateRequestV2(hbaseOperationContext.getUser(), entities));
                break;
        }
    }

    private void deleteColumnFamilyInstance(HBaseOperationContext hbaseOperationContext) {
        TableName tableName     = hbaseOperationContext.getTableName();
        String    nameSpaceName = tableName.getNamespaceAsString();

        if (nameSpaceName == null) {
            nameSpaceName = tableName.getNameWithNamespaceInclAsString();
        }

        String        tableNameStr      = tableName.getNameAsString();
        String        columnFamilyName  = hbaseOperationContext.getColummFamily();
        String        columnFamilyQName = getColumnFamilyQualifiedName(getMetadataNamespace(), nameSpaceName, tableNameStr, columnFamilyName);
        AtlasObjectId columnFamilyId    = new AtlasObjectId(HBaseDataTypes.HBASE_COLUMN_FAMILY.getName(), REFERENCEABLE_ATTRIBUTE_NAME, columnFamilyQName);

        LOG.info("Delete ColumnFamily {}", columnFamilyQName);

        hbaseOperationContext.addMessage(new EntityDeleteRequestV2(hbaseOperationContext.getUser(), Collections.singletonList(columnFamilyId)));
    }


    /**
     * Construct the qualified name used to uniquely identify a ColumnFamily instance in Atlas.
     *
     * @param metadataNamespace  Metadata namespace of the cluster to which the HBase component belongs
     * @param nameSpace    Name of the HBase database to which the Table belongs
     * @param tableName    Name of the HBase table
     * @param columnFamily Name of the ColumnFamily
     * @return Unique qualified name to identify the Table instance in Atlas.
     */
    public static String getColumnFamilyQualifiedName(String metadataNamespace, String nameSpace, String tableName, String columnFamily) {
        if (metadataNamespace == null || nameSpace == null || tableName == null || columnFamily == null) {
            return null;
        } else {
            return String.format(HBASE_COLUMN_FAMILY_QUALIFIED_NAME_FORMAT, nameSpace, stripNameSpace(tableName), columnFamily, metadataNamespace);
        }
    }

    /**
     * Construct the qualified name used to uniquely identify a Table instance in Atlas.
     *
     * @param metadataNamespace  Metadata namespace of the cluster to which the HBase component belongs
     * @param nameSpace   Name of the HBase database to which the Table belongs
     * @param tableName   Name of the HBase table
     * @return Unique qualified name to identify the Table instance in Atlas.
     */
    public static String getTableQualifiedName(String metadataNamespace, String nameSpace, String tableName) {
        if (metadataNamespace == null || nameSpace == null || tableName == null) {
            return null;
        } else {
            return String.format(HBASE_TABLE_QUALIFIED_NAME_FORMAT, nameSpace, stripNameSpace(tableName), metadataNamespace);
        }
    }

    /**
     * Construct the qualified name used to uniquely identify a HBase NameSpace instance in Atlas.
     *
     * @param metadataNamespace  Metadata namespace of the cluster to which the HBase component belongs
     * @param nameSpace
     * @return Unique qualified name to identify the HBase NameSpace instance in Atlas.
     */
    public static String getNameSpaceQualifiedName(String metadataNamespace, String nameSpace) {
        if (metadataNamespace == null || nameSpace == null) {
            return null;
        } else {
            return String.format(HBASE_NAMESPACE_QUALIFIED_NAME, nameSpace, metadataNamespace);
        }
    }

    private static String stripNameSpace(String tableName) {
        return tableName.substring(tableName.indexOf(":") + 1);
    }

    private AtlasEntity buildNameSpace(HBaseOperationContext hbaseOperationContext) {
        AtlasEntity         nameSpace     = new AtlasEntity(HBaseDataTypes.HBASE_NAMESPACE.getName());
        NamespaceDescriptor nameSpaceDesc = hbaseOperationContext.getNamespaceDescriptor();
        String              nameSpaceName = nameSpaceDesc == null ? null : hbaseOperationContext.getNamespaceDescriptor().getName();

        if (nameSpaceName == null) {
            nameSpaceName = hbaseOperationContext.getNameSpace();
        }

        Date now = new Date(System.currentTimeMillis());

        nameSpace.setAttribute(ATTR_NAME, nameSpaceName);
        nameSpace.setAttribute(REFERENCEABLE_ATTRIBUTE_NAME, getNameSpaceQualifiedName(getMetadataNamespace(), nameSpaceName));
        nameSpace.setAttribute(AtlasConstants.CLUSTER_NAME_ATTRIBUTE, getMetadataNamespace());
        nameSpace.setAttribute(ATTR_DESCRIPTION, nameSpaceName);
        nameSpace.setAttribute(ATTR_PARAMETERS, hbaseOperationContext.getHbaseConf());
        nameSpace.setAttribute(ATTR_OWNER, hbaseOperationContext.getOwner());
        nameSpace.setAttribute(ATTR_MODIFIED_TIME, now);

        if (OPERATION.CREATE_NAMESPACE.equals(hbaseOperationContext.getOperation())) {
            nameSpace.setAttribute(ATTR_CREATE_TIME, now);
        }

        return nameSpace;
    }

    private AtlasEntity buildTable(HBaseOperationContext hbaseOperationContext, AtlasEntity nameSpace) {
        AtlasEntity table         = new AtlasEntity(HBaseDataTypes.HBASE_TABLE.getName());
        String      tableName     = getTableName(hbaseOperationContext);
        String      nameSpaceName = (String) nameSpace.getAttribute(ATTR_NAME);
        String      tableQName    = getTableQualifiedName(getMetadataNamespace(), nameSpaceName, tableName);
        OPERATION   operation     = hbaseOperationContext.getOperation();
        Date        now           = new Date(System.currentTimeMillis());

        table.setAttribute(REFERENCEABLE_ATTRIBUTE_NAME, tableQName);
        table.setAttribute(ATTR_NAME, tableName);
        table.setAttribute(ATTR_URI, tableName);
        table.setAttribute(ATTR_OWNER, hbaseOperationContext.getOwner());
        table.setAttribute(ATTR_DESCRIPTION, tableName);
        table.setAttribute(ATTR_PARAMETERS, hbaseOperationContext.getHbaseConf());
        table.setRelationshipAttribute(ATTR_NAMESPACE, AtlasTypeUtil.getAtlasRelatedObjectId(nameSpace, RELATIONSHIP_HBASE_TABLE_NAMESPACE));

        TableDescriptor tableDescriptor = hbaseOperationContext.gethTableDescriptor();
        if (tableDescriptor != null) {
            table.setAttribute(ATTR_TABLE_MAX_FILESIZE, tableDescriptor.getMaxFileSize());
            table.setAttribute(ATTR_TABLE_REPLICATION_PER_REGION, tableDescriptor.getRegionReplication());
            table.setAttribute(ATTR_TABLE_ISREADONLY, tableDescriptor.isReadOnly());
            table.setAttribute(ATTR_TABLE_ISNORMALIZATION_ENABLED, tableDescriptor.isNormalizationEnabled());
            table.setAttribute(ATTR_TABLE_ISCOMPACTION_ENABLED, tableDescriptor.isCompactionEnabled());
            table.setAttribute(ATTR_TABLE_DURABLILITY, (tableDescriptor.getDurability() != null ? tableDescriptor.getDurability().name() : null));
            table.setAttribute(ATTR_TABLE_NORMALIZATION_ENABLED, tableDescriptor.isNormalizationEnabled());
        }

        switch (operation) {
            case CREATE_TABLE:
                table.setAttribute(ATTR_CREATE_TIME, now);
                table.setAttribute(ATTR_MODIFIED_TIME, now);
                break;
            case CREATE_COLUMN_FAMILY:
                table.setAttribute(ATTR_MODIFIED_TIME, now);
                break;
            case ALTER_TABLE:
            case ALTER_COLUMN_FAMILY:
                table.setAttribute(ATTR_MODIFIED_TIME, now);
                break;
            default:
                break;
        }

        return table;
    }

    private List<AtlasEntity> buildColumnFamilies(HBaseOperationContext hbaseOperationContext, AtlasEntity nameSpace, AtlasEntity table) {
        List<AtlasEntity>   columnFamilies     = new ArrayList<>();
        ColumnFamilyDescriptor[] columnFamilyDescriptors = hbaseOperationContext.gethColumnDescriptors();

        if (columnFamilyDescriptors != null) {
            for (ColumnFamilyDescriptor columnFamilyDescriptor : columnFamilyDescriptors) {
                AtlasEntity columnFamily = buildColumnFamily(hbaseOperationContext, columnFamilyDescriptor, nameSpace, table);

                columnFamilies.add(columnFamily);
            }
        }

        return columnFamilies;
    }

    private AtlasEntity buildColumnFamily(HBaseOperationContext hbaseOperationContext, ColumnFamilyDescriptor columnFamilyDescriptor, AtlasEntity nameSpace, AtlasEntity table) {
        AtlasEntity columnFamily      = new AtlasEntity(HBaseDataTypes.HBASE_COLUMN_FAMILY.getName());
        String      columnFamilyName  = columnFamilyDescriptor.getNameAsString();
        String      tableName         = (String) table.getAttribute(ATTR_NAME);
        String      nameSpaceName     = (String) nameSpace.getAttribute(ATTR_NAME);
        String      columnFamilyQName = getColumnFamilyQualifiedName(getMetadataNamespace(), nameSpaceName, tableName, columnFamilyName);
        Date        now               = new Date(System.currentTimeMillis());

        columnFamily.setAttribute(ATTR_NAME, columnFamilyName);
        columnFamily.setAttribute(ATTR_DESCRIPTION, columnFamilyName);
        columnFamily.setAttribute(REFERENCEABLE_ATTRIBUTE_NAME, columnFamilyQName);
        columnFamily.setAttribute(ATTR_OWNER, hbaseOperationContext.getOwner());
        columnFamily.setRelationshipAttribute(ATTR_TABLE, AtlasTypeUtil.getAtlasRelatedObjectId(table, RELATIONSHIP_HBASE_TABLE_COLUMN_FAMILIES));

        if (columnFamilyDescriptor!= null) {
            columnFamily.setAttribute(ATTR_CF_BLOCK_CACHE_ENABLED, columnFamilyDescriptor.isBlockCacheEnabled());
            columnFamily.setAttribute(ATTR_CF_BLOOMFILTER_TYPE, (columnFamilyDescriptor.getBloomFilterType() != null ? columnFamilyDescriptor.getBloomFilterType().name():null));
            columnFamily.setAttribute(ATTR_CF_CACHED_BLOOM_ON_WRITE, columnFamilyDescriptor.isCacheBloomsOnWrite());
            columnFamily.setAttribute(ATTR_CF_CACHED_DATA_ON_WRITE, columnFamilyDescriptor.isCacheDataOnWrite());
            columnFamily.setAttribute(ATTR_CF_CACHED_INDEXES_ON_WRITE, columnFamilyDescriptor.isCacheIndexesOnWrite());
            columnFamily.setAttribute(ATTR_CF_COMPACTION_COMPRESSION_TYPE, (columnFamilyDescriptor.getCompactionCompressionType() != null ? columnFamilyDescriptor.getCompactionCompressionType().name():null));
            columnFamily.setAttribute(ATTR_CF_COMPRESSION_TYPE, (columnFamilyDescriptor.getCompressionType() != null ? columnFamilyDescriptor.getCompressionType().name():null));
            columnFamily.setAttribute(ATTR_CF_DATA_BLOCK_ENCODING, (columnFamilyDescriptor.getDataBlockEncoding() != null ? columnFamilyDescriptor.getDataBlockEncoding().name():null));
            columnFamily.setAttribute(ATTR_CF_ENCRYPTION_TYPE, columnFamilyDescriptor.getEncryptionType());
            columnFamily.setAttribute(ATTR_CF_EVICT_BLOCK_ONCLOSE, columnFamilyDescriptor.isEvictBlocksOnClose());
            columnFamily.setAttribute(ATTR_CF_INMEMORY_COMPACTION_POLICY, (columnFamilyDescriptor.getInMemoryCompaction() != null ? columnFamilyDescriptor.getInMemoryCompaction().name():null));
            columnFamily.setAttribute(ATTR_CF_KEEP_DELETE_CELLS, ( columnFamilyDescriptor.getKeepDeletedCells() != null ? columnFamilyDescriptor.getKeepDeletedCells().name():null));
            columnFamily.setAttribute(ATTR_CF_MAX_VERSIONS, columnFamilyDescriptor.getMaxVersions());
            columnFamily.setAttribute(ATTR_CF_MIN_VERSIONS, columnFamilyDescriptor.getMinVersions());
            columnFamily.setAttribute(ATTR_CF_NEW_VERSION_BEHAVIOR, columnFamilyDescriptor.isNewVersionBehavior());
            columnFamily.setAttribute(ATTR_CF_MOB_ENABLED, columnFamilyDescriptor.isMobEnabled());
            columnFamily.setAttribute(ATTR_CF_MOB_COMPATCTPARTITION_POLICY, ( columnFamilyDescriptor.getMobCompactPartitionPolicy() != null ? columnFamilyDescriptor.getMobCompactPartitionPolicy().name():null));
            columnFamily.setAttribute(ATTR_CF_PREFETCH_BLOCK_ONOPEN, columnFamilyDescriptor.isPrefetchBlocksOnOpen());
            columnFamily.setAttribute(ATTR_CF_STORAGE_POLICY, columnFamilyDescriptor.getStoragePolicy());
            columnFamily.setAttribute(ATTR_CF_TTL, columnFamilyDescriptor.getTimeToLive());
        }

        switch (hbaseOperationContext.getOperation()) {
            case CREATE_COLUMN_FAMILY:
            case CREATE_TABLE:
                columnFamily.setAttribute(ATTR_CREATE_TIME, now);
                columnFamily.setAttribute(ATTR_MODIFIED_TIME, now);
                break;

            case ALTER_COLUMN_FAMILY:
                columnFamily.setAttribute(ATTR_MODIFIED_TIME, now);
                break;

            default:
                break;
        }

        return columnFamily;
    }

    public String getMessageSource() {
        return HBASE_SOURCE;
    }

    private String getTableName(HBaseOperationContext hbaseOperationContext) {
        final String ret;

        TableName tableName = hbaseOperationContext.getTableName();

        if (tableName != null) {
            ret = tableName.getNameAsString();
        } else {
            TableDescriptor tableDescriptor = hbaseOperationContext.gethTableDescriptor();

            ret = (tableDescriptor != null) ? tableDescriptor.getTableName().getNameAsString() : null;
        }

        return ret;
    }

    public void sendHBaseNameSpaceOperation(final NamespaceDescriptor namespaceDescriptor, final String nameSpace, final OPERATION operation, ObserverContext<MasterCoprocessorEnvironment> ctx) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HBaseAtlasHook.sendHBaseNameSpaceOperation()");
        }

        try {
            final UserGroupInformation ugi  = getUGI(ctx);
            final User user                 = getActiveUser(ctx);
            final String userName           = (user != null) ? user.getShortName() : null;
            HBaseOperationContext hbaseOperationContext = handleHBaseNameSpaceOperation(namespaceDescriptor, nameSpace, operation, ugi, userName);

            sendNotification(hbaseOperationContext);
        } catch (Throwable t) {
            LOG.error("HBaseAtlasHook.sendHBaseNameSpaceOperation(): failed to send notification", t);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasHook.sendHBaseNameSpaceOperation()");
        }
    }

    public void sendHBaseTableOperation(TableDescriptor tableDescriptor, final TableName tableName, final OPERATION operation, ObserverContext<MasterCoprocessorEnvironment> ctx) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HBaseAtlasHook.sendHBaseTableOperation()");
        }

        try {
            final UserGroupInformation ugi  = getUGI(ctx);
            final User user                 = getActiveUser(ctx);
            final String userName           = (user != null) ? user.getShortName() : null;
            HBaseOperationContext hbaseOperationContext = handleHBaseTableOperation(tableDescriptor, tableName, operation, ugi, userName);

            sendNotification(hbaseOperationContext);
        } catch (Throwable t) {
            LOG.error("<== HBaseAtlasHook.sendHBaseTableOperation(): failed to send notification", t);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasHook.sendHBaseTableOperation()");
        }
    }

    private void sendNotification(HBaseOperationContext hbaseOperationContext) {
        UserGroupInformation ugi = hbaseOperationContext.getUgi();

        if (ugi != null && ugi.getRealUser() != null) {
            ugi = ugi.getRealUser();
        }

        notifyEntities(hbaseOperationContext.getMessages(), ugi);
    }

    private HBaseOperationContext handleHBaseNameSpaceOperation(NamespaceDescriptor namespaceDescriptor, String nameSpace, OPERATION operation, UserGroupInformation ugi, String userName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HBaseAtlasHook.handleHBaseNameSpaceOperation()");
        }

        HBaseOperationContext hbaseOperationContext = new HBaseOperationContext(namespaceDescriptor, nameSpace, operation, ugi, userName, userName);
        createAtlasInstances(hbaseOperationContext);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasHook.handleHBaseNameSpaceOperation(): {}",  hbaseOperationContext);
        }

        return hbaseOperationContext;
    }

    private HBaseOperationContext handleHBaseTableOperation(TableDescriptor tableDescriptor, TableName tableName, OPERATION operation, UserGroupInformation ugi, String userName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HBaseAtlasHook.handleHBaseTableOperation()");
        }

        Map<String, String>  hbaseConf          = null;
        String               owner              = null;
        String               tableNameSpace     = null;
        TableName            hbaseTableName     = null;
        ColumnFamilyDescriptor[]  columnFamilyDescriptors = null;

        if (tableDescriptor != null) {
            owner = tableDescriptor.getOwnerString();
            hbaseConf = null;
            hbaseTableName = tableDescriptor.getTableName();
            if (hbaseTableName != null) {
                tableNameSpace = hbaseTableName.getNamespaceAsString();
                if (tableNameSpace == null) {
                    tableNameSpace = hbaseTableName.getNameWithNamespaceInclAsString();
                }
            }
        }

        if (owner == null) {
            owner = userName;
        }

        if (tableDescriptor != null) {
            columnFamilyDescriptors = tableDescriptor.getColumnFamilies();
        }

        HBaseOperationContext hbaseOperationContext = new HBaseOperationContext(tableNameSpace, tableDescriptor, tableName, columnFamilyDescriptors, operation, ugi, userName, owner, hbaseConf);
        createAtlasInstances(hbaseOperationContext);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasHook.handleHBaseTableOperation(): {}",  hbaseOperationContext);
        }
        return hbaseOperationContext;
    }

    private HBaseOperationContext handleHBaseColumnFamilyOperation(ColumnFamilyDescriptor columnFamilyDescriptor, TableName tableName, String columnFamily, OPERATION operation, UserGroupInformation ugi, String userName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HBaseAtlasHook.handleHBaseColumnFamilyOperation()");
        }

        String               owner     = userName;
        Map<String, String>  hbaseConf = new HashMap<>();

        String tableNameSpace = tableName.getNamespaceAsString();
        if (tableNameSpace == null) {
            tableNameSpace = tableName.getNameWithNamespaceInclAsString();
        }

        if (columnFamilyDescriptor != null) {
            hbaseConf = columnFamilyDescriptor.getConfiguration();
        }

        HBaseOperationContext hbaseOperationContext = new HBaseOperationContext(tableNameSpace, tableName, columnFamilyDescriptor, columnFamily, operation, ugi, userName, owner, hbaseConf);
        createAtlasInstances(hbaseOperationContext);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HBaseAtlasHook.handleHBaseColumnFamilyOperation(): {}",  hbaseOperationContext);
        }
        return hbaseOperationContext;
    }

    private UserGroupInformation getUGI(ObserverContext<?> ctx) {
        UserGroupInformation ugi  = null;
        User                 user = null;
        try {
            user = getActiveUser(ctx);
            ugi  = UserGroupInformation.getLoginUser();
        } catch (Exception e) {
            // not setting the UGI here
        }

        if (ugi == null) {
            if (user != null) {
                ugi = user.getUGI();
            }
        }

        LOG.info("HBaseAtlasHook: UGI: {}",  ugi);
        return ugi;
    }

    private User getActiveUser(ObserverContext<?> ctx) throws IOException {
        return (User)ctx.getCaller().orElse(User.getCurrent());
    }
}
