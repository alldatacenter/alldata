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

package com.qlangtech.tis.plugin.ds.mangodb;

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Lists;
import com.mongodb.AuthenticationMechanism;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.lang.StringUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-06 15:25
 **/
@Public
public class MangoDBDataSourceFactory extends DataSourceFactory {

    private static final String DS_TYPE_MONGO_DB = "MongoDB";


    @FormField(identity = true, ordinal = 0, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.identity})
    public String name;

    @FormField(ordinal = 1, type = FormFieldType.TEXTAREA, validate = {Validator.require})
    public String address;
    @FormField(ordinal = 2, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.db_col_name})
    public String dbName;
    @FormField(ordinal = 3, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String username;

    @FormField(ordinal = 4, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.identity})
    public String userSource;

    @FormField(ordinal = 5, type = FormFieldType.PASSWORD, validate = {})
    public String password;

    @FormField(ordinal = 6, type = FormFieldType.ENUM, validate = {Validator.require})
    public String authMechanism;

    public String getDbName() {
        return this.dbName;
    }

    public boolean isContainCredential() {
        return StringUtils.isNotBlank(this.username) && StringUtils.isNotBlank(this.password);
    }

    @Override
    public void refresh() {

    }

    @Override
    public String identityValue() {
        return this.name;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (!isWrapperFor(iface)) {
            throw new IllegalStateException(" is not wrapper for :" + iface.getName());
        }
        return (T) createMongoClient();
    }


    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface == MongoClient.class;
    }

    public String getUserName() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    @Override
    public DataDumpers getDataDumpers(TISTable table) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TableInDB getTablesInDB() {
        MongoClient mongoClient = null;
        TableInDB tabs = TableInDB.create(this);
        try {
            mongoClient = createMongoClient();
            MongoDatabase database = mongoClient.getDatabase(this.dbName);
            for (String tab : database.listCollectionNames()) {
                tabs.add(this.address, tab);
            }
            //  Lists.newArrayList(database.listCollectionNames());
            return tabs;
        } finally {
            try {
                mongoClient.close();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public DBConfig getDbConfig() {
        throw new IllegalStateException();
    }

    @Override
    public List<ColumnMetaData> getTableMetadata(boolean inSink, EntityName table) {
//        MongoClient mongoClient = null;
//        try {
//            mongoClient = createMongoClient();
//            MongoDatabase database = mongoClient.getDatabase(this.dbName);
//            MongoCollection collection = database.getCollection(table);
//            //collection.getReadConcern()
//            //  collection.find().map()
//        } finally {
//            try {
//                mongoClient.close();
//            } catch (Throwable e) {
//            }
//        }
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitFirstConnection(IConnProcessor connProcessor) {
        throw new UnsupportedOperationException();
    }

//    @Override
//    public void refectTableInDB(TableInDB tabs, Connection conn) throws SQLException {
//        throw new UnsupportedOperationException();
//    }

    private MongoClient createMongoClient() {
        MongoClient mongoClient = null;
        List<String> addressList = getAddressList(this.address); //conf.getList(KeyConstant.MONGO_ADDRESS);
        // try {
        if (StringUtils.isNotBlank(this.username) && StringUtils.isNotBlank(this.password)) {
            MongoCredential credential = null;
            AuthenticationMechanism aMechanism = AuthenticationMechanism.fromMechanismName(this.authMechanism);
            switch (aMechanism) {
                case PLAIN:
                    credential = MongoCredential.createPlainCredential(this.username, this.userSource, password.toCharArray());
                    break;
                case GSSAPI:
                    credential = MongoCredential.createGSSAPICredential(this.username);
                    break;
                case MONGODB_CR:
                    credential = MongoCredential.createMongoCRCredential(this.username, this.userSource, password.toCharArray());
                    break;
                case SCRAM_SHA_1:
                    credential = MongoCredential.createScramSha1Credential(this.username, this.userSource, password.toCharArray());
                    break;
                case MONGODB_X509:
                    credential = MongoCredential.createMongoX509Credential(this.username);
                    break;
                default:
                    throw new IllegalStateException("illegal authMechanism:" + aMechanism);
            }

            mongoClient = new MongoClient(parseServerAddress(addressList), Collections.singletonList(credential));
        } else {
            mongoClient = new MongoClient(parseServerAddress(addressList));
        }
        // mongoClient.close();
        return mongoClient;
    }

    public static List<Option> allAuthMechanism() {
        return Arrays.stream(AuthenticationMechanism.values())
                .map((e) -> new Option(e.getMechanismName(), e.getMechanismName()))
                .collect(Collectors.toList());
    }


    public static List<String> getAddressList(String address) {
        return Lists.newArrayList(StringUtils.split(address, ";"));
    }


    private static List<ServerAddress> parseServerAddress(List<String> rawAddressList) {
        List<ServerAddress> addressList = new ArrayList<ServerAddress>();
        for (String address : rawAddressList) {
            String[] tempAddress = StringUtils.split(address, ":");// .split(":");
            ServerAddress sa = new ServerAddress(tempAddress[0], Integer.valueOf(tempAddress[1]));
            addressList.add(sa);
        }
        return addressList;
    }


    @TISExtension
    public static class DefaultDescriptor extends DataSourceFactory.BaseDataSourceFactoryDescriptor {
        @Override
        protected String getDataSourceName() {
            return DS_TYPE_MONGO_DB;
        }

        @Override
        public EndType getEndType() {
            return EndType.MongoDB;
        }

        @Override
        public boolean supportFacade() {
            return false;
        }

        public boolean validateAddress(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            try {
                List<String> addressList = getAddressList(value);
                for (String address : addressList) {
                    if (!Validator.host.validate(msgHandler, context, fieldName, address)) {
                        return false;
                    }
                }

                List<ServerAddress> serverAddresses = parseServerAddress(addressList);
                if (serverAddresses.size() < 1) {
                    msgHandler.addFieldError(context, fieldName, "请填写");
                    return false;
                }
            } catch (Throwable e) {
                msgHandler.addFieldError(context, fieldName, "格式有误");
                return false;
            }
            return true;
        }

    }
}
