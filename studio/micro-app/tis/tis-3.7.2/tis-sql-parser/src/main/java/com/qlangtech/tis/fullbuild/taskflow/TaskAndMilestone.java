package com.qlangtech.tis.fullbuild.taskflow;

import com.qlangtech.tis.assemble.FullbuildPhase;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hudson.reactor.MilestoneImpl;

import java.util.Map;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-02-14 21:30
 **/
public class TaskAndMilestone {

    public static final String MILESTONE_PREFIX = "milestone_";
    public final DataflowTask task;

    public final MilestoneImpl milestone;

    public static TaskAndMilestone createMilestone(String milestoneId) {
        return new TaskAndMilestone(new DataflowTask(milestoneId) {
            @Override
            public FullbuildPhase phase() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getIdentityName() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void run() throws Exception {
                throw new UnsupportedOperationException();
            }

            @Override
            protected Map<String, Boolean> getTaskWorkStatus() {
                throw new UnsupportedOperationException();
            }
        }, false);
    }

    public TaskAndMilestone(DataflowTask task) {
        this(task, true);
    }

    public TaskAndMilestone(DataflowTask task, boolean isTsk) {
        super();
        this.task = task;
        this.milestone = new MilestoneImpl((isTsk ? MILESTONE_PREFIX : StringUtils.EMPTY) + task.id);
    }
}
