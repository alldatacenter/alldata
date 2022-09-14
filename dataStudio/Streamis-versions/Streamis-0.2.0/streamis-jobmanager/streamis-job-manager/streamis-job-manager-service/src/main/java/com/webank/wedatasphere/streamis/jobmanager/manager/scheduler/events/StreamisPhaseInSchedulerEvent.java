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


package com.webank.wedatasphere.streamis.jobmanager.manager.scheduler.events;

import com.webank.wedatasphere.streamis.jobmanager.manager.scheduler.exception.StreamisScheduleException;
import org.apache.linkis.scheduler.queue.JobInfo;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Streamis scheduler event with different phases
 */
public class StreamisPhaseInSchedulerEvent extends AbstractStreamisSchedulerEvent{


    private final StateContext context;

    private final ScheduleCommand scheduleCommand;

    public StreamisPhaseInSchedulerEvent(String scheduleId, ScheduleCommand command){
        this.scheduleId = scheduleId;
        this.scheduleCommand = command;
        if (Objects.isNull(this.scheduleCommand)){
            throw new IllegalArgumentException("SchedulerCommand<T> cannot be empty, please define it first");
        }
        // New context
        this.context = new StateContext();
    }

    @Override
    public void schedule(JobInfo jobInfo) throws StreamisScheduleException {
        Map<String, Object> resultSet = this.scheduleCommand.schedule(this.context, jobInfo);
        Optional.ofNullable(resultSet).ifPresent( result -> super.resultSet.putAll(result));
    }

    @Override
    public void errorHandle(JobInfo jobInfo, Throwable t) {
        this.scheduleCommand.onErrorHandle(this.context, jobInfo, t);
    }

    @Override
    public void postHandle(JobInfo jobInfo) throws StreamisScheduleException {
        this.scheduleCommand.onPostHandle(this.context, jobInfo);
    }

    @Override
    protected void prepareHandle(JobInfo jobInfo) throws StreamisScheduleException {
        this.scheduleCommand.onPrepare(this.context, jobInfo);
    }


    /**
     * Schedule command
     */
    public interface ScheduleCommand {

        default void onPrepare(StateContext context, JobInfo scheduleJob) throws StreamisScheduleException{
            // No operation
        }

        /**
         * Schedule and return the resultSet
         * @param context context
         * @param jobInfo job info
         * @return resultSet
         * @throws StreamisScheduleException
         */
        Map<String, Object> schedule(StateContext context, JobInfo jobInfo) throws StreamisScheduleException;

        default void onErrorHandle(StateContext context, JobInfo scheduleJob, Throwable t){
            // No operation
        }

        default void onPostHandle(StateContext context, JobInfo scheduleJob) throws StreamisScheduleException{
            // No operation
        }

    }

    /**
     * Context contains variables
     */
    public static class StateContext{
        private final Map<String, Object> variables = new ConcurrentHashMap<>();

        public void addVar(String name, Object value){
            variables.put(name, value);
        }

        public Object getVar(String name){
            return variables.get(name);
        }

        @SuppressWarnings("unchecked")
        public <T>T getVar(String name, Class<T> type){
            Object var = variables.get(name);
            return Objects.nonNull(var)? (T)var : null;
        }
    }
}
