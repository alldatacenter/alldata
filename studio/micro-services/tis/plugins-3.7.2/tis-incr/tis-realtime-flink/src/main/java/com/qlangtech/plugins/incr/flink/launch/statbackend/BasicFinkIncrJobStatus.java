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

package com.qlangtech.plugins.incr.flink.launch.statbackend;

import com.qlangtech.tis.coredefine.module.action.IFlinkIncrJobStatus;
import org.apache.commons.io.FileUtils;
import org.apache.flink.api.common.JobID;

import java.io.File;
import java.io.IOException;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-05-18 12:57
 **/
public abstract class BasicFinkIncrJobStatus implements IFlinkIncrJobStatus<JobID> {
    protected JobID jobID;
    protected final File incrJobFile;
    protected State state;

    public BasicFinkIncrJobStatus(File incrJobFile) {
        this.incrJobFile = incrJobFile;
    }

    public void cancel() {
        try {
            FileUtils.forceDelete(incrJobFile);
            this.state = State.NONE;
            this.jobID = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
