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
package org.apache.atlas.tasks;

import org.apache.atlas.AtlasException;
import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.tasks.AtlasTask;
import org.testng.Assert;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Guice(modules = TestModules.TestOnlyModule.class)
public class TaskManagementTest extends BaseTaskFixture {

    private static class NullFactory implements TaskFactory {
        @Override
        public AbstractTask create(AtlasTask atlasTask) {
            return null;
        }

        @Override
        public List<String> getSupportedTypes() {
            return null;
        }
    }

    @Test
    public void factoryReturningNullIsHandled() throws AtlasException {
        TaskManagement taskManagement = new TaskManagement(null, taskRegistry, new NullFactory());
        taskManagement.start();
    }

    @Test
    public void taskSucceedsTaskVertexRemoved() throws AtlasException, InterruptedException, AtlasBaseException {
        SpyingFactory spyingFactory = new SpyingFactory();
        TaskManagement taskManagement = new TaskManagement(null, taskRegistry, spyingFactory);
        taskManagement.start();

        AtlasTask spyTask = createTask(taskManagement, SPYING_TASK_ADD);
        AtlasTask spyTaskError = createTask(taskManagement, SPYING_TASK_ERROR_THROWING);
        graph.commit();

        taskManagement.addAll(Arrays.asList(spyTask, spyTaskError));

        TimeUnit.SECONDS.sleep(5);
        Assert.assertTrue(spyingFactory.getAddTask().taskPerformed());
        Assert.assertTrue(spyingFactory.getErrorTask().taskPerformed());

        AtlasTask task = taskManagement.getByGuid(spyTask.getGuid());
        Assert.assertNull(task);
    }

    @Test
    public void severalTaskAdds() throws AtlasException, InterruptedException {
        int MAX_THREADS = 5;

        TaskManagement taskManagement = new TaskManagement(null, taskRegistry);
        taskManagement.start();

        Thread[] threads = new Thread[MAX_THREADS];
        for (int i = 0; i < MAX_THREADS; i++) {
            threads[i] = new Thread(() -> {
                try {
                    AtlasTask spyAdd = taskManagement.createTask(SPYING_TASK_ADD, "test", Collections.emptyMap());
                    AtlasTask spyErr = taskManagement.createTask(SPYING_TASK_ERROR_THROWING, "test", Collections.emptyMap());

                    taskManagement.addAll(Collections.singletonList(spyAdd));
                    taskManagement.addAll(Collections.singletonList(spyErr));

                    Thread.sleep(10000);
                    for (int j = 0; j <= AtlasTask.MAX_ATTEMPT_COUNT; j++) {
                        taskManagement.addAll(Collections.singletonList(spyErr));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        for (int i = 0; i < MAX_THREADS; i++) {
            threads[i].start();
        }

        for (int i = 0; i < MAX_THREADS; i++) {
            threads[i].join();
        }
    }
}
