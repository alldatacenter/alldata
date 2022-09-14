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
package com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.state;

import com.webank.wedatasphere.streamis.jobmanager.launcher.job.manager.JobStateManager;
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.conf.JobLauncherConfiguration;
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.exception.StreamisJobLaunchException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import static com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.state.JobStateConf.SAVEPOINT_PATH_PATTERN;

/**
 * Savepoint JobState Fetcher
 */
public class SavepointJobStateFetcher extends AbstractLinkisJobStateFetcher<Savepoint>{

    private static final Logger LOG = LoggerFactory.getLogger(CheckpointJobStateFetcher.class);

    private static final Pattern PATH_PATTERN = Pattern.compile(SAVEPOINT_PATH_PATTERN.getValue());

    public SavepointJobStateFetcher(Class<Savepoint> stateClass, JobStateManager jobStateManager) {
        super(stateClass, jobStateManager);
    }

    @Override
    protected boolean isMatch(String path) {
        return PATH_PATTERN.matcher(path).matches();
    }

    @Override
    protected Savepoint getState(JobStateFileInfo fileInfo) {
        // TODO from linkis will lost the authority info
        URI location = URI.create(fileInfo.getPath());
        if (StringUtils.isBlank(location.getAuthority()) &&
                StringUtils.isNotBlank(JobLauncherConfiguration.FLINK_STATE_DEFAULT_AUTHORITY().getValue())){
            try {
                location = new URI(location.getScheme(), JobLauncherConfiguration.FLINK_STATE_DEFAULT_AUTHORITY().getValue(),
                        location.getPath(), null, null);
            } catch (URISyntaxException e) {
                throw new StreamisJobLaunchException.Runtime(-1, "Fail to resolve checkpoint location, message: " + e.getMessage(), e);
            }
        }
        Savepoint savepoint = new Savepoint(location.toString());
        savepoint.setMetadataInfo(fileInfo);
        savepoint.setTimestamp(fileInfo.getModifytime());
        LOG.info("Savepoint info is [path: {}, timestamp: {}]", savepoint.getLocation(), savepoint.getTimestamp());
        return savepoint;
    }
}
