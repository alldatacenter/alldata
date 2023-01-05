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

package com.qlangtech.tis.plugin.ds.sqlserver;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.DBConfig;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-07 09:47
 **/
@Public
public class SqlServerDatasourceFactory extends BasicDataSourceFactory {
    private static final String DS_TYPE_SQL_SERVER = "SqlServer";

    @Override
    public String buidJdbcUrl(DBConfig db, String ip, String dbName) {
        String jdbcUrl = "jdbc:sqlserver://" + ip + ":" + this.port + ";databaseName=" + dbName + ";user=" + this.userName + ";password=" + password;
        if (StringUtils.isNotEmpty(this.extraParams)) {
            jdbcUrl = jdbcUrl + ";" + this.extraParams;
        }
        return jdbcUrl;
    }

    @Override
    protected String getRefectTablesSql() {
        return "select name from sys.tables where is_ms_shipped = 0";
    }

    @Override
    public String getEscapeChar() {
        return "\"";
    }

    @Override
    public Connection getConnection(String jdbcUrl) throws SQLException {
        // return DriverManager.getConnection(jdbcUrl, StringUtils.trimToNull(this.userName), StringUtils.trimToNull(password));
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(e.getMessage(), e);
        }
        return DriverManager.getConnection(jdbcUrl);
    }

    @TISExtension
    public static class DefaultDescriptor extends BasicRdbmsDataSourceFactoryDescriptor {
        private static final Pattern urlParamsPattern = Pattern.compile("(\\w+?\\=\\w+?)(\\;\\w+?\\=\\w+?)*");

        @Override
        protected String getDataSourceName() {
            return DS_TYPE_SQL_SERVER;
        }

        @Override
        public boolean supportFacade() {
            return false;
        }

        @Override
        public boolean validateExtraParams(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            Matcher matcher = urlParamsPattern.matcher(value);
            if (!matcher.matches()) {
                msgHandler.addFieldError(context, fieldName, "不符合格式：" + urlParamsPattern);
                return false;
            }
            return true;
        }
    }
}
