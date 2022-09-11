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

package org.apache.inlong.agent.core.job;

import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.constant.AgentConstants;
import org.apache.inlong.agent.core.AgentManager;
import org.apache.inlong.agent.core.task.Task;
import org.apache.inlong.agent.core.task.TaskManager;
import org.apache.inlong.agent.db.CommandDb;
import org.apache.inlong.agent.state.AbstractStateWrapper;
import org.apache.inlong.agent.state.State;
import org.apache.inlong.common.constant.Constants;
import org.apache.inlong.common.db.CommandEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_JOB_VERSION;
import static org.apache.inlong.agent.constant.AgentConstants.JOB_VERSION;
import static org.apache.inlong.agent.constant.JobConstants.JOB_OFFSET_DELIMITER;

/**
 * JobWrapper is used in JobManager, it defines the life cycle of
 * running job and maintains the state of job.
 */
public class JobWrapper extends AbstractStateWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobWrapper.class);

    private final AgentConfiguration agentConf;
    private final TaskManager taskManager;
    private final JobManager jobManager;
    private final Job job;
    private final List<Task> allTasks;
    private CommandDb db;

    public JobWrapper(AgentManager manager, Job job) {
        super();
        this.agentConf = AgentConfiguration.getAgentConf();
        this.taskManager = manager.getTaskManager();
        this.jobManager = manager.getJobManager();
        this.job = job;
        this.allTasks = new ArrayList<>();
        this.db = manager.getCommandDb();
        doChangeState(State.ACCEPTED);
    }

    /**
     * check states of all tasks, wait if one of them not finished.
     */
    private void checkAllTasksStateAndWait() throws Exception {
        boolean isFinished = false;

        long checkInterval = agentConf.getLong(
                AgentConstants.JOB_FINISH_CHECK_INTERVAL, AgentConstants.DEFAULT_JOB_FINISH_CHECK_INTERVAL);
        do {
            // check whether all tasks have finished.
            isFinished = allTasks.stream().allMatch(task -> taskManager.isTaskFinished(task.getTaskId()));
            TimeUnit.SECONDS.sleep(checkInterval);
        } while (!isFinished);
        LOGGER.info("all tasks of {} has been checked", job.getJobInstanceId());
        boolean isSuccess = allTasks.stream().allMatch(task -> taskManager.isTaskSuccess(task.getTaskId()));
        if (isSuccess) {
            doChangeState(State.SUCCEEDED);
        } else {
            doChangeState(State.FAILED);
            saveFailedCommand();
        }
    }

    private void saveFailedCommand() {
        CommandEntity entity = new CommandEntity();
        entity.setId(job.getJobInstanceId());
        entity.setAcked(false);
        entity.setTaskId(Integer.valueOf(job.getJobInstanceId()));
        entity.setCommandResult(Constants.RESULT_FAIL);
        entity.setVersion(job.getJobConf().getInt(JOB_VERSION, DEFAULT_JOB_VERSION));
        db.storeCommand(entity);
    }

    /**
     * submit all tasks
     */
    private void submitAllTasks() {
        List<Task> tasks = job.createTasks();
        tasks.forEach(task -> {
            allTasks.add(task);
            taskManager.submitTask(task);
        });
    }

    /**
     * get snapshot of each task
     *
     * @return snapshot string
     */
    public String getSnapshot() {
        List<String> snapshotList = new ArrayList<>();
        for (Task task : allTasks) {
            String snapshot = task.getReader().getSnapshot();
            if (snapshot != null) {
                snapshotList.add(snapshot);
            }
        }
        return String.join(JOB_OFFSET_DELIMITER, snapshotList);
    }

    /**
     * get job
     *
     * @return job
     */
    public Job getJob() {
        return job;
    }

    /**
     * cleanup job
     */
    public void cleanup() {
        allTasks.forEach(task -> taskManager.removeTask(task.getTaskId()));
    }

    @Override
    public void run() {
        try {
            doChangeState(State.RUNNING);
            submitAllTasks();
            checkAllTasksStateAndWait();
            cleanup();
        } catch (Exception ex) {
            doChangeState(State.FAILED);
            LOGGER.error("error caught: {}, message: {}",
                    job.getJobConf().toJsonStr(), ex);
        }
    }

    @Override
    public void addCallbacks() {
        this.addCallback(State.ACCEPTED, State.RUNNING, (before, after) -> {

        }).addCallback(State.RUNNING, State.FAILED, (before, after) -> {
            jobManager.markJobAsFailed(job.getJobInstanceId());
        }).addCallback(State.RUNNING, State.SUCCEEDED, ((before, after) -> {
            jobManager.markJobAsSuccess(job.getJobInstanceId());
        }));
    }

    public List<Task> getAllTasks() {
        return allTasks;
    }
}
