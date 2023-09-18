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

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年8月20日
 */
public class ExecutePhaseRange {

    private final FullbuildPhase start;

    private final FullbuildPhase end;

    public static final ExecutePhaseRange fullRange() {
        return new ExecutePhaseRange(FullbuildPhase.FullDump, FullbuildPhase.IndexBackFlow);
    }

    public ExecutePhaseRange(FullbuildPhase start, FullbuildPhase end) {
        super();
        this.start = start;
        this.end = end;
    }

    /**
     * 阶段区间中是否包含phase？
     *
     * @param phase
     * @return
     */
    public boolean contains(FullbuildPhase phase) {
        return phase.getValue() >= this.start.getValue() && phase.getValue() <= this.end.getValue();
    }

    public FullbuildPhase getStart() {
        return this.start;
    }

    public FullbuildPhase getEnd() {
        return this.end;
    }
}
