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
package com.qlangtech.tis.fullbuild.phasestatus.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import com.qlangtech.tis.fullbuild.phasestatus.IChildProcessStatus;
import com.qlangtech.tis.fullbuild.phasestatus.IChildProcessStatusVisitor;
import com.qlangtech.tis.fullbuild.phasestatus.IProcessDetailStatus;

/**
 * 一个阶段有几个子过程(通常子过程执行中会有几个子过程)需要有一个总出口，说明执行详细
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月26日
 */
public abstract class ProcessDetailStatusImpl<T extends IChildProcessStatus> implements IProcessDetailStatus<T> {

    private final Map<String, T> /* childname */
    childStatus;

    public ProcessDetailStatusImpl(Map<String, T> childStatus) {
        super();
        this.childStatus = childStatus;
    }

    public Collection<T> getDetails() {
        if (childStatus == null || this.childStatus.isEmpty()) {
            return Collections.singleton(createMockStatus());
        }
        return this.childStatus.values();
    }

    protected abstract T createMockStatus();

    @Override
    public int getProcessPercent() {
        // return (int) (result / 100);
        return 0;
    }

    @Override
    public void detailVisit(IChildProcessStatusVisitor visitor) {
        for (Map.Entry<String, T> entry : childStatus.entrySet()) {
            visitor.visit(entry.getValue());
        }
    }
}
