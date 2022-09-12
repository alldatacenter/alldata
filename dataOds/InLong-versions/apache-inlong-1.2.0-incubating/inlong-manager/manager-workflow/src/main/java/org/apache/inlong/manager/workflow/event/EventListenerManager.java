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

import java.util.List;

/**
 * Event Listener Manager
 */
public interface EventListenerManager<E extends WorkflowEvent, T extends EventListener<E>> {

    /**
     * Register the listener
     */
    void register(T listener);

    /**
     * Get all asynchronous listeners according to event type
     *
     * @param event event
     * @return asynchronous listeners
     */
    List<T> asyncListeners(E event);

    /**
     * Get all the synchronous listeners according to the event type
     *
     * @param event event
     * @return Sync listeners
     */
    List<T> syncListeners(E event);

    /**
     * Obtain the listener according to the listener name
     *
     * @param listenerName The name of the listener
     * @return Listener
     */
    T listener(String listenerName);

}
