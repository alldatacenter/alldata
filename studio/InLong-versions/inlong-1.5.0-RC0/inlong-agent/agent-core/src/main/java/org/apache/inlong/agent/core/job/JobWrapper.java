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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.inlong.agent.common.AgentThreadFactory;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.AgentConstants;
import org.apache.inlong.agent.constant.JobConstants;
import org.apache.inlong.agent.core.AgentManager;
import org.apache.inlong.agent.core.task.Task;
import org.apache.inlong.agent.core.task.TaskManager;
import org.apache.inlong.agent.db.CommandDb;
import org.apache.inlong.agent.plugin.Channel;
import org.apache.inlong.agent.plugin.Reader;
import org.apache.inlong.agent.plugin.Sink;
import org.apache.inlong.agent.plugin.Source;
import org.apache.inlong.agent.state.AbstractStateWrapper;
import org.apache.inlong.agent.state.State;
import org.apache.inlong.agent.utils.ThreadUtils;
import org.apache.inlong.common.constant.Constants;
import org.apache.inlong.common.db.CommandEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final Queue<Task> taskQueue;
    private boolean isEnd; // Solve the concurrency problem submitted by jobProfile
    private final AtomicInteger index;
    private CommandDb db;

    public JobWrapper(AgentManager manager, Job job) {
        super();
        this.agentConf = AgentConfiguration.getAgentConf();
        this.taskManager = manager.getTaskManager();
        this.jobManager = manager.getJobManager();
        this.job = job;
        this.allTasks = new ArrayList<>();
        this.db = manager.getCommandDb();
        this.taskQueue = new LinkedList<>();
        this.index = new AtomicInteger(0);
        this.isEnd = false;

        doChangeState(State.ACCEPTED);
    }

    private void checkQueuedTasks() {
        while (taskQueue.peek() != null && !isEnd) {
            Task task = taskQueue.poll();
            allTasks.add(task);
            taskManager.submitTask(task);
        }
    }

    /**
     * check states of all tasks, wait if one of them not finished.
     */
    private void checkAllTasksStateAndWait() throws Exception {
        boolean isFinished = false;

        long checkInterval = agentConf.getLong(
                AgentConstants.JOB_FINISH_CHECK_INTERVAL, AgentConstants.DEFAULT_JOB_FINISH_CHECK_INTERVAL);
        do {
            checkQueuedTasks();
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
     * Check whether something in running jobs
     *
     * @param predicate a function to test whether running jobs met some conditions
     * @return
     */
    public boolean exist(Predicate<Collection<Task>> predicate) {
        Collection<Task> tasks = new HashSet<>();
        tasks.addAll(allTasks);
        tasks.addAll(taskQueue);
        return predicate.test(tasks);
    }

    /**
     * Submit jobProfile or add taskProfile as it's subtask
     */
    public void submit(JobProfile taskProfile) {
        try {
            JobProfile jobProfile = job.getJobConf();
            Preconditions.checkArgument(jobProfile.get(JobConstants.JOB_SOURCE_CLASS)
                    .equals(taskProfile.get(JobConstants.JOB_SOURCE_CLASS)), "Subtask source should be same");
            Preconditions.checkArgument(jobProfile.get(JobConstants.JOB_CHANNEL)
                    .equals(taskProfile.get(JobConstants.JOB_CHANNEL)), "Subtask channel should be same");
            Preconditions.checkArgument(jobProfile.get(JobConstants.JOB_SINK)
                    .equals(taskProfile.get(JobConstants.JOB_SINK, "Subtask sink should be same")));
            LOGGER.info("job id: {} submit new task {}.", job.getJobInstanceId(), taskProfile.toJsonStr());
            Source source = (Source) Class.forName(jobProfile.get(JobConstants.JOB_SOURCE_CLASS)).newInstance();
            for (Reader reader : source.split(taskProfile)) {
                Sink writer = (Sink) Class.forName(jobProfile.get(JobConstants.JOB_SINK)).newInstance();
                writer.setSourceName(reader.getReadSource());
                Channel channel = (Channel) Class.forName(jobProfile.get(JobConstants.JOB_CHANNEL)).newInstance();
                String taskId = String.format("%s_%d", job.getJobInstanceId(), index.incrementAndGet());
                jobProfile.set(reader.getReadSource(), DigestUtils.md5Hex(reader.getReadSource()));
                Task task = new Task(taskId, reader, writer, channel, taskProfile);
                taskQueue.offer(task);
            }
        } catch (Throwable ex) {
            LOGGER.error("Create task for profile({}) failed", taskProfile, ex);
            ThreadUtils.threadThrowableHandler(Thread.currentThread(), ex);
        }
        LOGGER.info("Job name is {} and task size {}", job.getName(), allTasks.size());
    }

    /**
     * cleanup job
     */
    public void cleanup() {
        isEnd = true;
        allTasks.forEach(task -> taskManager.removeTask(task.getTaskId()));
    }

    @Override
    public void run() {
        try {
            AgentThreadFactory.nameThread(job.getJobInstanceId());
            LOGGER.info("job id: {}, source: {}, channel: {}, sink: {}",
                    job.getJobInstanceId(), job.getJobConf().get(JobConstants.JOB_SOURCE_CLASS),
                    job.getJobConf().get(JobConstants.JOB_CHANNEL), job.getJobConf().get(JobConstants.JOB_SINK));
            doChangeState(State.RUNNING);
            submit(job.getJobConf());
            checkQueuedTasks();
            checkAllTasksStateAndWait();
            cleanup();
            LOGGER.info("job name is {}, state is {}", job.getName(), getCurrentState());
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

    /**
     * get all running task
     */
    public List<Task> getAllTasks() {
        return allTasks;
    }
}
