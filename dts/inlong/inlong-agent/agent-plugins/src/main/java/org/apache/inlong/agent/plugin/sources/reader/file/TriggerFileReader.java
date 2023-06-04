/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.plugin.sources.reader.file;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.JobConstants;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.Reader;

import javax.validation.constraints.NotNull;

public class TriggerFileReader implements Reader {

    @NotNull
    private String triggerId;

    @Override
    public Message read() {
        try {
            // just mock trigger is running
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            // do nothing
        }
        return null;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public String getReadSource() {
        return "Mock file reader trigger " + triggerId;
    }

    @Override
    public void setReadTimeout(long mill) {

    }

    @Override
    public void setWaitMillisecond(long millis) {

    }

    @Override
    public String getSnapshot() {
        return StringUtils.EMPTY;
    }

    @Override
    public void finishRead() {

    }

    @Override
    public boolean isSourceExist() {
        return true;
    }

    @Override
    public void init(JobProfile jobConf) {
        this.triggerId = jobConf.get(JobConstants.JOB_TRIGGER);
    }

    @Override
    public void destroy() {

    }
}
