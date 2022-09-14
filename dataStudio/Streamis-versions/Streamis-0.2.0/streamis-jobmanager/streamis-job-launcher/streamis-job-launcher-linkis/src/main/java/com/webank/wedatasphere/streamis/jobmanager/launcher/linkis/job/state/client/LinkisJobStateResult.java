/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.state.client;

import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.exception.FlinkJobStateFetchException;
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.state.AbstractJobStateResult;
import org.apache.linkis.httpclient.dws.DWSHttpClient;
import org.apache.linkis.httpclient.dws.annotation.DWSHttpMessageResult;

import java.util.HashMap;
import java.util.Map;

/**
 * JobState result
 */
@DWSHttpMessageResult("/api/rest_j/v\\d+/filesystem/getDirFileTrees")
public class LinkisJobStateResult extends AbstractJobStateResult {

    private Map<String, Object> dirFileTrees = new HashMap<>();

    /**
     * Convert the result data to state file tree
     * @return state file tree
     */
    public StateFileTree getStateFileTree() throws FlinkJobStateFetchException {
        try {
            return DWSHttpClient.jacksonJson().convertValue(dirFileTrees, StateFileTree.class);
        }catch(Exception e){
            throw new FlinkJobStateFetchException(-1, "Fail to parse JobState result data, message: " + e.getMessage(), e);
        }
    }

}
