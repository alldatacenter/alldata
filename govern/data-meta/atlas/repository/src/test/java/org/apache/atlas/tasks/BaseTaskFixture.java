/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.tasks;

import org.apache.atlas.model.tasks.AtlasTask;
import org.apache.atlas.repository.graphdb.AtlasGraph;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BaseTaskFixture {
    protected static final String SPYING_TASK_ADD            = "add";
    protected static final String SPYING_TASK_ERROR_THROWING = "errorThrowingTask";

    @Inject
    protected AtlasGraph graph;

    @Inject
    protected TaskRegistry taskRegistry;

    static class SpyConcreteTask extends AbstractTask {
        private boolean taskPerformed;

        public SpyConcreteTask(AtlasTask atlasTask) {
            super(atlasTask);
        }

        @Override
        public AtlasTask.Status perform() {
            this.taskPerformed = true;

            return AtlasTask.Status.COMPLETE;
        }

        public boolean taskPerformed() {
            return this.taskPerformed;
        }
    }

    static class SpyErrorThrowingTask extends AbstractTask {
        private boolean taskPerformed;

        public SpyErrorThrowingTask(AtlasTask atlasTask) {
            super(atlasTask);
        }

        @Override
        public AtlasTask.Status perform() {
            this.taskPerformed = true;

            throw new NullPointerException("SpyErrorThrowingTask: NullPointerException Encountered!");
        }

        public boolean taskPerformed() {
            return this.taskPerformed;
        }
    }

    static class SpyingFactory implements TaskFactory {
        private SpyConcreteTask      addTask;
        private SpyErrorThrowingTask errorTask;

        @Override
        public AbstractTask create(AtlasTask atlasTask) {
            switch (atlasTask.getType()) {
                case "add":
                    addTask = new SpyConcreteTask(atlasTask);
                    return addTask;

                case "errorThrowingTask":
                    errorTask = new SpyErrorThrowingTask(atlasTask);
                    return errorTask;

                default:
                    return null;
            }
        }

        @Override
        public List<String> getSupportedTypes() {
            return new ArrayList<String>() {{
                add(SPYING_TASK_ADD);
                add(SPYING_TASK_ERROR_THROWING);
            }};
        }

        public SpyConcreteTask getAddTask() {
            return this.addTask;
        }

        public SpyErrorThrowingTask getErrorTask() {
            return this.errorTask;
        }
    }

    protected AtlasTask createTask(TaskManagement taskManagement, String type) {
        return taskManagement.createTask(type, "testUser", Collections.singletonMap("params", "params"));
    }
}