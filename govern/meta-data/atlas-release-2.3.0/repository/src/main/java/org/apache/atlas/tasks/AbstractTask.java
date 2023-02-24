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

import org.apache.atlas.model.tasks.AtlasTask;

import static org.apache.atlas.model.tasks.AtlasTask.Status;

public abstract class AbstractTask {
    private final AtlasTask task;

    public AbstractTask(AtlasTask task) {
        this.task = task;
    }

    public void run() throws Exception {
        try {
            perform();
        } catch (Exception exception) {
            task.setStatusPending();

            task.setErrorMessage(exception.getMessage());

            task.incrementAttemptCount();

            throw exception;
        } finally {
            task.end();
        }
    }

    protected void setStatus(Status status) {
        task.setStatus(status);
    }

    public Status getStatus() {
        return this.task.getStatus();
    }

    public String getTaskGuid() {
        return task.getGuid();
    }

    public String getTaskType() {
        return task.getType();
    }

    protected AtlasTask getTaskDef() {
        return this.task;
    }

    public abstract Status perform() throws Exception;
}