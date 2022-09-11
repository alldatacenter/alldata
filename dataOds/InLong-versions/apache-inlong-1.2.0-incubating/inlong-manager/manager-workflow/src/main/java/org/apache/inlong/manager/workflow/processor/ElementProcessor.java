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

package org.apache.inlong.manager.workflow.processor;

import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.definition.Element;

import java.util.List;

/**
 * Workflow component-processor
 */
public interface ElementProcessor<T extends Element> {

    /**
     * Which type to handle
     */
    Class<T> watch();

    /**
     * Execute the logic of the current component
     */
    void create(T element, WorkflowContext context);

    /**
     * Whether to wait for user action
     */
    boolean pendingForAction(WorkflowContext context);

    /**
     * Finish the current
     *
     * @return Whether the execution is complete
     */
    boolean complete(WorkflowContext context);

    /**
     * Get the next component to be executed
     *
     * @return The next component to be executed
     */
    List<Element> next(T element, WorkflowContext context);

}
