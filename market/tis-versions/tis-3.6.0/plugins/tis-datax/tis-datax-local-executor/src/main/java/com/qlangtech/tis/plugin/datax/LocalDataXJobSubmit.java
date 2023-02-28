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

import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.datax.CuratorDataXTaskMessage;
import com.qlangtech.tis.datax.DataXJobSubmit;
import com.qlangtech.tis.datax.DataxExecutor;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.order.center.IJoinTaskContext;
import com.qlangtech.tis.web.start.TisSubModule;
import com.tis.hadoop.rpc.RpcServiceReference;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-04-27 17:28
 **/
@TISExtension()
@Public
public class LocalDataXJobSubmit extends DataXJobSubmit {

    private String mainClassName = DataxExecutor.class.getName();
    private File workingDirectory = new File(".");
    private String classpath;

    private final static Logger logger = LoggerFactory.getLogger(LocalDataXJobSubmit.class);

    @Override
    public InstanceType getType() {
        return InstanceType.LOCAL;
    }

    @Override
    public IRemoteTaskTrigger createDataXJob(DataXJobSubmit.IDataXJobContext taskContext, RpcServiceReference statusRpc
            , IDataxProcessor dataxProcessor, String dataXfileName, List<String> dependencyTasks) {
        if (StringUtils.isEmpty(this.classpath)) {
            File assebleDir = new File(Config.getTisHome(), TisSubModule.TIS_ASSEMBLE.moduleName);
            File localExecutorLibDir = new File(Config.getLibDir(), "plugins/tis-datax-local-executor/WEB-INF/lib");
            File webStartDir = new File(Config.getTisHome(), TisSubModule.WEB_START.moduleName + "/lib");
            if (!localExecutorLibDir.exists()) {
                throw new IllegalStateException("target localExecutorLibDir dir is not exist:" + localExecutorLibDir.getAbsolutePath());
            }
            if (!assebleDir.exists()) {
                throw new IllegalStateException("target asseble dir is not exist:" + assebleDir.getAbsolutePath());
            }
            if (!webStartDir.exists()) {
                throw new IllegalStateException("target " + TisSubModule.WEB_START.moduleName + "/lib dir is not exist:" + webStartDir.getAbsolutePath());
            }
            this.classpath = assebleDir.getPath() + "/lib/*:" + localExecutorLibDir.getPath()
                    + "/*:" + assebleDir.getPath() + "/conf:" + new File(webStartDir, "*").getPath();
        }
        logger.info("dataX Job:{},classpath:{},workingDir:{}", dataXfileName, this.classpath, workingDirectory.getPath());

        Objects.requireNonNull(statusRpc, "statusRpc can not be null");
        return TaskExec.getRemoteJobTrigger(taskContext, this, dataXfileName, dependencyTasks);
    }


    @Override
    public DataXJobSubmit.IDataXJobContext createJobContext(final IJoinTaskContext parentContext) {
        return new DataXJobSubmit.IDataXJobContext() {
            @Override
            public IJoinTaskContext getTaskContext() {
                return parentContext;
            }

            @Override
            public void destroy() {
            }
        };
    }

    public void setMainClassName(String mainClassName) {
        this.mainClassName = mainClassName;
    }


    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String getMainClassName() {
        return mainClassName;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public String getClasspath() {
        if (StringUtils.isEmpty(this.classpath)) {
            throw new IllegalStateException("param classpath can not be null");
        }
        return classpath;
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    @Override
    public CuratorDataXTaskMessage getDataXJobDTO(IJoinTaskContext taskContext, String dataXfileName) {
        return super.getDataXJobDTO(taskContext, dataXfileName);
    }
}
