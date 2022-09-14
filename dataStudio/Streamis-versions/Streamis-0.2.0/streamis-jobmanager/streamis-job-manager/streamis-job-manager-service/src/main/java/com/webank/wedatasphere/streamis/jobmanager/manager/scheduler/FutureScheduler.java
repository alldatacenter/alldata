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
package com.webank.wedatasphere.streamis.jobmanager.manager.scheduler;

import com.webank.wedatasphere.streamis.jobmanager.manager.scheduler.exception.StreamisScheduleException;
import org.apache.linkis.scheduler.queue.SchedulerEvent;

import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Include the method submit(SchedulerEvent, Function): Future
 */
public interface FutureScheduler {

    /**
     * Submit the scheduler event and return the Future value
     * @param event scheduler event
     * @param resultMapping result mapping
     * @return future task
     */
    <T>Future<T> submit(SchedulerEvent event, Function<SchedulerEvent, T> resultMapping) throws StreamisScheduleException;

}
