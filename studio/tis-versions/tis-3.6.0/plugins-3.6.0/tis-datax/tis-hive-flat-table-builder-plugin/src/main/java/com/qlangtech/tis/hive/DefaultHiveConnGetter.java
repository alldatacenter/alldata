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

package com.qlangtech.tis.hive;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.config.hive.HiveUserToken;
import com.qlangtech.tis.config.hive.IHiveConnGetter;
import com.qlangtech.tis.config.hive.IHiveUserTokenVisitor;
import com.qlangtech.tis.config.hive.impl.IKerberosUserToken;
import com.qlangtech.tis.config.hive.impl.OffHiveUserToken;
import com.qlangtech.tis.config.hive.meta.HiveTable;
import com.qlangtech.tis.config.hive.meta.IHiveMetaStore;
import com.qlangtech.tis.dump.hive.HiveDBUtils;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.offline.flattable.HiveFlatTableBuilder;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.ql.metadata.Hive;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-28 10:50
 **/
@Public
public class DefaultHiveConnGetter extends ParamsConfig implements IHiveConnGetter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultHiveConnGetter.class);

    public static final String KEY_HIVE_ADDRESS = "hiveAddress";
    // public static final String KEY_USE_USERTOKEN = "useUserToken";
//    public static final String KEY_USER_NAME = "userName";
//    public static final String KEY_PASSWORD = "password";
    public static final String KEY_META_STORE_URLS = "metaStoreUrls";
    public static final String KEY_DB_NAME = "dbName";

    @FormField(ordinal = 0, validate = {Validator.require, Validator.identity}, identity = true)
    public String name;

    @Override
    public String identityValue() {
        return this.name;
    }

    @FormField(ordinal = 1, validate = {Validator.require, Validator.db_col_name})
    public String dbName;
    @FormField(ordinal = 2, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String metaStoreUrls;

    @FormField(ordinal = 3, validate = {Validator.require, Validator.host})
    public String // "192.168.28.200:10000";
            hiveAddress;


//    @FormField(ordinal = 4, validate = {Validator.require}, type = FormFieldType.ENUM)
//    public boolean useUserToken;

    @FormField(ordinal = 5, validate = {Validator.require})
    public HiveUserToken userToken;

//    @FormField(ordinal = 6, type = FormFieldType.PASSWORD, validate = {})
//    public String password;

    @Override
    public String getDbName() {
        return this.dbName;
    }

    @Override
    public String getMetaStoreUrls() {
        return this.metaStoreUrls;
    }

    @Override
    public Connection createConfigInstance() {
        try {
            return HiveDBUtils.getInstance(this.hiveAddress, this.dbName, getUserToken()).createConnection();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IHiveMetaStore createMetaStoreClient() {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(DefaultHiveConnGetter.class.getClassLoader());
            HiveConf hiveCfg = new HiveConf();
            hiveCfg.set(HiveConf.ConfVars.METASTOREURIS.varname, this.metaStoreUrls);

            HiveUserToken userToken = getUserToken();
            //if (userToken.isPresent()) {
            // HiveUserToken hiveToken = userToken.get();
            userToken.accept(new IHiveUserTokenVisitor() {
                @Override
                public void visit(IKerberosUserToken token) {
                    token.getKerberosCfg().setConfiguration(hiveCfg);
                }
            });
            //}

            final IMetaStoreClient storeClient = Hive.get(hiveCfg, false).getMSC();
            return new IHiveMetaStore() {
                @Override
                public HiveTable getTable(String database, String tableName) {
                    try {
                        // storeClient.getta
                        Table table = storeClient.getTable(database, tableName);
                        StorageDescriptor storageDesc = table.getSd();
                        return new HiveTable(table.getTableName()) {
                            @Override
                            public String getStorageLocation() {
                                return storageDesc.getLocation();
                            }
                        };
                    } catch (NoSuchObjectException e) {
                        logger.warn(database + "." + tableName + " is not exist in hive:" + metaStoreUrls);
                        return null;
                    } catch (TException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void close() throws IOException {
                    storeClient.close();
                }
            };
        } catch (
                Exception e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    // @Override
    public HiveUserToken getUserToken() {
//        return this.useUserToken
//                ? Optional.of(new HiveUserToken(this.userName, this.password)) : Optional.empty();
        if (this.userToken == null) {
            // throw new IllegalStateException("hive userToken can not be null");
            return new OffHiveUserToken();
        }
        return this.userToken;
    }


    @Override
    public String getJdbcUrl() {
        return IHiveConnGetter.HIVE2_JDBC_SCHEMA + this.hiveAddress;

//        Objects.requireNonNull(userToken, "userToken can not be null").accept(new IHiveUserTokenVisitor() {
//            @Override
//            public void visit(DefaultHiveUserToken token) {
//
//            }
//
//            @Override
//            public void visit(KerberosUserToken token) {
//                KerberosCfg kerberosCfg = (KerberosCfg) token.getKerberosCfg();
//                jdbcUrl.append(";principal=")
//                        .append(kerberosCfg.principal)
//                        .append(";sasl.qop=").append(kerberosCfg.getKeyTabPath().getAbsolutePath());
//            }
//        });
//
//        if (userToken.isPresent()) {
//            userToken.get().accept(new IHiveUserTokenVisitor() {
//                @Override
//                public void visit(DefaultHiveUserToken ut) {
//                    hiveDatasource.setUsername(ut.userName);
//                    hiveDatasource.setPassword(ut.password);
//                }
//
//
//            });
//
//        }

    }

    @TISExtension()
    public static class DefaultDescriptor extends Descriptor<ParamsConfig> {
        public DefaultDescriptor() {
            super();
            // this.registerSelectOptions(HiveFlatTableBuilder.KEY_FIELD_NAME, () -> TIS.getPluginStore(FileSystemFactory.class).getPlugins());
        }

        public boolean validateMetaStoreUrls(IFieldErrorHandler msgHandler, Context context, String fieldName, String metaUrls) {
            Pattern PATTERN_THRIFT_URL = Pattern.compile("thrift://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");

            Matcher matcher = PATTERN_THRIFT_URL.matcher(metaUrls);
            if (!matcher.matches()) {
                msgHandler.addFieldError(context, fieldName, "value:\"" + metaUrls + "\" not match " + PATTERN_THRIFT_URL);
                return false;
            }

            return true;
        }

//        @Override
//        protected boolean validateAll(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
//            return this.verify(msgHandler, context, postFormVals);
//        }

        @Override
        protected boolean verify(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {

            DefaultHiveConnGetter params = (DefaultHiveConnGetter) postFormVals.newInstance(this, msgHandler);

//            String metaUrls = postFormVals.getField(KEY_META_STORE_URLS);
//            String dbName = postFormVals.getField(KEY_DB_NAME);
            if (!this.validateMetaStoreUrls(msgHandler, context, KEY_META_STORE_URLS, params.getMetaStoreUrls())) {
                return false;
            }

            final ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(DefaultHiveConnGetter.class.getClassLoader());
                HiveConf conf = new HiveConf(new Configuration(false), HiveConf.class);
                conf.set(HiveConf.ConfVars.METASTOREURIS.varname, params.getMetaStoreUrls());
                Hive hive = Hive.get(conf);
                Database database = hive.getDatabase(params.getDbName());
                if (database == null) {
                    msgHandler.addFieldError(context, KEY_DB_NAME, "DB:" + params.getDbName() + " 请先在库中创建");
                    return false;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                msgHandler.addFieldError(context, KEY_META_STORE_URLS, "请检查地址是否可用，" + e.getMessage());
                return false;
            } finally {
                Thread.currentThread().setContextClassLoader(currentLoader);
                try {
                    Hive.closeCurrent();
                } catch (Throwable e) { }
            }


            if (!HiveFlatTableBuilder.validateHiveAvailable(msgHandler, context, params)) {
                return false;
            }
            return super.verify(msgHandler, context, postFormVals);
        }

        @Override
        public String getDisplayName() {
            return PLUGIN_NAME;
        }
    }


}
