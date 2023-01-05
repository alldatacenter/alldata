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

package com.qlangtech.tis.plugin.datax;

import com.alibaba.datax.core.util.container.JarLoader;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.datax.*;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.order.center.IJoinTaskContext;
import com.tis.hadoop.rpc.RpcServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 测试用让实例与assemble节点在同一个VM中跑
 * 需要在 tis-assemble工程中添加
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-04-22 20:29
 **/
@TISExtension()
@Public
public class EmbeddedDataXJobSubmit extends DataXJobSubmit {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedDataXJobSubmit.class);

    private transient JarLoader uberClassLoader;

    @Override
    public InstanceType getType() {
        return InstanceType.EMBEDDED;
    }

    @Override
    public IRemoteTaskTrigger createDataXJob(IDataXJobContext taskContext, RpcServiceReference statusRpc
            , IDataxProcessor dataxProcessor, String dataXfileName, List<String> dependencyTasks) {


        CuratorDataXTaskMessage jobDTO = getDataXJobDTO(taskContext.getTaskContext(), dataXfileName);
        Integer jobId = jobDTO.getJobId();
        String jobName = jobDTO.getJobName();
        String dataXName = jobDTO.getDataXName();


        final DataxExecutor dataxExecutor
                = new DataxExecutor(statusRpc, InstanceType.EMBEDDED, jobDTO.getAllRowsApproximately());

        if (uberClassLoader == null) {
            uberClassLoader = new TISJarLoader(TIS.get().getPluginManager());
        }

        return new IRemoteTaskTrigger() {
            @Override
            public String getTaskName() {
                return dataXfileName;
            }

            @Override
            public List<String> getTaskDependencies() {
                return dependencyTasks;
            }

            @Override
            public void run() {
                try {
                    dataxExecutor.reportDataXJobStatus(false, false, false, jobId, jobName);
                    dataxExecutor.exec(uberClassLoader, jobId, jobName, dataXName);
                    dataxExecutor.reportDataXJobStatus(false, jobId, jobName);
                } catch (Throwable e) {
                    dataxExecutor.reportDataXJobStatus(true, jobId, jobName);
                    //logger.error(e.getMessage(), e);
                    try {
                        //确保日志向远端写入了
                        Thread.sleep(3000);
                    } catch (InterruptedException ex) {

                    }
                    throw new RuntimeException(e);
                }

            }
        };
    }

    @Override
    public IDataXJobContext createJobContext(IJoinTaskContext parentContext) {
        return new DataXJobSubmit.IDataXJobContext() {
//            @Override
//            public ExecutorService getContextInstance() {
//                return executorService;
//            }

            @Override
            public IJoinTaskContext getTaskContext() {
                return parentContext;
            }

            @Override
            public void destroy() {
                // executorService.shutdownNow();
            }
        };
    }
}
