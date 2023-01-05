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
package com.qlangtech.tis.dump;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.TisZkClient;
import com.qlangtech.tis.build.task.TaskMapper;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.fs.ITISFileSystemFactory;
import com.qlangtech.tis.fs.ITableBuildTask;
import com.qlangtech.tis.fs.ITaskContext;
import com.qlangtech.tis.fs.local.LocalFileSystem;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteJobTrigger;
import com.qlangtech.tis.fullbuild.indexbuild.RunningStatus;
import com.qlangtech.tis.fullbuild.indexbuild.TaskContext;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.offline.DbScope;
import com.qlangtech.tis.offline.TableDumpFactory;
import com.qlangtech.tis.order.dump.task.SingleTableDumpTask;
import com.qlangtech.tis.plugin.IPluginStore;
import com.qlangtech.tis.plugin.PluginStore;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.PostedDSProp;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.tis.hadoop.rpc.RpcServiceReference;
import com.tis.hadoop.rpc.StatusRpcClient;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author: baisui 百岁
 * @create: 2021-03-03 09:10
 **/
public class LocalTableDumpFactory extends TableDumpFactory implements ITISFileSystemFactory {
    private static final Logger logger = LoggerFactory.getLogger(LocalTableDumpFactory.class);

    @FormField(identity = true, ordinal = 0, validate = {Validator.require, Validator.identity})
    public String name;

    private transient ITISFileSystem fileSystem;

    private transient DetailedDataSourceFactoryGetter dataSourceFactoryGetter;


    private DataSourceFactory getDataSourceFactory(IDumpTable table) {
        if (dataSourceFactoryGetter == null) {
            dataSourceFactoryGetter = (tab) -> {
                IPluginStore<DataSourceFactory> dbPlugin = TIS.getDataBasePluginStore(new PostedDSProp(tab.getDbName(), DbScope.DETAILED));
                Objects.requireNonNull(dbPlugin, "dbPlugin can not be null");
                DataSourceFactory dsFactory = dbPlugin.getPlugin();
                if (dsFactory == null) {
                    throw new IllegalStateException("table:" + table + " can not find relevant ds config,config file:" + dbPlugin.getTargetFile());
                }
                return dsFactory;
            };
        }
        return dataSourceFactoryGetter.get(table);
    }

    public interface DetailedDataSourceFactoryGetter {
        DataSourceFactory get(IDumpTable table);
    }

    public void setDataSourceFactoryGetter(DetailedDataSourceFactoryGetter dataSourceFactoryGetter) {
        this.dataSourceFactoryGetter = dataSourceFactoryGetter;
    }

    public static File getLocalOfflineRootDir() {
        try {
            File localOfflineRoot = new File(Config.getDataDir(), "localOffline");
            FileUtils.forceMkdir(localOfflineRoot);
            return localOfflineRoot;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public ITISFileSystem getFileSystem() {
        if (fileSystem == null) {
            fileSystem = new LocalFileSystem(getLocalOfflineRootDir().getAbsolutePath());
        }
        return fileSystem;
    }


    @Override
    public IRemoteJobTrigger createSingleTableDumpJob(final IDumpTable table, TaskContext context) {

        return triggerTask(context, (rpc) -> {
            SingleTableDumpTask tableDumpTask = new SingleTableDumpTask((EntityName) table, LocalTableDumpFactory.this
                    , getDataSourceFactory(table), context.getCoordinator().unwrap(), rpc) {
                protected void registerZKDumpNodeIn(TaskContext context) {
                }
            };
            // 开始执行数据dump
            tableDumpTask.map(context);
        });
    }


    public static IRemoteJobTrigger triggerTask(TaskContext context, ILocalTask task) {
        TisZkClient zk = context.getCoordinator().unwrap();
        Objects.requireNonNull(zk, "zk(TisZkClient) can not be null");

        AtomicReference<Throwable> errRef = new AtomicReference<>();
        CountDownLatch countDown = new CountDownLatch(1);
        final ExecutorService executor = Executors.newSingleThreadExecutor((r) -> {
            Thread t = new Thread(r);
            t.setUncaughtExceptionHandler((thread, e) -> {
                errRef.set(e);
                logger.error(e.getMessage(), e);
                countDown.countDown();
            });
            return t;
        });

        return new IRemoteJobTrigger() {
            @Override
            public void submitJob() {

                executor.execute(() -> {
                    RpcServiceReference statusRpc = null;
                    try {
                        statusRpc = StatusRpcClient.getService(zk);
                        task.process(statusRpc);
                        countDown.countDown();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            statusRpc.get().close();
                        } catch (Throwable e) {

                        }
                    }
                });
            }

            @Override
            public RunningStatus getRunningStatus() {
                RunningStatus runningStatus = null;
                // 反馈执行状态
                if (countDown.getCount() > 0) {
                    runningStatus = new RunningStatus(0, false, false);
                } else {
                    executor.shutdown();
                    runningStatus = new RunningStatus(1, true, errRef.get() == null);
                }

                return runningStatus;
            }
        };
    }

    public interface ILocalTask {
        public void process(RpcServiceReference statusRpc) throws Exception;
    }


    @Override
    public void bindTables(Set<EntityName> hiveTables, String timestamp, ITaskContext context) {

    }

    @Override
    public void deleteHistoryFile(EntityName dumpTable, ITaskContext taskContext) {
        try {
            getFileSystem().deleteHistoryFile(dumpTable);
        } catch (Exception e) {
            throw new RuntimeException("delete table:" + dumpTable.toString() + " relevant history file", e);
        }
    }

    @Override
    public void deleteHistoryFile(EntityName dumpTable, ITaskContext taskContext, String timestamp) {
        try {
            getFileSystem().deleteHistoryFile(dumpTable, timestamp);
        } catch (Exception e) {
            throw new RuntimeException("delete table:" + dumpTable.toString()
                    + ",timestamp:" + timestamp + " relevant history file,", e);
        }
    }

    @Override
    public void dropHistoryTable(EntityName dumpTable, ITaskContext taskContext) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void startTask(TaskMapper taskMapper, TaskContext taskContext) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startTask(ITableBuildTask dumpTask) {
        ITaskContext ctx = new ITaskContext() {
            @Override
            public Object getObj() {
                throw new UnsupportedOperationException();
            }
        };
        try {
            dumpTask.process(ctx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @TISExtension()
    public static class DefaultDescriptor extends Descriptor<TableDumpFactory> {
        @Override
        public String getDisplayName() {
            return "localDump";
        }

        public boolean validateRootDir(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            File rootDir = new File(value);
            if (!rootDir.exists()) {
                msgHandler.addFieldError(context, fieldName, "path:" + rootDir.getAbsolutePath() + " is not exist");
                return false;
            }
            return true;
        }

    }
}
