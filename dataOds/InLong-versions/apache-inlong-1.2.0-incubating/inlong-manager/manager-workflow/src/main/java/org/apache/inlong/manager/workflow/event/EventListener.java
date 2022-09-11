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

package org.apache.inlong.manager.workflow.event;

import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.workflow.WorkflowContext;

/**
 * WorkflowProcess event listener
 */
public interface EventListener<EventType extends WorkflowEvent> {

    /**
     * The name of the listener, the default is the full name of the class
     *
     * @return The name of the listener
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * The type of the listener
     *
     * @return type
     */
    EventType event();

    /**
     * Monitor event processing
     *
     * @param context WorkflowProcess context
     * @return ListenerResult Listener execution result
     * @throws WorkflowListenerException Listener execution exception
     */
    ListenerResult listen(WorkflowContext context) throws Exception;

    /**
     * Whether to execute asynchronously
     *
     * @return yes/no
     * @apiNote If you need to perceive the execution of each sub-process in time,
     *         it should be synchronized, that is, set to false; otherwise, set to true
     */
    boolean async();

    /**
     * Whether to ignore the execution log, if ignored, it will not be recorded in the log table
     *
     * @return yes/no
     */
    default boolean ignoreRecordLog() {
        return false;
    }

}
