///**
// *   Licensed to the Apache Software Foundation (ASF) under one
// *   or more contributor license agreements.  See the NOTICE file
// *   distributed with this work for additional information
// *   regarding copyright ownership.  The ASF licenses this file
// *   to you under the Apache License, Version 2.0 (the
// *   "License"); you may not use this file except in compliance
// *   with the License.  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *   Unless required by applicable law or agreed to in writing, software
// *   distributed under the License is distributed on an "AS IS" BASIS,
// *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *   See the License for the specific language governing permissions and
// *   limitations under the License.
// */
//
//package com.dorisdb.connector.datax.plugin.writer.doriswriter;
//
//
//import com.alibaba.datax.common.element.Record;
//import com.alibaba.datax.common.exception.DataXException;
//import com.alibaba.datax.common.plugin.RecordReceiver;
//import com.alibaba.datax.common.spi.Writer;
//import com.alibaba.datax.common.util.Configuration;
//import com.alibaba.datax.plugin.rdbms.util.DBUtil;
//import com.alibaba.datax.plugin.rdbms.util.DBUtilErrorCode;
//import com.dorisdb.connector.datax.plugin.writer.doriswriter.manager.DorisWriterManager;
//import com.dorisdb.connector.datax.plugin.writer.doriswriter.row.DorisISerializer;
//import com.dorisdb.connector.datax.plugin.writer.doriswriter.row.DorisSerializerFactory;
//import com.dorisdb.connector.datax.plugin.writer.doriswriter.util.DorisWriterUtil;
//import com.qlangtech.tis.offline.DataxUtils;
//import com.qlangtech.tis.plugin.datax.common.InitWriterTable;
//import com.qlangtech.tis.plugin.ds.IDataSourceFactoryGetter;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//public class DorisWriter extends Writer {
//    private static final Logger logger = LoggerFactory.getLogger(DorisWriter.class);
//
//    public static class Job extends Writer.Job {
//
//        private static final Logger LOG = LoggerFactory.getLogger(Job.class);
//        private Configuration originalConfig = null;
//        private DorisWriterOptions options;
//        private IDataSourceFactoryGetter dsFactoryGetter;
//
//        @Override
//        public void init() {
//            try {
//                this.originalConfig = super.getPluginJobConf();
//                options = new DorisWriterOptions(super.getPluginJobConf());
//                options.doPretreatment();
//                // 执行初始化建标操作
//                String dataXName = this.originalConfig.getString(DataxUtils.DATAX_NAME);
//                String tableName = options.getTable();
//                List<String> jdbcUrls = Collections.singletonList(options.getJdbcUrl());
//                InitWriterTable.process(dataXName, tableName, jdbcUrls);
//                this.dsFactoryGetter = DBUtil.getWriterDataSourceFactoryGetter(this.originalConfig);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        @Override
//        public void preCheck() {
//            this.init();
//            DorisWriterUtil.preCheckPrePareSQL(options);
//            DorisWriterUtil.preCheckPostSQL(options);
//        }
//
//        @Override
//        public void prepare() {
//
//            List<String> renderedPreSqls = DorisWriterUtil.renderPreOrPostSqls(options.getPreSqlList(), options.getTable());
//            if (null != renderedPreSqls && !renderedPreSqls.isEmpty()) {
//                String username = options.getUsername();
//                String password = options.getPassword();
//                String jdbcUrl = options.getJdbcUrl();
//                try (Connection conn = DBUtil.getConnection(this.dsFactoryGetter, jdbcUrl, username, password)) {
//                    LOG.info("Begin to execute preSqls:[{}]. context info:{}.", String.join(";", renderedPreSqls), jdbcUrl);
//                    DorisWriterUtil.executeSqls(conn, renderedPreSqls);
//                    // DBUtil.closeDBResources(null, null, conn);
//                } catch (SQLException e) {
//                    logger.warn(e.getMessage(), e);
//                }
//            }
//        }
//
//        @Override
//        public List<Configuration> split(int mandatoryNumber) {
//            List<Configuration> configurations = new ArrayList<>(mandatoryNumber);
//            for (int i = 0; i < mandatoryNumber; i++) {
//                configurations.add(originalConfig);
//            }
//            return configurations;
//        }
//
//        @Override
//        public void post() {
//
//            List<String> renderedPostSqls = DorisWriterUtil.renderPreOrPostSqls(options.getPostSqlList(), options.getTable());
//            if (null != renderedPostSqls && !renderedPostSqls.isEmpty()) {
//                String username = options.getUsername();
//                String password = options.getPassword();
//                String jdbcUrl = options.getJdbcUrl();
//                try (Connection conn = DBUtil.getConnection(this.dsFactoryGetter, jdbcUrl, username, password)) {
//                    LOG.info("Begin to execute preSqls:[{}]. context info:{}.", String.join(";", renderedPostSqls), jdbcUrl);
//                    DorisWriterUtil.executeSqls(conn, renderedPostSqls);
////                    DBUtil.closeDBResources(null, null, conn);
//                } catch (SQLException e) {
//                    logger.warn(e.getMessage(), e);
//                }
//
//            }
//        }
//
//        @Override
//        public void destroy() {
//        }
//
//    }
//
//    public static class Task extends Writer.Task {
//        private DorisWriterManager writerManager;
//        private DorisWriterOptions options;
//        private DorisISerializer rowSerializer;
//
//        @Override
//        public void init() {
//            options = new DorisWriterOptions(super.getPluginJobConf());
//            writerManager = new DorisWriterManager(options);
//            rowSerializer = DorisSerializerFactory.createSerializer(options);
//        }
//
//        @Override
//        public void prepare() {
//        }
//
//        public void startWrite(RecordReceiver recordReceiver) {
//            try {
//                Record record;
//                while ((record = recordReceiver.getFromReader()) != null) {
//                    if (record.getColumnNumber() != options.getColumns().size()) {
//                        throw DataXException
//                                .asDataXException(
//                                        DBUtilErrorCode.CONF_ERROR,
//                                        String.format(
//                                                "列配置信息有错误. 因为您配置的任务中，源头读取字段数:%s 与 目的表要写入的字段数:%s 不相等. 请检查您的配置并作出修改.",
//                                                record.getColumnNumber(),
//                                                options.getColumns().size()));
//                    }
//                    writerManager.writeRecord(rowSerializer.serialize(record));
//                }
//            } catch (Exception e) {
//                throw DataXException.asDataXException(DBUtilErrorCode.WRITE_DATA_ERROR, e);
//            }
//        }
//
//        @Override
//        public void post() {
//            try {
//                writerManager.close();
//            } catch (Exception e) {
//                throw DataXException.asDataXException(DBUtilErrorCode.WRITE_DATA_ERROR, e);
//            }
//        }
//
//        @Override
//        public void destroy() {
//        }
//
//        @Override
//        public boolean supportFailOver() {
//            return false;
//        }
//    }
//}
