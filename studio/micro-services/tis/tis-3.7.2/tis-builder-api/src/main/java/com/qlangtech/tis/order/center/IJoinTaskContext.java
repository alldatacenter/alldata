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
package com.qlangtech.tis.order.center;

import com.qlangtech.tis.fullbuild.phasestatus.IPhaseStatusCollection;
import com.qlangtech.tis.job.common.IPipelineExecContext;

import java.util.function.Supplier;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IJoinTaskContext extends IParamContext, IPipelineExecContext {






    public boolean isDryRun();

    /**
     * 目标索引的组数
     *
     * @return
     */
    public int getIndexShardCount();

    public <T> T getAttribute(String key);

    public <T> T getAttribute(String key, Supplier<T> creator);

    public void setAttribute(String key, Object v);

    /**
     * dataX 管道、incr增量管道控制器
     *
     * @return
     */
    public IAppSourcePipelineController getPipelineController();

    /**
     * 取得最近一次成功执行的状态，例如，dataX 执行任务是为了取到本次执行任务的总记录数可以在执行中计算进步百分比
     *
     * @return 可以为空
     */
    default <T extends IPhaseStatusCollection> T loadPhaseStatusFromLatest() {
        throw new UnsupportedOperationException();
    }

}
