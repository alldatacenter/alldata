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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.state.JobStateConf.CHECKPOINT_PATH_PATTERN;

/**
 * Checkpoint JobState Fetcher
 */
public class CheckpointJobStateFetcher extends AbstractLinkisJobStateFetcher<Checkpoint> {

    private static final Logger LOG = LoggerFactory.getLogger(CheckpointJobStateFetcher.class);

    private static final Pattern PATH_PATTERN = Pattern.compile(CHECKPOINT_PATH_PATTERN.getValue());

    public CheckpointJobStateFetcher(Class<Checkpoint> stateClass, JobStateManager jobStateManager) {
        super(stateClass, jobStateManager);
    }

    @Override
    protected boolean isMatch(String path) {
        return PATH_PATTERN.matcher(path).matches();
    }

    @Override
    public Checkpoint getState(JobStateFileInfo fileInfo) {
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
        Checkpoint checkpoint = new Checkpoint(location.toString());
        checkpoint.setMetadataInfo(fileInfo);
        checkpoint.setTimestamp(fileInfo.getModifytime());
        LOG.info("Checkpoint info is [path: {}, timestamp: {}]" ,checkpoint.getLocation(), checkpoint.getTimestamp());
        return checkpoint;
    }

}
