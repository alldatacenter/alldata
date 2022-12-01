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

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.hbase.model.HBaseDataTypes;
import org.apache.atlas.hook.AtlasHookException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.utils.AtlasConfigurationUtil;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HBaseBridge {
    private static final Logger LOG = LoggerFactory.getLogger(HBaseBridge.class);

    private static final int     EXIT_CODE_SUCCESS          = 0;
    private static final int     EXIT_CODE_FAILED           = 1;
    private static final String  ATLAS_ENDPOINT             = "atlas.rest.address";
    private static final String  DEFAULT_ATLAS_URL          = "http://localhost:21000/";
    private static final String  CLUSTER_NAME_KEY           = "atlas.cluster.name";
    private static final String  DEFAULT_CLUSTER_NAME       = "primary";
    private static final String  HBASE_METADATA_NAMESPACE   = "atlas.metadata.namespace";
    private static final String  QUALIFIED_NAME             = "qualifiedName";
    private static final String  NAME                       = "name";
    private static final String  URI                        = "uri";
    private static final String  OWNER                      = "owner";
    private static final String  DESCRIPTION_ATTR           = "description";
    private static final String  CLUSTERNAME                = "clusterName";
    private static final String  NAMESPACE                  = "namespace";
    private static final String  TABLE                      = "table";
    private static final String  COLUMN_FAMILIES            = "column_families";

    // table metadata
    private static final String ATTR_TABLE_MAX_FILESIZE              = "maxFileSize";
    private static final String ATTR_TABLE_ISREADONLY                = "isReadOnly";
    private static final String ATTR_TABLE_ISCOMPACTION_ENABLED      = "isCompactionEnabled";
    private static final String ATTR_TABLE_REPLICATION_PER_REGION    = "replicasPerRegion";
    private static final String ATTR_TABLE_DURABLILITY               = "durability";
    private static final String ATTR_TABLE_NORMALIZATION_ENABLED     = "isNormalizationEnabled";

    // column family metadata
    private static final String ATTR_CF_BLOOMFILTER_TYPE             = "bloomFilterType";
    private static final String ATTR_CF_COMPRESSION_TYPE             = "compressionType";
    private static final String ATTR_CF_COMPACTION_COMPRESSION_TYPE  = "compactionCompressionType";
    private static final String ATTR_CF_ENCRYPTION_TYPE              = "encryptionType";
    private static final String ATTR_CF_KEEP_DELETE_CELLS            = "keepDeletedCells";
    private static final String ATTR_CF_MAX_VERSIONS                 = "maxVersions";
    private static final String ATTR_CF_MIN_VERSIONS                 = "minVersions";
    private static final String ATTR_CF_DATA_BLOCK_ENCODING          = "dataBlockEncoding";
    private static final String ATTR_CF_TTL                          = "ttl";
    private static final String ATTR_CF_BLOCK_CACHE_ENABLED          = "blockCacheEnabled";
    private static final String ATTR_CF_CACHED_BLOOM_ON_WRITE        = "cacheBloomsOnWrite";
    private static final String ATTR_CF_CACHED_DATA_ON_WRITE         = "cacheDataOnWrite";
    private static final String ATTR_CF_CACHED_INDEXES_ON_WRITE      = "cacheIndexesOnWrite";
    private static final String ATTR_CF_EVICT_BLOCK_ONCLOSE          = "evictBlocksOnClose";
    private static final String ATTR_CF_PREFETCH_BLOCK_ONOPEN        = "prefetchBlocksOnOpen";
    private static final String ATTRIBUTE_QUALIFIED_NAME             = "qualifiedName";
    private static final String ATTR_CF_INMEMORY_COMPACTION_POLICY   = "inMemoryCompactionPolicy";
    private static final String ATTR_CF_MOB_COMPATCTPARTITION_POLICY = "mobCompactPartitionPolicy";
    private static final String ATTR_CF_MOB_ENABLED                  = "isMobEnabled";
    private static final String ATTR_CF_NEW_VERSION_BEHAVIOR         = "newVersionBehavior";

    private static final String HBASE_NAMESPACE_QUALIFIED_NAME            = "%s@%s";
    private static final String HBASE_TABLE_QUALIFIED_NAME_FORMAT         = "%s:%s@%s";
    private static final String HBASE_COLUMN_FAMILY_QUALIFIED_NAME_FORMAT = "%s:%s.%s@%s";

    private final String        metadataNamespace;
    private final AtlasClientV2 atlasClientV2;
    private final Admin         hbaseAdmin;


    public static void main(String[] args) {
        int exitCode = EXIT_CODE_FAILED;
        AtlasClientV2 atlasClientV2  =null;

        try {
            Options options = new Options();
            options.addOption("n","namespace", true, "namespace");
            options.addOption("t", "table", true, "tablename");
            options.addOption("f", "filename", true, "filename");

            CommandLineParser parser            = new BasicParser();
            CommandLine       cmd               = parser.parse(options, args);
            String            namespaceToImport = cmd.getOptionValue("n");
            String            tableToImport     = cmd.getOptionValue("t");
            String            fileToImport      = cmd.getOptionValue("f");
            Configuration     atlasConf         = ApplicationProperties.get();
            String[]          urls              = atlasConf.getStringArray(ATLAS_ENDPOINT);

            if (urls == null || urls.length == 0) {
                urls = new String[] { DEFAULT_ATLAS_URL };
            }


            if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
                String[] basicAuthUsernamePassword = AuthenticationUtil.getBasicAuthenticationInput();

                atlasClientV2 = new AtlasClientV2(urls, basicAuthUsernamePassword);
            } else {
                UserGroupInformation ugi = UserGroupInformation.getCurrentUser();

                atlasClientV2 = new AtlasClientV2(ugi, ugi.getShortUserName(), urls);
            }

            HBaseBridge importer = new HBaseBridge(atlasConf, atlasClientV2);

            if (StringUtils.isNotEmpty(fileToImport)) {
                File f = new File(fileToImport);

                if (f.exists() && f.canRead()) {
                    BufferedReader br   = new BufferedReader(new FileReader(f));
                    String         line = null;

                    while((line = br.readLine()) != null) {
                        String val[] = line.split(":");

                        if (ArrayUtils.isNotEmpty(val)) {
                            namespaceToImport = val[0];

                            if (val.length > 1) {
                                tableToImport = val[1];
                            } else {
                                tableToImport = "";
                            }

                            importer.importHBaseEntities(namespaceToImport, tableToImport);
                        }
                    }

                    exitCode = EXIT_CODE_SUCCESS;
                } else {
                    LOG.error("Failed to read the file");
                }
            } else {
                importer.importHBaseEntities(namespaceToImport, tableToImport);

                exitCode = EXIT_CODE_SUCCESS;
            }
        } catch(ParseException e) {
            LOG.error("Failed to parse arguments. Error: ", e.getMessage());
            printUsage();
        } catch(Exception e) {
            System.out.println("ImportHBaseEntities failed. Please check the log file for the detailed error message");

            LOG.error("ImportHBaseEntities failed", e);
        }finally {
            if(atlasClientV2!=null) {
                atlasClientV2.close();
            }
        }

        System.exit(exitCode);
    }

    public HBaseBridge(Configuration atlasConf, AtlasClientV2 atlasClientV2) throws Exception {
        this.atlasClientV2     = atlasClientV2;
        this.metadataNamespace = getMetadataNamespace(atlasConf);

        org.apache.hadoop.conf.Configuration conf = HBaseConfiguration.create();

        LOG.info("checking HBase availability..");

        HBaseAdmin.available(conf);

        LOG.info("HBase is available");

        Connection conn = ConnectionFactory.createConnection(conf);

        hbaseAdmin = conn.getAdmin();
    }

    private String getMetadataNamespace(Configuration config) {
        return AtlasConfigurationUtil.getRecentString(config, HBASE_METADATA_NAMESPACE, getClusterName(config));
    }

    private String getClusterName(Configuration config) {
        return config.getString(CLUSTER_NAME_KEY, DEFAULT_CLUSTER_NAME);
    }

    private boolean importHBaseEntities(String namespaceToImport, String tableToImport) throws Exception {
        boolean ret = false;

        if (StringUtils.isEmpty(namespaceToImport) && StringUtils.isEmpty(tableToImport)) {
            // when both NameSpace and Table options are not present
            importNameSpaceAndTable();
            ret = true;
        } else if (StringUtils.isNotEmpty(namespaceToImport)) {
            // When Namespace option is present or both namespace and table options are present
            importNameSpaceWithTable(namespaceToImport, tableToImport);
            ret = true;
        } else  if (StringUtils.isNotEmpty(tableToImport)) {
            importTable(tableToImport);
            ret = true;
        }

        return ret;
    }

    public void importNameSpace(final String nameSpace) throws Exception {
        List<NamespaceDescriptor> matchingNameSpaceDescriptors = getMatchingNameSpaces(nameSpace);

        if (CollectionUtils.isNotEmpty(matchingNameSpaceDescriptors)) {
            for (NamespaceDescriptor namespaceDescriptor : matchingNameSpaceDescriptors) {
                createOrUpdateNameSpace(namespaceDescriptor);
            }
        } else {
            throw new AtlasHookException("No NameSpace found for the given criteria. NameSpace = " + nameSpace);
        }
    }

    public void importTable(final String tableName) throws Exception {
        String            tableNameStr = null;
        TableDescriptor[] htds         = hbaseAdmin.listTables(Pattern.compile(tableName));

        if (ArrayUtils.isNotEmpty(htds)) {
            for (TableDescriptor htd : htds) {
                String tblNameWithNameSpace    = htd.getTableName().getNameWithNamespaceInclAsString();
                String tblNameWithOutNameSpace = htd.getTableName().getNameAsString();

                if (tableName.equals(tblNameWithNameSpace)) {
                    tableNameStr = tblNameWithNameSpace;
                } else if (tableName.equals(tblNameWithOutNameSpace)) {
                    tableNameStr = tblNameWithOutNameSpace;
                } else {
                    // when wild cards are used in table name
                    if (tblNameWithNameSpace != null) {
                        tableNameStr = tblNameWithNameSpace;
                    } else if (tblNameWithOutNameSpace != null) {
                        tableNameStr = tblNameWithOutNameSpace;
                    }
                }

                byte[]                 nsByte       = htd.getTableName().getNamespace();
                String                 nsName       = new String(nsByte);
                NamespaceDescriptor    nsDescriptor = hbaseAdmin.getNamespaceDescriptor(nsName);
                AtlasEntityWithExtInfo entity       = createOrUpdateNameSpace(nsDescriptor);
                ColumnFamilyDescriptor[]    hcdts        = htd.getColumnFamilies();

                createOrUpdateTable(nsName, tableNameStr, entity.getEntity(), htd, hcdts);
            }
        } else {
            throw new AtlasHookException("No Table found for the given criteria. Table = " + tableName);
        }
    }

    private void importNameSpaceAndTable() throws Exception {
        NamespaceDescriptor[] namespaceDescriptors = hbaseAdmin.listNamespaceDescriptors();

        if (ArrayUtils.isNotEmpty(namespaceDescriptors)) {
            for (NamespaceDescriptor namespaceDescriptor : namespaceDescriptors) {
                String namespace = namespaceDescriptor.getName();

                importNameSpace(namespace);
            }
        }

        TableDescriptor[] htds = hbaseAdmin.listTables();

        if (ArrayUtils.isNotEmpty(htds)) {
            for (TableDescriptor htd : htds) {
                String tableName = htd.getTableName().getNameAsString();

                importTable(tableName);
            }
        }
    }

    private void importNameSpaceWithTable(String namespaceToImport, String tableToImport) throws Exception {
        importNameSpace(namespaceToImport);

        List<TableDescriptor> hTableDescriptors = new ArrayList<>();

        if (StringUtils.isEmpty(tableToImport)) {
            List<NamespaceDescriptor> matchingNameSpaceDescriptors = getMatchingNameSpaces(namespaceToImport);

            if (CollectionUtils.isNotEmpty(matchingNameSpaceDescriptors)) {
                hTableDescriptors = getTableDescriptors(matchingNameSpaceDescriptors);
            }
        } else {
            tableToImport = namespaceToImport +":" + tableToImport;

            TableDescriptor[] htds = hbaseAdmin.listTables(Pattern.compile(tableToImport));

            hTableDescriptors.addAll(Arrays.asList(htds));
        }

        if (CollectionUtils.isNotEmpty(hTableDescriptors)) {
            for (TableDescriptor htd : hTableDescriptors) {
                String tblName = htd.getTableName().getNameAsString();

                importTable(tblName);
            }
        }
    }

    private List<NamespaceDescriptor> getMatchingNameSpaces(String nameSpace) throws Exception {
        List<NamespaceDescriptor> ret                  = new ArrayList<>();
        NamespaceDescriptor[]     namespaceDescriptors = hbaseAdmin.listNamespaceDescriptors();
        Pattern                                pattern = Pattern.compile(nameSpace);

        for (NamespaceDescriptor namespaceDescriptor:namespaceDescriptors){
            String  nmSpace = namespaceDescriptor.getName();
            Matcher matcher = pattern.matcher(nmSpace);

            if (matcher.find()){
                ret.add(namespaceDescriptor);
            }
        }

        return ret;
    }

    private List<TableDescriptor> getTableDescriptors(List<NamespaceDescriptor> namespaceDescriptors) throws Exception {
        List<TableDescriptor> ret = new ArrayList<>();

        for(NamespaceDescriptor namespaceDescriptor:namespaceDescriptors) {
            TableDescriptor[] tableDescriptors = hbaseAdmin.listTableDescriptorsByNamespace(namespaceDescriptor.getName());

            ret.addAll(Arrays.asList(tableDescriptors));
        }

        return ret;
    }

    protected AtlasEntityWithExtInfo createOrUpdateNameSpace(NamespaceDescriptor namespaceDescriptor) throws Exception {
        String                 nsName          = namespaceDescriptor.getName();
        String                 nsQualifiedName = getNameSpaceQualifiedName(metadataNamespace, nsName);
        AtlasEntityWithExtInfo nsEntity        = findNameSpaceEntityInAtlas(nsQualifiedName);

        if (nsEntity == null) {
            LOG.info("Importing NameSpace: " + nsQualifiedName);

            AtlasEntity entity = getNameSpaceEntity(nsName, null);

            nsEntity = createEntityInAtlas(new AtlasEntityWithExtInfo(entity));
        } else {
            LOG.info("NameSpace already present in Atlas. Updating it..: " + nsQualifiedName);

            AtlasEntity entity = getNameSpaceEntity(nsName, nsEntity.getEntity());

            nsEntity.setEntity(entity);

            nsEntity = updateEntityInAtlas(nsEntity);
        }
        return nsEntity;
    }

    protected  AtlasEntityWithExtInfo  createOrUpdateTable(String nameSpace, String tableName, AtlasEntity nameSapceEntity, TableDescriptor htd, ColumnFamilyDescriptor[] hcdts) throws Exception {
        String                 owner            = htd.getOwnerString();
        String                 tblQualifiedName = getTableQualifiedName(metadataNamespace, nameSpace, tableName);
        AtlasEntityWithExtInfo ret              = findTableEntityInAtlas(tblQualifiedName);

        if (ret == null) {
            LOG.info("Importing Table: " + tblQualifiedName);

            AtlasEntity entity = getTableEntity(nameSpace, tableName, owner, nameSapceEntity, htd, null);

            ret = createEntityInAtlas(new AtlasEntityWithExtInfo(entity));
        } else {
            LOG.info("Table already present in Atlas. Updating it..: " + tblQualifiedName);

            AtlasEntity entity = getTableEntity(nameSpace, tableName, owner, nameSapceEntity, htd, ret.getEntity());

            ret.setEntity(entity);

            ret = updateEntityInAtlas(ret);
        }

        AtlasEntity tableEntity = ret.getEntity();

        if (tableEntity != null) {
            List<AtlasEntityWithExtInfo> cfEntities = createOrUpdateColumnFamilies(nameSpace, tableName, owner, hcdts, tableEntity);

            List<AtlasObjectId> cfIDs = new ArrayList<>();

            if (CollectionUtils.isNotEmpty(cfEntities)) {
                for (AtlasEntityWithExtInfo cfEntity : cfEntities) {
                    cfIDs.add(AtlasTypeUtil.getAtlasObjectId(cfEntity.getEntity()));
                }
            }
            tableEntity.setRelationshipAttribute(COLUMN_FAMILIES, AtlasTypeUtil.getAtlasRelatedObjectIdList(cfIDs, HBaseAtlasHook.RELATIONSHIP_HBASE_TABLE_COLUMN_FAMILIES));
        }

        return ret;
    }

    protected List<AtlasEntityWithExtInfo> createOrUpdateColumnFamilies(String nameSpace, String tableName, String owner, ColumnFamilyDescriptor[] hcdts , AtlasEntity tableEntity) throws Exception {
        List<AtlasEntityWithExtInfo > ret = new ArrayList<>();

        if (hcdts != null) {
            AtlasObjectId tableId = AtlasTypeUtil.getAtlasObjectId(tableEntity);

            for (ColumnFamilyDescriptor columnFamilyDescriptor : hcdts) {
                String                 cfName          = columnFamilyDescriptor.getNameAsString();
                String                 cfQualifiedName = getColumnFamilyQualifiedName(metadataNamespace, nameSpace, tableName, cfName);
                AtlasEntityWithExtInfo cfEntity        = findColumnFamiltyEntityInAtlas(cfQualifiedName);

                if (cfEntity == null) {
                    LOG.info("Importing Column-family: " + cfQualifiedName);

                    AtlasEntity entity = getColumnFamilyEntity(nameSpace, tableName, owner, columnFamilyDescriptor, tableId, null);

                    cfEntity = createEntityInAtlas(new AtlasEntityWithExtInfo(entity));
                } else {
                    LOG.info("ColumnFamily already present in Atlas. Updating it..: " + cfQualifiedName);

                    AtlasEntity entity = getColumnFamilyEntity(nameSpace, tableName, owner, columnFamilyDescriptor, tableId, cfEntity.getEntity());

                    cfEntity.setEntity(entity);

                    cfEntity = updateEntityInAtlas(cfEntity);
                }

                ret.add(cfEntity);
            }
        }

        return ret;
    }

    private AtlasEntityWithExtInfo findNameSpaceEntityInAtlas(String nsQualifiedName) {
        AtlasEntityWithExtInfo  ret = null;

        try {
            ret = findEntityInAtlas(HBaseDataTypes.HBASE_NAMESPACE.getName(), nsQualifiedName);
            clearRelationshipAttributes(ret);
        } catch (Exception e) {
            ret = null; // entity doesn't exist in Atlas
        }

        return ret;
    }

    private AtlasEntityWithExtInfo findTableEntityInAtlas(String tableQualifiedName) {
        AtlasEntityWithExtInfo  ret = null;

        try {
            ret = findEntityInAtlas(HBaseDataTypes.HBASE_TABLE.getName(), tableQualifiedName);
            clearRelationshipAttributes(ret);
        } catch (Exception e) {
            ret = null; // entity doesn't exist in Atlas
        }

        return ret;
    }

    private AtlasEntityWithExtInfo findColumnFamiltyEntityInAtlas(String columnFamilyQualifiedName) {
        AtlasEntityWithExtInfo  ret = null;

        try {
            ret = findEntityInAtlas(HBaseDataTypes.HBASE_COLUMN_FAMILY.getName(), columnFamilyQualifiedName);
            clearRelationshipAttributes(ret);
        } catch (Exception e) {
            ret = null; // entity doesn't exist in Atlas
        }

        return ret;
    }

    private AtlasEntityWithExtInfo findEntityInAtlas(String typeName, String qualifiedName) throws Exception {
        Map<String, String> attributes = Collections.singletonMap(QUALIFIED_NAME, qualifiedName);

        return atlasClientV2.getEntityByAttribute(typeName, attributes);
    }

    private AtlasEntity getNameSpaceEntity(String nameSpace, AtlasEntity nsEtity) {
        AtlasEntity ret  = null ;

        if (nsEtity == null) {
            ret = new AtlasEntity(HBaseDataTypes.HBASE_NAMESPACE.getName());
        } else {
            ret = nsEtity;
        }

        String qualifiedName = getNameSpaceQualifiedName(metadataNamespace, nameSpace);

        ret.setAttribute(QUALIFIED_NAME, qualifiedName);
        ret.setAttribute(CLUSTERNAME, metadataNamespace);
        ret.setAttribute(NAME, nameSpace);
        ret.setAttribute(DESCRIPTION_ATTR, nameSpace);

        return ret;
    }

    private AtlasEntity getTableEntity(String nameSpace, String tableName, String owner, AtlasEntity nameSpaceEntity, TableDescriptor htd, AtlasEntity atlasEntity) {
        AtlasEntity ret = null;

        if (atlasEntity == null) {
            ret = new AtlasEntity(HBaseDataTypes.HBASE_TABLE.getName());
        } else {
            ret = atlasEntity;
        }

        String tableQualifiedName = getTableQualifiedName(metadataNamespace, nameSpace, tableName);

        ret.setAttribute(QUALIFIED_NAME, tableQualifiedName);
        ret.setAttribute(CLUSTERNAME, metadataNamespace);
        ret.setRelationshipAttribute(NAMESPACE, AtlasTypeUtil.getAtlasRelatedObjectId(nameSpaceEntity, HBaseAtlasHook.RELATIONSHIP_HBASE_TABLE_NAMESPACE));
        ret.setAttribute(NAME, tableName);
        ret.setAttribute(DESCRIPTION_ATTR, tableName);
        ret.setAttribute(OWNER, owner);
        ret.setAttribute(URI, tableName);
        ret.setAttribute(ATTR_TABLE_MAX_FILESIZE, htd.getMaxFileSize());
        ret.setAttribute(ATTR_TABLE_REPLICATION_PER_REGION, htd.getRegionReplication());
        ret.setAttribute(ATTR_TABLE_ISREADONLY, htd.isReadOnly());
        ret.setAttribute(ATTR_TABLE_ISCOMPACTION_ENABLED, htd.isCompactionEnabled());
        ret.setAttribute(ATTR_TABLE_DURABLILITY, (htd.getDurability() != null ? htd.getDurability().name() : null));
        ret.setAttribute(ATTR_TABLE_NORMALIZATION_ENABLED, htd.isNormalizationEnabled());

        return ret;
    }

    private AtlasEntity getColumnFamilyEntity(String nameSpace, String tableName, String owner, ColumnFamilyDescriptor hcdt, AtlasObjectId tableId, AtlasEntity atlasEntity){
        AtlasEntity ret = null;

        if (atlasEntity == null) {
            ret = new AtlasEntity(HBaseDataTypes.HBASE_COLUMN_FAMILY.getName());
        } else {
            ret = atlasEntity;
        }

        String cfName          = hcdt.getNameAsString();
        String cfQualifiedName = getColumnFamilyQualifiedName(metadataNamespace, nameSpace, tableName, cfName);

        ret.setAttribute(QUALIFIED_NAME, cfQualifiedName);
        ret.setAttribute(CLUSTERNAME, metadataNamespace);
        ret.setRelationshipAttribute(TABLE, AtlasTypeUtil.getAtlasRelatedObjectId(tableId, HBaseAtlasHook.RELATIONSHIP_HBASE_TABLE_COLUMN_FAMILIES));
        ret.setAttribute(NAME, cfName);
        ret.setAttribute(DESCRIPTION_ATTR, cfName);
        ret.setAttribute(OWNER, owner);
        ret.setAttribute(ATTR_CF_BLOCK_CACHE_ENABLED, hcdt.isBlockCacheEnabled());
        ret.setAttribute(ATTR_CF_BLOOMFILTER_TYPE, (hcdt.getBloomFilterType() != null ? hcdt.getBloomFilterType().name():null));
        ret.setAttribute(ATTR_CF_CACHED_BLOOM_ON_WRITE, hcdt.isCacheBloomsOnWrite());
        ret.setAttribute(ATTR_CF_CACHED_DATA_ON_WRITE, hcdt.isCacheDataOnWrite());
        ret.setAttribute(ATTR_CF_CACHED_INDEXES_ON_WRITE, hcdt.isCacheIndexesOnWrite());
        ret.setAttribute(ATTR_CF_COMPACTION_COMPRESSION_TYPE, (hcdt.getCompactionCompressionType() != null ? hcdt.getCompactionCompressionType().name():null));
        ret.setAttribute(ATTR_CF_COMPRESSION_TYPE, (hcdt.getCompressionType() != null ? hcdt.getCompressionType().name():null));
        ret.setAttribute(ATTR_CF_DATA_BLOCK_ENCODING, (hcdt.getDataBlockEncoding() != null ? hcdt.getDataBlockEncoding().name():null));
        ret.setAttribute(ATTR_CF_ENCRYPTION_TYPE, hcdt.getEncryptionType());
        ret.setAttribute(ATTR_CF_EVICT_BLOCK_ONCLOSE, hcdt.isEvictBlocksOnClose());
        ret.setAttribute(ATTR_CF_KEEP_DELETE_CELLS, ( hcdt.getKeepDeletedCells() != null ? hcdt.getKeepDeletedCells().name():null));
        ret.setAttribute(ATTR_CF_MAX_VERSIONS, hcdt.getMaxVersions());
        ret.setAttribute(ATTR_CF_MIN_VERSIONS, hcdt.getMinVersions());
        ret.setAttribute(ATTR_CF_PREFETCH_BLOCK_ONOPEN, hcdt.isPrefetchBlocksOnOpen());
        ret.setAttribute(ATTR_CF_TTL, hcdt.getTimeToLive());
        ret.setAttribute(ATTR_CF_INMEMORY_COMPACTION_POLICY, (hcdt.getInMemoryCompaction() != null ? hcdt.getInMemoryCompaction().name():null));
        ret.setAttribute(ATTR_CF_MOB_COMPATCTPARTITION_POLICY, ( hcdt.getMobCompactPartitionPolicy() != null ? hcdt.getMobCompactPartitionPolicy().name():null));
        ret.setAttribute(ATTR_CF_MOB_ENABLED,hcdt.isMobEnabled());
        ret.setAttribute(ATTR_CF_NEW_VERSION_BEHAVIOR,hcdt.isNewVersionBehavior());

        return ret;
    }

    private AtlasEntityWithExtInfo createEntityInAtlas(AtlasEntityWithExtInfo entity) throws Exception {
        AtlasEntityWithExtInfo  ret      = null;
        EntityMutationResponse  response = atlasClientV2.createEntity(entity);
        List<AtlasEntityHeader> entities = response.getCreatedEntities();

        if (CollectionUtils.isNotEmpty(entities)) {
            AtlasEntityWithExtInfo getByGuidResponse = atlasClientV2.getEntityByGuid(entities.get(0).getGuid());

            ret = getByGuidResponse;

            LOG.info("Created {} entity: name={}, guid={}", ret.getEntity().getTypeName(), ret.getEntity().getAttribute(ATTRIBUTE_QUALIFIED_NAME), ret.getEntity().getGuid());
        }
        return ret;
    }

    private AtlasEntityWithExtInfo  updateEntityInAtlas(AtlasEntityWithExtInfo entity) throws Exception {
        AtlasEntityWithExtInfo ret      = null;
        EntityMutationResponse  response = atlasClientV2.updateEntity(entity);

        if (response != null) {
            List<AtlasEntityHeader> entities = response.getUpdatedEntities();

            if (CollectionUtils.isNotEmpty(entities)) {
                AtlasEntityWithExtInfo getByGuidResponse = atlasClientV2.getEntityByGuid(entities.get(0).getGuid());

                ret = getByGuidResponse;

                LOG.info("Updated {} entity: name={}, guid={} ", ret.getEntity().getTypeName(), ret.getEntity().getAttribute(ATTRIBUTE_QUALIFIED_NAME), ret.getEntity().getGuid());
            } else {
                LOG.info("Entity: name={} ", entity.toString() + " not updated as it is unchanged from what is in Atlas" );
                ret = entity;
            }
        } else {
            LOG.info("Entity: name={} ", entity.toString() + " not updated as it is unchanged from what is in Atlas" );
            ret = entity;
        }

        return ret;
    }

    /**
     * Construct the qualified name used to uniquely identify a ColumnFamily instance in Atlas.
     * @param metadataNamespace Metadata namespace of the cluster to which the Hbase component belongs
     * @param nameSpace Name of the Hbase database to which the Table belongs
     * @param tableName Name of the Hbase table
     * @param columnFamily Name of the ColumnFamily
     * @return Unique qualified name to identify the Table instance in Atlas.
     */
    private static String getColumnFamilyQualifiedName(String metadataNamespace, String nameSpace, String tableName, String columnFamily) {
        tableName = stripNameSpace(tableName);
        return String.format(HBASE_COLUMN_FAMILY_QUALIFIED_NAME_FORMAT, nameSpace, tableName, columnFamily, metadataNamespace);
    }

    /**
     * Construct the qualified name used to uniquely identify a Table instance in Atlas.
     * @param metadataNamespace Metadata namespace of the cluster to which the Hbase component belongs
     * @param nameSpace Name of the Hbase database to which the Table belongs
     * @param tableName Name of the Hbase table
     * @return Unique qualified name to identify the Table instance in Atlas.
     */
    private static String getTableQualifiedName(String metadataNamespace, String nameSpace, String tableName) {
        tableName = stripNameSpace(tableName);
        return String.format(HBASE_TABLE_QUALIFIED_NAME_FORMAT, nameSpace, tableName, metadataNamespace);
    }

    /**
     * Construct the qualified name used to uniquely identify a Hbase NameSpace instance in Atlas.
     * @param metadataNamespace Metadata namespace of the cluster to which the Hbase component belongs
     * @param nameSpace Name of the NameSpace
     * @return Unique qualified name to identify the HBase NameSpace instance in Atlas.
     */
    private static String getNameSpaceQualifiedName(String metadataNamespace, String nameSpace) {
        return String.format(HBASE_NAMESPACE_QUALIFIED_NAME, nameSpace, metadataNamespace);
    }

    private static String stripNameSpace(String tableName){
        tableName = tableName.substring(tableName.indexOf(":")+1);

        return tableName;
    }

    private static void printUsage() {
        System.out.println("Usage 1: import-hbase.sh [-n <namespace regex> OR --namespace <namespace regex >] [-t <table regex > OR --table <table regex>]");
        System.out.println("Usage 2: import-hbase.sh [-f <filename>]" );
        System.out.println("   Format:");
        System.out.println("        namespace1:tbl1");
        System.out.println("        namespace1:tbl2");
        System.out.println("        namespace2:tbl1");
    }

    private void clearRelationshipAttributes(AtlasEntityWithExtInfo entity) {
        if (entity != null) {
            clearRelationshipAttributes(entity.getEntity());

            if (entity.getReferredEntities() != null) {
                clearRelationshipAttributes(entity.getReferredEntities().values());
            }
        }
    }

    private void clearRelationshipAttributes(Collection<AtlasEntity> entities) {
        if (entities != null) {
            for (AtlasEntity entity : entities) {
                clearRelationshipAttributes(entity);
            }
        }
    }

    private void clearRelationshipAttributes(AtlasEntity entity) {
        if (entity != null && entity.getRelationshipAttributes() != null) {
            entity.getRelationshipAttributes().clear();
        }
    }
}
