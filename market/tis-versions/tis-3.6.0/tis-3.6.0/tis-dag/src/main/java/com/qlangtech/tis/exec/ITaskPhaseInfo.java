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
package com.qlangtech.tis.exec;

import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.fullbuild.phasestatus.impl.BasicPhaseStatus;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月23日
 */
public interface ITaskPhaseInfo {

    /**
     * 取得某个阶段的状态
     *
     * @param phase
     * @return
     */
    // public <T extends BasicPhaseStatus> T getPhaseStatus(IExecChainContext
    // execContext);
    public <T extends BasicPhaseStatus<?>> T getPhaseStatus(IExecChainContext execContext, FullbuildPhase phase);
}
