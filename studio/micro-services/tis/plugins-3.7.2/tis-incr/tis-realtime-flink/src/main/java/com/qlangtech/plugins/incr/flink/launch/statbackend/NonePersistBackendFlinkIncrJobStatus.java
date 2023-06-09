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
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.manage.common.TisUTF8;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.flink.api.common.JobID;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-05-18 11:58
 **/
public class NonePersistBackendFlinkIncrJobStatus extends BasicFinkIncrJobStatus {


    public static IFlinkIncrJobStatus getIncrStatus(TargetResName collection) {
        IDataxProcessor processor = DataxProcessor.load(null, collection.getName());
        File dataXWorkDir = processor.getDataXWorkDir(null);

        return new NonePersistBackendFlinkIncrJobStatus(new File(dataXWorkDir, "incr_job_none_persist_backend.log"));
    }

    private NonePersistBackendFlinkIncrJobStatus(File incrJobFile) {
        super(incrJobFile);
        if (!incrJobFile.exists()) {
            state = State.NONE;
            return;
        }

        try (LineIterator lines = FileUtils.lineIterator(incrJobFile, TisUTF8.getName())) {
            if (lines.hasNext()) {
                jobID = JobID.fromHexString(lines.nextLine());
                state = State.RUNNING;
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        state = State.NONE;
    }


    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public JobID createNewJob(JobID jobID) {
        try {
            FileUtils.write(this.incrJobFile, jobID.toHexString(), TisUTF8.get());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.state = State.RUNNING;
        return this.jobID = jobID;
    }

    @Override
    public JobID getLaunchJobID() {
        return this.jobID;
    }

    @Override
    public void relaunch(JobID jobID) {

    }

    @Override
    public void addSavePoint(String savepointDirectory, State state) {

    }

    @Override
    public void discardSavepoint(String savepointDirectory) {

    }

    @Override
    public void stop(String savepointDirectory) {

    }

    @Override
    public Optional<FlinkSavepoint> containSavepoint(String path) {
        return Optional.empty();
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public List<FlinkSavepoint> getSavepointPaths() {
        return Collections.emptyList();
    }
}
