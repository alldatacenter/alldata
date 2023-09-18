/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.plugin.ds.doris;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.fastjson.JSONArray;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.lang.TisException;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.DBConfig;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import org.apache.commons.lang.StringUtils;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-09-07 09:44
 **/
@Public
public class DorisSourceFactory extends BasicDataSourceFactory {

    public static final String NAME_DORIS = "Doris";
    public static final String FIELD_KEY_NODEDESC = "nodeDesc";

    private static final com.mysql.jdbc.Driver mysql5Driver;

    static {
        try {
            mysql5Driver = new com.mysql.jdbc.Driver();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @FormField(ordinal = 8, type = FormFieldType.TEXTAREA, validate = {Validator.require})
    public String loadUrl;


    public List<String> getLoadUrls() {
        return DorisSourceFactory.getLoadUrls(this.loadUrl);
    }

    @Override
    public Optional<String> getEscapeChar() {
        return Optional.of("`");
    }

    @Override
    public String buidJdbcUrl(DBConfig db, String ip, String dbName) {
        StringBuffer jdbcUrl = new StringBuffer();
        jdbcUrl.append("jdbc:mysql://").append(ip).append(":").append(this.port);

        if (StringUtils.isNotEmpty(dbName)) {
            jdbcUrl.append("/").append(dbName);
        }
        return jdbcUrl.toString();
    }


    @Override
    public JDBCConnection getConnection(String jdbcUrl) throws SQLException {
        Properties props = new Properties();
        props.put("user", StringUtils.trimToEmpty(this.userName));
        if (StringUtils.isNotEmpty(this.password)) {
            props.put("password", StringUtils.trimToEmpty(this.password));
        }
        try {
            return new JDBCConnection(mysql5Driver.connect(jdbcUrl, props), jdbcUrl);
        } catch (SQLException e) {
            throw TisException.create(e.getMessage() + ",jdbcUrl:" + jdbcUrl + ",props:" + props.toString(), e);
        }
    }

    @Override
    public String identityValue() {
        return this.name;
    }

    @TISExtension
    public static class DefaultDescriptor extends BasicRdbmsDataSourceFactoryDescriptor {
        @Override
        protected String getDataSourceName() {
            return NAME_DORIS;
        }

        @Override
        public boolean supportFacade() {
            return false;
        }

        @Override
        public EndType getEndType() {
            return EndType.Doris;
        }

        @Override
        public List<String> facadeSourceTypes() {
            return Collections.emptyList();
        }

        public boolean validateLoadUrl(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {

            try {
                List<String> loadUrls = getLoadUrls(value);
                if (loadUrls.size() < 1) {
                    msgHandler.addFieldError(context, fieldName, "请填写至少一个loadUrl");
                    return false;
                }

                for (String loadUrl : loadUrls) {
                    if (!Validator.host.validate(msgHandler, context, fieldName, loadUrl)) {
                        return false;
                    }
                }

            } catch (Exception e) {
                msgHandler.addFieldError(context, fieldName, e.getMessage());
                return false;
            }

            return true;
        }

        @Override
        protected boolean validateDSFactory(IControlMsgHandler msgHandler, Context context, BasicDataSourceFactory dsFactory) {
            boolean valid = super.validateDSFactory(msgHandler, context, dsFactory);
            try {
                if (valid) {
                    int[] hostCount = new int[1];
                    DBConfig dbConfig =  dsFactory.getDbConfig();
                    dbConfig.vistDbName((config, jdbcUrl, ip, dbName) -> {
                        hostCount[0]++;
                        return false;
                    });
                    if (hostCount[0] != 1) {
                        msgHandler.addFieldError(context, FIELD_KEY_NODEDESC, "只能定义一个节点");
                        return false;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return valid;
        }

    }

    private static List<String> getLoadUrls(String value) {
        return JSONArray.parseArray(value, String.class);
    }

}
