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
package com.qlangtech.tis.offline.flattable;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.dump.hive.HiveDBUtils;
import com.qlangtech.tis.hive.DefaultHiveConnGetter;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.sql.Connection;

//import com.qlangtech.tis.fullbuild.taskflow.hive.HiveTaskFactory;

//
///**
// * HIVE 宽表构建task
// * @create: 2020-04-03 12:12
// *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2020/04/13
// */
@Public
public class HiveFlatTableBuilder  //extends FlatTableBuilder
{
    //
//
    public static final String KEY_HIVE_ADDRESS = "hiveAddress";
    public static final String KEY_DB_NAME = "dbName";

    //
//    @FormField(identity = true, ordinal = 0, validate = {Validator.require, Validator.identity})
//    public String name;
//
//    @FormField(ordinal = 1, validate = {Validator.require, Validator.host})
//    public String // "jdbc:hive2://10.1.5.68:10000/tis";
//            hiveAddress;
//
//    @FormField(ordinal = 2, validate = {Validator.require, Validator.identity})
//    public String dbName;
//
//    @FormField(ordinal = 3, validate = {Validator.require, Validator.identity}, type = FormFieldType.SELECTABLE)
//    public String fsName;
//
//    private FileSystemFactory fileSystem;
//
//    private FileSystemFactory getFs() {
//        if (fileSystem == null) {
//            this.fileSystem = FileSystemFactory.getFsFactory(fsName);
//        }
//        Objects.requireNonNull(this.fileSystem, "fileSystem has not be initialized");
//        return fileSystem;
//    }
//
//    private HiveTaskFactory taskFactory;
//
//    @Override
//    public DataflowTask createTask(ISqlTask nodeMeta, boolean isFinalNode
//            , ITemplateContext tplContext, ITaskContext taskContext, //
//                                   IJoinTaskStatus joinTaskStatus) {
//        HiveTaskFactory taskFactory = getTaskFactory(tplContext);
//        return taskFactory.createTask(nodeMeta, isFinalNode, tplContext, taskContext, joinTaskStatus);
//    }
//
//    private HiveTaskFactory getTaskFactory(ITemplateContext tplContext) {
//        IPrimaryTabFinder erRules = tplContext.getExecContext().getAttribute(IFullBuildContext.KEY_ER_RULES);
//        Objects.requireNonNull(erRules, "erRule can not be null");
//        Objects.requireNonNull(getFs(), "join relevant FS can not be null");
//        this.taskFactory = new HiveTaskFactory(erRules, getFs());
//        return taskFactory;
//    }
//
//    @Override
//    public void startTask(ITableBuildTask dumpTask) {
//        final Connection conn = getConnection();
//        final DelegatingConnection delegate = new DelegatingConnection(conn) {
//            @Override
//            public void close() throws SQLException {
//                throw new UnsupportedOperationException("in exec phrase close is not supported");
//            }
//        };
//        ITaskContext context = new ITaskContext() {
//            @Override
//            public Connection getObj() {
//                return delegate;
//            }
//        };
//        try {
//            dumpTask.process(context);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        } finally {
//            try {
//                conn.close();
//            } catch (Exception e) {
//            }
//        }
//    }
//
//    private Connection getConnection() {
//        try {
//            return HiveDBUtils.getInstance(this.hiveAddress, this.dbName).createConnection();
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
////    public String getJoinTableStorePath(String user, INameWithPathGetter pathGetter) {
////        return FSHistoryFileUtils.getJoinTableStorePath(user, pathGetter);
////    }
//
//    @TISExtension
//    public static class DefaultDescriptor extends Descriptor<FlatTableBuilder> {
//        public DefaultDescriptor() {
//            super();
//            this.registerSelectOptions(ITISFileSystemFactory.KEY_FIELD_NAME_FS_NAME, () -> TIS.getPluginStore(FileSystemFactory.class).getPlugins());
//        }
//
//        @Override
//        public String getDisplayName() {
//            return "hive";
//        }
//
//        @Override
//        protected boolean verify(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
//            return validateHiveAvailable(msgHandler, context, postFormVals);
//        }
//    }
//
    public static boolean validateHiveAvailable(IControlMsgHandler msgHandler, Context context, DefaultHiveConnGetter params) {
//        String hiveAddress = postFormVals.getField(KEY_HIVE_ADDRESS);
//        String dbName = postFormVals.getField(KEY_DB_NAME);

        String hiveAddress = params.hiveAddress;
        String dbName = params.dbName;

//        boolean useUserToken = Boolean.parseBoolean(postFormVals.getField(DefaultHiveConnGetter.KEY_USE_USERTOKEN));
//        HiveUserToken userToken = null;
//        if (useUserToken) {
//            userToken = new HiveUserToken(
//                    postFormVals.getField(DefaultHiveConnGetter.KEY_USER_NAME), postFormVals.getField(DefaultHiveConnGetter.KEY_PASSWORD));
//            if (StringUtils.isBlank(userToken.userName)) {
//                msgHandler.addFieldError(context, DefaultHiveConnGetter.KEY_USER_NAME, ValidatorCommons.MSG_EMPTY_INPUT_ERROR);
//                return false;
//            }
//        }

        Connection conn = null;
        try {

            conn = HiveDBUtils.getInstance(hiveAddress, dbName, params.getUserToken()).createConnection();
        } catch (Throwable e) {
            Throwable[] throwables = ExceptionUtils.getThrowables(e);
            for (Throwable t : throwables) {
                if (StringUtils.indexOf(t.getMessage(), "refused") > -1) {
                    msgHandler.addFieldError(context, KEY_HIVE_ADDRESS, "连接地址不可用，请确保连接Hive服务地址可用");
                    return false;
                }
                if (StringUtils.indexOf(t.getMessage(), "NoSuchDatabaseException") > -1) {
                    msgHandler.addFieldError(context, KEY_DB_NAME, "dbName:" + dbName + " is not exist ,please create");
                    return false;
                }
            }
            throw e;
        } finally {
            try {
                conn.close();
            } catch (Throwable e) {}
        }
        return true;
    }
}
