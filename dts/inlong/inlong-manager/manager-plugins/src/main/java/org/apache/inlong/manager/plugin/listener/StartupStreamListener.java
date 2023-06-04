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

package org.apache.inlong.manager.plugin.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.SortOperateListener;

/**
 * Listener for startup the Sort task for InlongStream
 */
@Slf4j
public class StartupStreamListener implements SortOperateListener {

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    /**
     * Currently, the process of starting Sort tasks has been initiated in {@link StartupSortListener}.
     * <p/>Because the Sort task is only associated with InlongGroup, no need to start it for InlongStream.
     */
    @Override
    public boolean accept(WorkflowContext workflowContext) {
        log.info("not need to start the sort task for InlongStream");
        return false;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws Exception {
        return ListenerResult.success();
    }

}
