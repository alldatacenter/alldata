///**
// * Copyright (c) 2020 QingLang, Inc. <baisui@qlangtech.com>
// *
// * This program is free software: you can use, redistribute, and/or modify
// * it under the terms of the GNU Affero General Public License, version 3
// * or later ("AGPL"), as published by the Free Software Foundation.
// *
// * This program is distributed in the hope that it will be useful, but WITHOUT
// * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// * FITNESS FOR A PARTICULAR PURPOSE.
// *
// * You should have received a copy of the GNU Affero General Public License
// * along with this program. If not, see <http://www.gnu.org/licenses/>.
// */
//package com.qlangtech.tis.manage.common;
//
//import com.qlangtech.tis.db.parser.domain.DBConfig;
//import org.apache.commons.lang.StringUtils;
//import org.apache.commons.lang.exception.ExceptionUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.BeansException;
//import org.springframework.beans.factory.InitializingBean;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.ApplicationContextAware;
//import javax.sql.DataSource;
//import java.sql.Connection;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2016年1月17日 下午2:45:47
// */
//public class DataSourceRegister implements ApplicationContextAware, InitializingBean {
//
//    private static final Logger log = LoggerFactory.getLogger(DataSourceRegister.class);
//
//    private String processDbs;
//
//    public String getProcessDbs() {
//        return processDbs;
//    }
//
//    public void setProcessDbs(String processDbs) {
//        this.processDbs = processDbs;
//    }
//
//    public abstract static class DBRegister {
//
//        // 由于facade的dbname会和detail的不一样，所以需要额外在加一个供注册到spring datasource中作为id用
//        private final String dbName;
//
//        private final DBConfig dbConfig;
//
//        public DBRegister(String dbName, DBConfig dbConfig) {
//            this.dbName = dbName;
//            this.dbConfig = dbConfig;
//        }
//
//        protected abstract void createDefinition(String dbDefinitionId, String driverClassName, String jdbcUrl, String userName, String password);
//
//        /**
//         * 读取多个数据源中的一个一般是用于读取数据源Meta信息用
//         *
//         * @throws BeansException
//         */
//        public void visitFirst() throws BeansException {
//            this.setApplicationContext(false, true);
//        }
//
//        /**
//         * 读取所有可访问的数据源
//         */
//        public void visitAll() {
//            this.setApplicationContext(true, false);
//        }
//
//        private void setApplicationContext(boolean resolveHostIp, boolean facade) throws BeansException {
//            this.dbConfig.vistDbURL(resolveHostIp, (dbName, jdbcUrl) -> {
//                final String dbDefinitionId = (facade ? DBRegister.this.dbName : dbName);
//                createDefinition(dbDefinitionId, "com.mysql.jdbc.Driver", jdbcUrl, dbConfig.getUserName(), dbConfig.getPassword());
//            }, facade);
//        }
//    }
//
//    private ApplicationContext applicationContext;
//
//    @Override
//    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//        this.applicationContext = applicationContext;
//    }
//
//    @Override
//    public void afterPropertiesSet() throws Exception {
//        // 校验bean 是否正确
//        String[] dbs = StringUtils.split(processDbs, ";");
//        DataSource ds = null;
//        StringBuffer buffer = new StringBuffer();
//        for (String db : dbs) {
//            ds = (DataSource) applicationContext.getBean(db);
//            if (ds == null) {
//                throw new IllegalStateException("db:" + db + " is not defined");
//            }
//            if (!isOK(ds, buffer)) {
//                buffer.append(db).append(",");
//            }
//        }
//        if (buffer.length() > 0) {
//            log.error("db config has some error," + buffer);
//            throw new IllegalStateException("db has some error:" + buffer.toString());
//        }
//    }
//
//    private boolean hasRrcordDbError = false;
//
//    /**
//     * @param ds
//     * @throws SQLException
//     */
//    private boolean isOK(DataSource ds, StringBuffer errorRecord) throws SQLException {
//        try {
//            Connection conn;
//            Statement statement;
//            ResultSet result;
//            conn = ds.getConnection();
//            statement = conn.createStatement();
//            result = statement.executeQuery("select 1");
//            if (result.next()) {
//                if (result.getInt(1) != 1) {
//                    return false;
//                }
//            } else {
//                return false;
//            }
//            result.close();
//            statement.close();
//            conn.close();
//            return true;
//        } catch (Exception e) {
//            if (!hasRrcordDbError) {
//                errorRecord.append(ExceptionUtils.getStackTrace(e));
//                hasRrcordDbError = true;
//            }
//            return false;
//        }
//    }
//}
