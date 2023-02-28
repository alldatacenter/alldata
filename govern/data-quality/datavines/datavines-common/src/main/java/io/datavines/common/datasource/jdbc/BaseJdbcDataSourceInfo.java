/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.common.datasource.jdbc;

import io.datavines.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * data source base class
 */
public abstract class BaseJdbcDataSourceInfo {

    private static final Logger logger = LoggerFactory.getLogger(BaseJdbcDataSourceInfo.class);

    protected final JdbcConnectionInfo jdbcConnectionInfo;

    public BaseJdbcDataSourceInfo(JdbcConnectionInfo jdbcConnectionInfo) {
        this.jdbcConnectionInfo = jdbcConnectionInfo;
    }

    public String getUser() {
        return jdbcConnectionInfo.getUser();
    }

    public String getPassword() {
        return jdbcConnectionInfo.getPassword();
    }

    public String getHost() {
        return jdbcConnectionInfo.getHost();
    }

    public int getPort() {
        return jdbcConnectionInfo.getPort();
    }

    public String getValidationQuery() {
        return "Select 1";
    }

    public abstract String getAddress();

    public String getCatalog() {
        return jdbcConnectionInfo.getCatalog();
    }

    public String getDatabase() {
        return jdbcConnectionInfo.getDatabase();
    }

    public String getProperties() {
        return jdbcConnectionInfo.getProperties();
    }

    /**
     * @return driver class
     */
    public abstract String getDriverClass();

    /**
     * @return db type
     */
    public abstract String getType();

    /**
     * gets the JDBC url for the data source connection
     * @return getJdbcUrl
     */
    public String getJdbcUrl() {
        StringBuilder jdbcUrl = new StringBuilder(getAddress());

        appendDatabase(jdbcUrl);
        appendProperties(jdbcUrl);

        return jdbcUrl.toString();
    }

    /**
     * append database
     * @param jdbcUrl jdbc url
     */
    protected void appendCatalog(StringBuilder jdbcUrl) {
        if (StringUtils.isNotEmpty(getCatalog())) {
            if (getAddress().lastIndexOf('/') != (jdbcUrl.length() - 1)) {
                jdbcUrl.append("/");
            }
            jdbcUrl.append(getCatalog());
        }
    }

    /**
     * append database
     * @param jdbcUrl jdbc url
     */
    protected void appendDatabase(StringBuilder jdbcUrl) {
        if (StringUtils.isNotEmpty(getDatabase())) {
            if (getAddress().lastIndexOf('/') != (jdbcUrl.length() - 1)) {
                jdbcUrl.append("/");
            }
            jdbcUrl.append(getDatabase());
        }
    }

    /**
     * append other
     * @param jdbcUrl jdbc url
     */
    protected void appendProperties(StringBuilder jdbcUrl) {
        String otherParams = filterProperties(getProperties());
        if (StringUtils.isNotEmpty(otherParams)) {
            jdbcUrl.append(getSeparator()).append(otherParams);
        }
    }

    protected abstract String getSeparator();

    /**
     * the data source test connection
     * @return Connection
     * @throws Exception Exception
     */
    public Connection getConnection() throws Exception {
        Class.forName(getDriverClass());
        return DriverManager.getConnection(getJdbcUrl(), getUser(), getPassword());
    }

    protected String filterProperties(String otherParams) {
        return otherParams;
    }

    public void loadClass() {
        try {
            Class.forName(getDriverClass());
        } catch (ClassNotFoundException e) {
            logger.error("load driver error" , e);
        }
    }

    public String getUniqueKey() {
        return jdbcConnectionInfo.getUniqueKey();
    }
}
