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
package com.qlangtech.tis.fullbuild.taskflow;

import com.qlangtech.tis.assemble.FullbuildPhase;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-04-03 15:32
 */
public abstract class DataflowTask {

    protected final String id;


    protected DataflowTask(String id) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("parm id can not be blank");
        }
        this.id = id;
    }

    public abstract FullbuildPhase phase();

    public abstract String getIdentityName();

    public abstract void run() throws Exception;

    protected final void signTaskSuccess() {
        this.getTaskWorkStatus().put(this.id, true);
    }

    protected final void signTaskFaild() {
        this.getTaskWorkStatus().put(this.id, false);
    }

    // 每个节点的执行状态
    protected abstract Map<String, Boolean> getTaskWorkStatus();

    @Override
    public String toString() {
        return this.id;
    }


}
