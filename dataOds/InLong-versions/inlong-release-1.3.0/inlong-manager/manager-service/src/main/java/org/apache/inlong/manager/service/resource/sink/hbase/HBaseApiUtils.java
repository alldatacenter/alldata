/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.resource.sink.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.inlong.manager.pojo.sink.hbase.HBaseColumnFamilyInfo;
import org.apache.inlong.manager.pojo.sink.hbase.HBaseTableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utils for hbase api
 */
public class HBaseApiUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(HBaseApiUtils.class);

    private static final String HBASE_CONF_ZK_QUORUM = "hbase.zookeeper.quorum";
    private static final String HBASE_CONF_ZNODE_PARENT = "zookeeper.znode.parent";

    /**
     * Get and verify hbase connection
     */
    private static Connection getConnection(String zkAddress, String zkNode) throws Exception {
        Configuration config = HBaseConfiguration.create();

        // ip1:port,ip2:port,...
        config.set(HBASE_CONF_ZK_QUORUM, zkAddress);
        config.set(HBASE_CONF_ZNODE_PARENT, zkNode);

        return ConnectionFactory.createConnection(config);
    }

    /**
     * Create hbase namespace
     */
    public static void createNamespace(String zkAddress, String zkNode, String namespace) throws Exception {
        if (namespace == null || namespace.isEmpty()) {
            return;
        }
        try (Connection conn = getConnection(zkAddress, zkNode)) {
            Admin admin = conn.getAdmin();
            if (Arrays.asList(admin.listNamespaces()).contains(namespace)) {
                LOGGER.info("hbase namespace {} already exists", namespace);
                return;
            }
            admin.createNamespace(NamespaceDescriptor.create(namespace).build());
            LOGGER.info("hbase namespace {} created", namespace);
        }
    }

    /**
     * Create hbase table
     */
    public static void createTable(String zkAddress, String zkNode, HBaseTableInfo tableInfo) throws Exception {
        TableName tableName = TableName.valueOf(tableInfo.getNamespace(), tableInfo.getTableName());
        TableDescriptorBuilder desc = TableDescriptorBuilder.newBuilder(tableName);
        for (HBaseColumnFamilyInfo cf : tableInfo.getColumnFamilies()) {
            // properties of column families can also be set here with builder
            // frontend doesn't introduce much at this moment
            desc.setColumnFamily(ColumnFamilyDescriptorBuilder.of(cf.getCfName()));
        }
        try (Connection conn = getConnection(zkAddress, zkNode)) {
            Admin admin = conn.getAdmin();
            admin.createTable(desc.build());
        }
    }

    /**
     * Check hbase table already exists or not
     */
    public static boolean tableExists(String zkAddress, String zkNode, String namespace, String qualifier)
            throws Exception {
        TableName tableName = TableName.valueOf(namespace, qualifier);
        try (Connection conn = getConnection(zkAddress, zkNode)) {
            Admin admin = conn.getAdmin();
            return admin.tableExists(tableName);
        }
    }

    /**
     * Query hbase table column families
     */
    public static List<HBaseColumnFamilyInfo> getColumnFamilies(String zkAddress, String zkNode, String namespace,
            String qualifier) throws Exception {
        List<HBaseColumnFamilyInfo> cfList = new ArrayList<>();
        TableName tableName = TableName.valueOf(namespace, qualifier);
        try (Connection conn = getConnection(zkAddress, zkNode)) {
            Table table = conn.getTable(tableName);
            for (ColumnFamilyDescriptor cf : table.getDescriptor().getColumnFamilies()) {
                HBaseColumnFamilyInfo info = new HBaseColumnFamilyInfo();
                info.setCfName(cf.getNameAsString());
                info.setTtl(cf.getTimeToLive());
                cfList.add(info);
            }
        }
        return cfList;
    }

    /**
     * Add column families for hbase table
     */
    public static void addColumnFamilies(String zkAddress, String zkNode, String namespace, String qualifier,
            List<HBaseColumnFamilyInfo> columnFamilies) throws Exception {
        TableName tableName = TableName.valueOf(namespace, qualifier);
        try (Connection conn = getConnection(zkAddress, zkNode)) {
            Admin admin = conn.getAdmin();
            admin.disableTable(tableName);
            try {
                for (HBaseColumnFamilyInfo info : columnFamilies) {
                    admin.addColumnFamily(tableName, ColumnFamilyDescriptorBuilder.of(info.getCfName()));
                }
            } finally {
                admin.enableTable(tableName);
            }
        }
    }

}
