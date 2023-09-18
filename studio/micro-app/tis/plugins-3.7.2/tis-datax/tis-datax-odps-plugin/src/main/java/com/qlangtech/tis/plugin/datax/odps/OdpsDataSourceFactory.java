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

package com.qlangtech.tis.plugin.datax.odps;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.writer.odpswriter.Constant;
import com.alibaba.datax.plugin.writer.odpswriter.Key;
import com.alibaba.datax.plugin.writer.odpswriter.util.OdpsUtil;
import com.aliyun.odps.*;
import com.google.common.collect.Lists;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.AuthToken;
import com.qlangtech.tis.plugin.aliyun.AccessKey;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * https://help.aliyun.com/document_detail/161246.html
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-02-21 14:20
 **/
public class OdpsDataSourceFactory extends BasicDataSourceFactory {
    private static final Logger logger = LoggerFactory.getLogger(OdpsDataSourceFactory.class);
    public static final String NAME = "AliyunODPS";
    // private static final String FIELD_ENDPOINT = "endpoint";
    private static final String DRIVER_NAME = "com.aliyun.odps.jdbc.OdpsDriver";


    @FormField(ordinal = 1, validate = {Validator.require, Validator.url})
    public String odpsServer;

    @FormField(ordinal = 2, validate = {Validator.require, Validator.url})
    public String tunnelServer;

    @FormField(ordinal = 3, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String project;

    @FormField(ordinal = 4, type = FormFieldType.ENUM, advance = true, validate = {Validator.require})
    public Boolean useProjectTimeZone;

    @FormField(ordinal = 5, validate = {})
    public AuthToken authToken;

    @Override
    public List<ColumnMetaData> getTableMetadata(JDBCConnection conn, boolean inSink, EntityName table) throws TableNotFoundException {
        List<ColumnMetaData> cols = Lists.newArrayList();
        ColumnMetaData colMeta = null;
        try {
            Odps odps = this.createOdps();
            Tables tables = odps.tables();
            if (!tables.exists(table.getTabName())) {
                throw new TableNotFoundException(this, table.getTableName());
            }

            Table tab = tables.get(table.getTabName());
            TableSchema schema = tab.getSchema();
            int index = 0;
            for (Column col : schema.getColumns()) {
                //int index, String key, DataType type, boolean pk, boolean nullable
                //TODO: convert  col.getTypeInfo() to DataType
                colMeta = new ColumnMetaData(index++, col.getName(), null, false);
                cols.add(colMeta);
            }
            return cols;
        } catch (OdpsException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * example: jdbc:odps:http://service.cn-hangzhou.maxcompute.aliyun.com/api?project=test_project&useProjectTimeZone=true;
     *
     * @param db
     * @param ip
     * @param dbName
     * @return
     */
    @Override
    public String buidJdbcUrl(DBConfig db, String ip, String dbName) {
        StringBuffer jdbc = new StringBuffer("jdbc:odps:");
        jdbc.append(this.odpsServer);
        jdbc.append("?project=").append(this.project);
        jdbc.append("&useProjectTimeZone=").append(this.useProjectTimeZone);
        return jdbc.toString();
    }

    @Override
    public DBConfig getDbConfig() {
        final DBConfig dbConfig = new DBConfig(this);
        dbConfig.setName(this.getDbName());
        dbConfig.setDbEnum(Collections.singletonMap(this.odpsServer, Lists.newArrayList(this.project)));
        return dbConfig;
    }

    @Override
    public final Optional<String> getEscapeChar() {
        return Optional.of("`");
    }

//    private OdpsEndpoint getEndpoint() {
//        return OdpsEndpoint.getEndpoint(this.endpoint);
//    }

    public String getJdbcUrl() {
        for (String jdbcUrl : this.getJdbcUrls()) {
            return jdbcUrl;
        }
        throw new IllegalStateException("jdbcUrl can not be empty");
    }

    /**
     * "subDescEnumFilter": "return com.qlangtech.tis.plugins.incr.flink.chunjun.sink.SinkTabPropsExtends.filter(desc);"
     *
     * @param descs
     * @return
     */
    public static List<? extends Descriptor> filter(List<? extends Descriptor> descs) {
        return descs.stream().filter((desc) -> {
            return desc instanceof AccessKey.DefaultDescriptor;
        }).collect(Collectors.toList());
    }

    public String getDbName() {
        return this.project;
    }

    @Override
    public DataSourceMeta.JDBCConnection getConnection(String jdbcUrl) throws SQLException {
        try {
            Class.forName(DRIVER_NAME);
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }

        AccessKey accessKey = getAccessKey();
        Connection conn = DriverManager.getConnection(
                jdbcUrl,
                accessKey.getAccessKeyId(), accessKey.getAccessKeySecret());

        return new JDBCConnection(conn, jdbcUrl);
    }

    public Odps createOdps() {
        OdpsDataSourceFactory endpoint = this;
        Configuration config = Configuration.newDefault();
        config.set(Key.ACCOUNT_TYPE, Constant.DEFAULT_ACCOUNT_TYPE);
        AccessKey accessKey = endpoint.getAccessKey();
        config.set(Key.ACCESS_ID, accessKey.getAccessKeyId());
        config.set(Key.ACCESS_KEY, accessKey.getAccessKeySecret());
        config.set(Key.ODPS_SERVER, endpoint.odpsServer);
        config.set(Key.PROJECT, endpoint.project);
        //config.set(Key.SECURITY_TOKEN,)
        Odps odps = OdpsUtil.initOdpsProject(config);
        return odps;
    }

    public TableSchema getTableSchema(EntityName dumpTable) {
        Table table = getOdpsTable(dumpTable, Optional.empty());
        TableSchema schema = table.getSchema();
        return schema;
    }

    public Table getOdpsTable(EntityName dumpTable, Optional<String> escapChar) {
        Odps odps = this.createOdps();
        //odps.tables().get("dd").createPartition();
        return odps.tables().get(this.project, dumpTable.getTableName(escapChar));
    }

    public List<String> getJdbcUrls() {
        return getJdbcUrls(false);
    }

    public AccessKey getAccessKey() {
        return this.authToken.accept(new AuthToken.Visitor<AccessKey>() {
            @Override
            public AccessKey visit(AccessKey accessKey) {
                return accessKey;
            }
        });
    }

    @TISExtension
    public static class DefaultDescriptor extends BasicRdbmsDataSourceFactoryDescriptor {
        public DefaultDescriptor() {
            super();
            // this.registerSelectOptions(FIELD_ENDPOINT, () -> ParamsConfig.getItems(OdpsEndpoint.ODPS_ENDPOINT_NAME));
        }

        @Override
        public EndType getEndType() {
            return EndType.AliyunODPS;
        }

        @Override
        protected boolean validateDSFactory(IControlMsgHandler msgHandler, Context context, BasicDataSourceFactory dsFactory) {
            OdpsDataSourceFactory endpoint = (OdpsDataSourceFactory) dsFactory;
            final Odps odps = endpoint.createOdps();
//            Configuration config = Configuration.newDefault();
//            config.set(Key.ACCOUNT_TYPE, Constant.DEFAULT_ACCOUNT_TYPE);
//            AccessKey accessKey = endpoint.getAccessKey();
//            config.set(Key.ACCESS_ID, accessKey.getAccessKeyId());
//            config.set(Key.ACCESS_KEY, accessKey.getAccessKeySecret());
//            config.set(Key.ODPS_SERVER, endpoint.odpsServer);
//            config.set(Key.PROJECT, endpoint.project);
////config.set(Key.SECURITY_TOKEN,)
//            = OdpsUtil.initOdpsProject(config);
            try {
                // 校验配置是否正确
                odps.projects().exists(odps.getDefaultProject());
                return super.validateDSFactory(msgHandler, context, dsFactory);
            } catch (OdpsException e) {
                logger.warn(e.getMessage(), e);
                msgHandler.addErrorMessage(context, e.getMessage());
                return false;
            }
        }


        @Override
        protected String getDataSourceName() {
            return NAME;
        }

        @Override
        protected boolean supportFacade() {
            return false;
        }
    }

}
