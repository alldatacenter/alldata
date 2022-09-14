
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

import com.webank.wedatasphere.streamis.jobmanager.launcher.job.JobInfo;
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.manager.JobStateManager;
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.state.JobState;
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.state.JobStateFetcher;
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.conf.JobLauncherConfiguration;
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.exception.FlinkJobStateFetchException;
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.exception.StreamisJobLaunchException;
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.state.client.StateFileTree;
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.state.client.LinkisJobStateGetAction;
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.state.client.LinkisJobStateResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.linkis.computation.client.LinkisJobBuilder$;
import org.apache.linkis.httpclient.Client;
import org.apache.linkis.httpclient.dws.DWSHttpClient;
import org.apache.linkis.httpclient.dws.response.DWSResult;
import org.apache.linkis.httpclient.response.Result;
import org.apache.linkis.ujes.client.response.ResultSetListResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;


/**
 * Linkis Job state fetcher
 * 1) Init to build http client
 * 2) Invoke the getState method to fetch form /api/rest_j/v1/filesystem/getDirFileTrees, the new JobState info
 *   (Note: linkis doesn't support to fetch the file tree recursively, so should invoke several times)
 * 3) Destroy to close the http client when the system is closed
 * @param <T>
 */

public abstract class AbstractLinkisJobStateFetcher<T extends JobState> implements JobStateFetcher<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractLinkisJobStateFetcher.class);

    /**
     * Modify time properties name
     */
    private static final String PROPS_MODIFY_TIME = "modifytime";

    /**
     * Size properties name
     */
    private static final String PROPS_SIZE = "size";
    /**
     * Http Client
     */
    Client client;

    private final Class<T> stateClass;

    private final JobStateManager jobStateManager;

    public AbstractLinkisJobStateFetcher(Class<T> stateClass, JobStateManager jobStateManager){
        this.stateClass = stateClass;
        this.jobStateManager = jobStateManager;
    }

    /**
     * Init method
     */
    @Override
    public void init() {
        String fetcherName = this.getClass().getSimpleName();
        LOG.info("Initialize httpClient in JobStateFetcher for [{}] start", fetcherName);
        client = new DWSHttpClient(LinkisJobBuilder$.MODULE$.getDefaultClientConfig(), fetcherName + "-Client");
        LOG.info("Initialize httpClient in JobStateFetcher for [{}] finished", fetcherName);
    }

    /**
     * Main entrance
     * @param jobInfo job info
     * @return
     */
    @Override
    public T getState(JobInfo jobInfo) {
        String treeDir = this.jobStateManager.getJobStateDir(stateClass, jobInfo.getName()).toString();
        StateFileTree stateFileTree =  traverseFileTreeToFind(jobInfo, getDirFileTree(jobInfo, treeDir), this::isMatch, false);
        if (Objects.nonNull(stateFileTree) && StringUtils.isNotBlank(stateFileTree.getPath())){
            JobStateFileInfo stateFileInfo = new JobStateFileInfo(stateFileTree.getName(),
                    stateFileTree.getPath(), stateFileTree.getParentPath(),
                    Long.parseLong(stateFileTree.getProperties().getOrDefault(PROPS_SIZE, "0")),
                    Long.parseLong(stateFileTree.getProperties().getOrDefault(PROPS_MODIFY_TIME, "0")));
            return getState(stateFileInfo);
        }
        return null;
    }


    @Override
    public void destroy() {
        try {
            client.close();
        } catch (IOException e) {
            throw new StreamisJobLaunchException.Runtime(-1,
                    "Fail to destroy httpClient in JobStateFetcher[" + this.getClass().getSimpleName() + "]",e);
        }
    }

    /**
     * Traverse the file tree to find the suitable state file
     * @param jobInfo job info
     * @param stateFileTree state file tree
     * @param matcher matcher
     * @param resolved resolved
     * @return
     */
    private StateFileTree traverseFileTreeToFind(JobInfo jobInfo, StateFileTree stateFileTree, Function<String, Boolean> matcher,
                                                    boolean resolved){
        AtomicReference<StateFileTree> latestFileTree = new AtomicReference<>(new StateFileTree());
        if (Objects.nonNull(stateFileTree)){
            if (!resolved && stateFileTree.getIsLeaf()){
                if (matcher.apply(stateFileTree.getPath()) && compareTime(stateFileTree, latestFileTree.get()) > 0){
                    latestFileTree.set(stateFileTree);
                }
            } else if (!stateFileTree.getIsLeaf()){
                Optional.ofNullable(stateFileTree.getChildren()).ifPresent(children -> children.forEach(childStateFileTree -> {
                    StateFileTree candidateFileTree = childStateFileTree.getIsLeaf() ? childStateFileTree :
                            traverseFileTreeToFind(jobInfo,
                                Objects.nonNull(childStateFileTree.getChildren())? childStateFileTree : getDirFileTree(jobInfo, childStateFileTree.getPath()),
                                matcher,
                                true);
                    if (compareTime(candidateFileTree, latestFileTree.get()) > 0 && matcher.apply(candidateFileTree.getPath())){
                        latestFileTree.set(candidateFileTree);
                    }
                }));
            }
        }
        return latestFileTree.get();
    }

    /**
     * Fetch the File tree form directory
     * @param jobInfo job info
     * @param dirPath directory path
     * @return state file tree
     */
    private StateFileTree getDirFileTree(JobInfo jobInfo, String dirPath){
        try {
            LinkisJobStateGetAction getAction = new LinkisJobStateGetAction(jobInfo.getUser(), dirPath);
            Result result = client.execute(getAction);
            String responseBody = Optional.ofNullable(result.getResponseBody()).orElse("");
            LOG.trace("JobState FileTree => [responseBody: {}]",
                    responseBody.length() > 100? responseBody.substring(0, 100) + "..." : responseBody);
            StateFileTree stateFileTree;
            if (result instanceof ResultSetListResult){
                ResultSetListResult setListResult = (ResultSetListResult)result;
                checkFetchStateResult(setListResult);
                stateFileTree = DWSHttpClient.jacksonJson().convertValue(setListResult.getDirFileTrees(), StateFileTree.class);
            } else if(result instanceof LinkisJobStateResult){
                LinkisJobStateResult stateResult = (LinkisJobStateResult) result;
                checkFetchStateResult(stateResult);
                stateFileTree = stateResult.getStateFileTree();
            }else {
                throw new FlinkJobStateFetchException(-1, "JobState FileTree result is not a unrecognized type: " +
                        "[" + result.getClass().getCanonicalName() + "]",null);
            }
            if(stateFileTree == null){
                LOG.warn("'StateFileTree' for path [{}] is null/empty, just return the null FileTree", dirPath);
                return null;
            }
            LOG.trace(stateFileTree.getChildren() + "");
            return stateFileTree;
        } catch (FlinkJobStateFetchException e) {
            throw new StreamisJobLaunchException.Runtime(e.getErrCode(),e.getMessage(),e);
        } catch (Exception e) {
            throw new StreamisJobLaunchException.Runtime(-1,"Unexpected exception in fetching JobState FileTree",e);
        }
    }

    private void checkFetchStateResult(DWSResult result) throws FlinkJobStateFetchException {
        if(result.getStatus()!= 0) {
            String errMsg = result.getMessage();
            throw new FlinkJobStateFetchException(-1, "Fail to fetch JobState FileTree, message: " + errMsg, null);
        }
    }
    /**
     * Compare timestamp value in file trees
     * @param leftTree left
     * @param rightTree right
     * @return size
     */
    private long compareTime(StateFileTree leftTree, StateFileTree rightTree){
        long leftTime = 0L,rightTime = 0L;
        try {
            leftTime = Long.parseLong(Optional.ofNullable(leftTree.getProperties()).orElse(new HashMap<>()).getOrDefault(PROPS_MODIFY_TIME, "0"));
        } catch (NumberFormatException e){
            LOG.warn("Illegal format value for property '{}' in FilePath [{}]", PROPS_MODIFY_TIME, leftTree.getPath(), e);
        }
        try {
            rightTime = Long.parseLong(Optional.ofNullable(rightTree.getProperties()).orElse(new HashMap<>()).getOrDefault(PROPS_MODIFY_TIME, "0"));
        } catch (NumberFormatException e){
            LOG.warn("Illegal format value for property '{}' in FilePath [{}]", PROPS_MODIFY_TIME, rightTree.getPath(), e);
        }
        return leftTime - rightTime;
    }
    /**
     * Is the path is match
     * @param path path
     * @return boolean
     */
    protected abstract boolean isMatch(String path);

    /**
     * Get the concrete JobState entity from FileInfo
     * @param fileInfo file info
     * @return JobState
     */
    protected abstract T getState(JobStateFileInfo fileInfo);

}
