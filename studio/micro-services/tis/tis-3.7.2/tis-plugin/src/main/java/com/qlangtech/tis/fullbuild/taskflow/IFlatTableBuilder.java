package com.qlangtech.tis.fullbuild.taskflow;

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

import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.fs.ITaskContext;
import com.qlangtech.tis.fullbuild.phasestatus.IJoinTaskStatus;
import com.qlangtech.tis.plugin.ds.IDataSourceFactoryGetter;
import com.qlangtech.tis.sql.parser.ISqlTask;
import com.qlangtech.tis.sql.parser.er.IPrimaryTabFinder;

import java.util.function.Supplier;

/**
 * 全局和索引不相关,工作流内构建宽表之用 <br>
 * 执行打宽表任务
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IFlatTableBuilder extends ITaskFactory, ITableBuildTaskContext {
    /**
     * 创建打宽表join节点
     *
     * @param nodeMeta
     * @param isFinalNode      是否是DF的最终节点
     * @param execChainContext
     * @param joinTaskStatus
     * @param dsGetter
     * @param primaryTabFinder
     * @return
     */
    public DataflowTask createTask(ISqlTask nodeMeta
            , boolean isFinalNode
            , IExecChainContext execChainContext
            , ITaskContext tskContext
            , IJoinTaskStatus joinTaskStatus
            , final IDataSourceFactoryGetter dsGetter
            , Supplier<IPrimaryTabFinder> primaryTabFinder);


}
