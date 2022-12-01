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

import org.apache.atlas.AtlasException;
import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.tasks.AtlasTask;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.testng.Assert;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@Guice(modules = TestModules.TestOnlyModule.class)
public class TaskRegistryTest {
    @Inject
    AtlasGraph graph;

    @Inject
    TaskRegistry registry;

    @Test
    public void basic() throws AtlasException, AtlasBaseException {
        AtlasTask task = new AtlasTask("abcd", "test", Collections.singletonMap("p1", "p1"));
        Assert.assertNull(registry.getById(task.getGuid()));

        AtlasTask taskFromVertex = registry.save(task);
        AtlasVertex taskVertex = registry.getVertex(task.getGuid());

        Assert.assertEquals(taskFromVertex.getGuid(), task.getGuid());
        Assert.assertEquals(taskFromVertex.getType(), task.getType());
        Assert.assertEquals(taskFromVertex.getAttemptCount(), task.getAttemptCount());
        Assert.assertEquals(taskFromVertex.getParameters(), task.getParameters());
        Assert.assertEquals(taskFromVertex.getCreatedBy(), task.getCreatedBy());

        taskFromVertex.incrementAttemptCount();
        taskFromVertex.setStatusPending();
        registry.updateStatus(taskVertex, taskFromVertex);
        registry.commit();

        taskFromVertex = registry.getById(task.getGuid());
        Assert.assertNotNull(taskVertex);
        Assert.assertEquals(taskFromVertex.getStatus(), AtlasTask.Status.PENDING);
        Assert.assertEquals(taskFromVertex.getAttemptCount(), 1);

        registry.deleteByGuid(taskFromVertex.getGuid());

        try {
            AtlasTask t = registry.getById(taskFromVertex.getGuid());
            Assert.assertNull(t);
        }
        catch (IllegalStateException e) {
            Assert.assertTrue(true, "Indicates vertex is deleted!");
        }
    }

    @Test
    public void pendingTasks() throws AtlasBaseException {
        final int MAX_TASKS = 3;
        final String TASK_TYPE_FORMAT = "abcd:%d";

        for (int i = 0; i < MAX_TASKS; i++) {
            AtlasTask task = new AtlasTask(String.format(TASK_TYPE_FORMAT, i), "test", Collections.singletonMap("p1", "p1"));
            registry.save(task);
        }

        List<AtlasTask> pendingTasks = registry.getPendingTasks();
        Assert.assertEquals(pendingTasks.size(), MAX_TASKS);

        for (int i = 0; i < MAX_TASKS; i++) {
            Assert.assertEquals(pendingTasks.get(i).getType(), String.format(TASK_TYPE_FORMAT, i));
            registry.deleteByGuid(pendingTasks.get(i).getGuid());
        }

        graph.commit();
        pendingTasks = registry.getPendingTasks();
        Assert.assertEquals(pendingTasks.size(), 0);
    }
}
