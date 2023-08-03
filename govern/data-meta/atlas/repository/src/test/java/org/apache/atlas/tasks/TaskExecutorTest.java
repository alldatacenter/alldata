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

import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.tasks.AtlasTask;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;
import org.testng.Assert;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Guice(modules = TestModules.TestOnlyModule.class)
public class TaskExecutorTest extends BaseTaskFixture {
    @Inject
    private AtlasGraph graph;

    @Inject
    private TaskRegistry taskRegistry;

    @Inject
    private TaskManagement taskManagement;

    @Test
    public void noTasksExecuted() {
        TaskManagementTest.SpyingFactory spyingFactory = new TaskManagementTest.SpyingFactory();
        Map<String, TaskFactory> taskFactoryMap = new HashMap<>();
        TaskManagement.createTaskTypeFactoryMap(new HashMap<>(), spyingFactory);

        TaskManagement.Statistics statistics = new TaskManagement.Statistics();
        new TaskExecutor(taskRegistry, taskFactoryMap, statistics);

        Assert.assertEquals(statistics.getTotal(), 0);
    }

    @Test
    public void tasksNotPersistedIsNotExecuted() throws InterruptedException {
        TaskManagementTest.SpyingFactory spyingFactory = new TaskManagementTest.SpyingFactory();
        Map<String, TaskFactory> taskFactoryMap = new HashMap<>();
        TaskManagement.createTaskTypeFactoryMap(taskFactoryMap, spyingFactory);

        TaskManagement.Statistics statistics = new TaskManagement.Statistics();
        TaskExecutor taskExecutor = new TaskExecutor(taskRegistry, taskFactoryMap, statistics);

        taskExecutor.addAll(Collections.singletonList(new AtlasTask(SPYING_TASK_ADD, "test", Collections.emptyMap())));

        taskExecutor.waitUntilDone();
        Assert.assertEquals(statistics.getTotal(), 0);
    }


    @Test
    public void persistedIsExecuted() throws AtlasBaseException, InterruptedException {
        TaskManagementTest.SpyingFactory spyingFactory = new TaskManagementTest.SpyingFactory();
        Map<String, TaskFactory> taskFactoryMap = new HashMap<>();
        TaskManagement.createTaskTypeFactoryMap(taskFactoryMap, spyingFactory);

        AtlasTask addTask = taskManagement.createTask("add", "test", Collections.emptyMap());
        AtlasTask errorThrowingTask = taskManagement.createTask("errorThrowingTask", "test", Collections.emptyMap());

        TaskManagement.Statistics statistics = new TaskManagement.Statistics();
        List<AtlasTask> tasks = new ArrayList<AtlasTask>() {{
            add(addTask);
            add(errorThrowingTask);
            }};
        graph.commit();

        TaskExecutor taskExecutor = new TaskExecutor(taskRegistry, taskFactoryMap, statistics);
        taskExecutor.addAll(tasks);

        taskExecutor.waitUntilDone();
        Assert.assertEquals(statistics.getTotal(), 2);
        Assert.assertEquals(statistics.getTotalSuccess(), 1);
        Assert.assertEquals(statistics.getTotalError(), 1);

        Assert.assertNotNull(spyingFactory.getAddTask());
        Assert.assertNotNull(spyingFactory.getErrorTask());

        Assert.assertTrue(spyingFactory.getAddTask().taskPerformed());
        Assert.assertTrue(spyingFactory.getErrorTask().taskPerformed());

        assertTaskUntilFail(errorThrowingTask, taskExecutor);
    }

    private void assertTaskUntilFail(AtlasTask errorThrowingTask, TaskExecutor taskExecutor) throws AtlasBaseException, InterruptedException {
        AtlasTask errorTaskFromDB = taskManagement.getByGuid(errorThrowingTask.getGuid());
        Assert.assertNotNull(errorTaskFromDB);
        Assert.assertTrue(StringUtils.isNotEmpty(errorTaskFromDB.getErrorMessage()));
        Assert.assertEquals(errorTaskFromDB.getAttemptCount(), 1);
        Assert.assertEquals(errorTaskFromDB.getStatus(), AtlasTask.Status.PENDING);

        for (int i = errorTaskFromDB.getAttemptCount(); i <= AtlasTask.MAX_ATTEMPT_COUNT; i++) {
            taskExecutor.addAll(Collections.singletonList(errorThrowingTask));
        }

        taskExecutor.waitUntilDone();
        graph.commit();
        Assert.assertEquals(errorThrowingTask.getStatus(), AtlasTask.Status.FAILED);
    }
}
