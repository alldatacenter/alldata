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
package com.qlangtech.tis.plugin.incr;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.coredefine.module.action.IFlinkIncrJobStatus;
import com.qlangtech.tis.coredefine.module.action.IRCController;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
@Public
public abstract class IncrStreamFactory implements Describable<IncrStreamFactory> {

    public abstract IRCController getIncrSync();
    /**
     * 缺的当前执行任务的状态
     *
     * @return
     */
    public abstract IFlinkIncrJobStatus getIncrJobStatus(TargetResName collection);

    public abstract <StreamExecutionEnvironment> StreamExecutionEnvironment createStreamExecutionEnvironment();


    @Override
    public final Descriptor<IncrStreamFactory> getDescriptor() {
        return TIS.get().getDescriptor(this.getClass());
    }
}
