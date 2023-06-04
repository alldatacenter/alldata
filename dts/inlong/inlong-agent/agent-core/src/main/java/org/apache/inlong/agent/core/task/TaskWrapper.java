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

package org.apache.inlong.agent.core.task;

import org.apache.inlong.agent.common.AgentThreadFactory;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.constant.AgentConstants;
import org.apache.inlong.agent.constant.JobConstants;
import org.apache.inlong.agent.message.EndMessage;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.state.AbstractStateWrapper;
import org.apache.inlong.agent.state.State;
import org.apache.inlong.agent.utils.AgentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TaskWrapper is used in taskManager, it maintains the life cycle of
 * running task.
 */
public class TaskWrapper extends AbstractStateWrapper {

    public static final int WAIT_FINISH_TIME_OUT = 1;
    public static final int WAIT_BEGIN_TIME_SECONDS = 60;
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskWrapper.class);
    private final TaskManager taskManager;
    private final Task task;

    private final AtomicInteger retryTime = new AtomicInteger(0);
    private final int maxRetryTime;
    private final int pushMaxWaitTime;
    private final int pullMaxWaitTime;
    private ExecutorService executorService;

    public TaskWrapper(TaskManager manager, Task task) {
        super();
        this.taskManager = manager;
        this.task = task;
        AgentConfiguration conf = AgentConfiguration.getAgentConf();
        maxRetryTime = conf.getInt(
                AgentConstants.TASK_MAX_RETRY_TIME, AgentConstants.DEFAULT_TASK_MAX_RETRY_TIME);
        pushMaxWaitTime = conf.getInt(
                AgentConstants.TASK_PUSH_MAX_SECOND, AgentConstants.DEFAULT_TASK_PUSH_MAX_SECOND);
        pullMaxWaitTime = conf.getInt(
                AgentConstants.TASK_PULL_MAX_SECOND, AgentConstants.DEFAULT_TASK_PULL_MAX_SECOND);
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                    60L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(),
                    new AgentThreadFactory("task-reader-writer-task_" + task.getTaskId()));
        }
        doChangeState(State.ACCEPTED);
    }

    /**
     * submit read thread
     *
     * @return CompletableFuture
     */
    private CompletableFuture<?> submitReadThread() {
        return CompletableFuture.runAsync(() -> {
            Message message = null;
            while (!isException() && !task.isReadFinished()) {
                // if source deleted,then failed
                if (!task.getReader().isSourceExist()) {
                    doChangeState(State.FAILED);
                } else {
                    if (message == null
                            || task.getChannel().push(message, pushMaxWaitTime, TimeUnit.SECONDS)) {
                        message = task.getReader().read();
                    }
                }
            }
            LOGGER.info("read end, task exception status is {}, read finish status is {}", isException(),
                    task.isReadFinished());
            // write end message
            task.getChannel().push(new EndMessage());
            task.getReader().destroy();
        }, executorService);
    }

    /**
     * submit write thread
     *
     * @return CompletableFuture
     */
    private CompletableFuture<?> submitWriteThread() {
        return CompletableFuture.runAsync(() -> {
            while (!isException()) {
                Message message = task.getChannel().pull(pullMaxWaitTime, TimeUnit.SECONDS);
                if (message instanceof EndMessage) {
                    break;
                }
                task.getSink().write(message);
            }
        }, executorService);
    }

    /**
     * submit reader/writer
     */
    private void submitThreadsAndWait() {
        CompletableFuture<?> reader = submitReadThread();
        CompletableFuture<?> writer = submitWriteThread();
        CompletableFuture.allOf(reader, writer)
                .exceptionally(ex -> {
                    doChangeState(State.FAILED);
                    LOGGER.error("exception caught", ex);
                    return null;
                }).join();
    }

    /**
     * kill task
     */
    void kill() {
        LOGGER.info("task id {} is killed", task.getTaskId());
        doChangeState(State.KILLED);
    }

    /**
     * In standalone mode, the job to be removed should wait until the read is finished, set
     * timeout to WAIT_FINISH_TIME_OUT minute to wait for finishing
     */
    void waitForFinish() {
        LOGGER.info("set readTime out to 1 minute task id is {}", task.getTaskId());
        task.getReader().setReadTimeout(TimeUnit.MINUTES.toMillis(WAIT_FINISH_TIME_OUT));
    }

    /**
     * destroy task
     */
    void destroyTask() {
        LOGGER.info("destroy task id is {}", task.getTaskId());
        task.getReader().finishRead();
    }

    /**
     * whether task retry times exceed max retry time.
     *
     * @return whether should retry
     */
    boolean shouldRetry() {
        return retryTime.get() < maxRetryTime;
    }

    Task getTask() {
        return task;
    }

    @Override
    public void addCallbacks() {
        this.addCallback(State.ACCEPTED, State.RUNNING, (before, after) -> {

        }).addCallback(State.RUNNING, State.FAILED, (before, after) -> {
            LOGGER.info("task {} is failed, please check it", task.getTaskId());
            retryTime.incrementAndGet();
            if (!shouldRetry()) {
                doChangeState(State.FATAL);
                taskManager.getTaskMetrics().taskFatalCount.incrementAndGet();
            }
        }).addCallback(State.FAILED, State.FATAL, (before, after) -> {

        }).addCallback(State.FAILED, State.ACCEPTED, (before, after) -> {

        }).addCallback(State.FAILED, State.RUNNING, ((before, after) -> {

        })).addCallback(State.RUNNING, State.SUCCEEDED, (before, after) -> {

        });
    }

    @Override
    public void run() {
        try {
            AgentThreadFactory.nameThread(task.getTaskId());
            LOGGER.info("start to run {}, retry time is {}", task.getTaskId(), retryTime.get());
            AgentUtils.silenceSleepInSeconds(task.getJobConf()
                    .getLong(JobConstants.JOB_TASK_BEGIN_WAIT_SECONDS, WAIT_BEGIN_TIME_SECONDS));
            doChangeState(State.RUNNING);
            task.init();
            submitThreadsAndWait();
            if (!isException()) {
                doChangeState(State.SUCCEEDED);
            }
            LOGGER.info("task state is {}, start to destroy task {}", getCurrentState(), task.getTaskId());
            task.destroy();
        } catch (Exception ex) {
            LOGGER.error("error while running wrapper", ex);
            doChangeState(State.FAILED);
        }
    }
}
