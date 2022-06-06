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

import org.apache.atlas.annotation.GraphTransaction;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.tasks.AtlasTask;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasGraphQuery;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.utils.AtlasJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.repository.Constants.TASK_GUID;
import static org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2.setEncodedProperty;

@Component
public class TaskRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(TaskRegistry.class);

    private AtlasGraph graph;

    @Inject
    public TaskRegistry(AtlasGraph graph) {
        this.graph = graph;
    }

    @GraphTransaction
    public AtlasTask save(AtlasTask task) {
        AtlasVertex vertex = createVertex(task);

        return toAtlasTask(vertex);
    }

    @GraphTransaction
    public List<AtlasTask> getPendingTasks() {
        List<AtlasTask> ret = new ArrayList<>();

        try {
            AtlasGraphQuery query = graph.query()
                                         .has(Constants.TASK_TYPE_PROPERTY_KEY, Constants.TASK_TYPE_NAME)
                                         .has(Constants.TASK_STATUS, AtlasTask.Status.PENDING)
                                         .orderBy(Constants.TASK_CREATED_TIME, AtlasGraphQuery.SortOrder.ASC);

            Iterator<AtlasVertex> results = query.vertices().iterator();

            while (results.hasNext()) {
                AtlasVertex vertex = results.next();

                ret.add(toAtlasTask(vertex));
            }
        } catch (Exception exception) {
            LOG.error("Error fetching pending tasks!", exception);
        } finally {
            graph.commit();
        }

        return ret;
    }

    @GraphTransaction
    public void updateStatus(AtlasVertex taskVertex, AtlasTask task) {
        if (taskVertex == null) {
            return;
        }

        setEncodedProperty(taskVertex, Constants.TASK_ATTEMPT_COUNT, task.getAttemptCount());
        setEncodedProperty(taskVertex, Constants.TASK_STATUS, task.getStatus().toString());
        setEncodedProperty(taskVertex, Constants.TASK_UPDATED_TIME, System.currentTimeMillis());
        setEncodedProperty(taskVertex, Constants.TASK_ERROR_MESSAGE, task.getErrorMessage());
    }

    @GraphTransaction
    public void deleteByGuid(String guid) throws AtlasBaseException {
        try {
            AtlasGraphQuery query = graph.query()
                                         .has(Constants.TASK_TYPE_PROPERTY_KEY, Constants.TASK_TYPE_NAME)
                                         .has(TASK_GUID, guid);

            Iterator<AtlasVertex> results = query.vertices().iterator();

            if (results.hasNext()) {
                graph.removeVertex(results.next());
            }
        } catch (Exception exception) {
            LOG.error("Error: deletingByGuid: {}", guid);

            throw new AtlasBaseException(exception);
        }
    }

    @GraphTransaction
    public void deleteComplete(AtlasVertex taskVertex, AtlasTask task) {
        updateStatus(taskVertex, task);

        deleteVertex(taskVertex);
    }

    @GraphTransaction
    public AtlasTask getById(String guid) {
        AtlasGraphQuery query = graph.query()
                                     .has(Constants.TASK_TYPE_PROPERTY_KEY, Constants.TASK_TYPE_NAME)
                                     .has(TASK_GUID, guid);

        Iterator<AtlasVertex> results = query.vertices().iterator();

        return results.hasNext() ? toAtlasTask(results.next()) : null;
    }

    @GraphTransaction
    public AtlasVertex getVertex(String taskGuid) {
        AtlasGraphQuery query = graph.query().has(Constants.TASK_GUID, taskGuid);

        Iterator<AtlasVertex> results = query.vertices().iterator();

        return results.hasNext() ? results.next() : null;
    }

    @GraphTransaction
    public List<AtlasTask> getAll() {
        List<AtlasTask> ret = new ArrayList<>();
        AtlasGraphQuery query = graph.query()
                                     .has(Constants.TASK_TYPE_PROPERTY_KEY, Constants.TASK_TYPE_NAME)
                                     .orderBy(Constants.TASK_CREATED_TIME, AtlasGraphQuery.SortOrder.ASC);

        Iterator<AtlasVertex> results = query.vertices().iterator();

        while (results.hasNext()) {
            ret.add(toAtlasTask(results.next()));
        }

        return ret;
    }

    public void commit() {
        this.graph.commit();
    }

    public AtlasTask createVertex(String taskType, String createdBy, Map<String, Object> parameters) {
        AtlasTask ret = new AtlasTask(taskType, createdBy, parameters);

        createVertex(ret);

        return ret;
    }

    private void deleteVertex(AtlasVertex taskVertex) {
        if (taskVertex == null) {
            return;
        }

        graph.removeVertex(taskVertex);
    }

    private AtlasTask toAtlasTask(AtlasVertex v) {
        AtlasTask ret = new AtlasTask();

        String guid = v.getProperty(Constants.TASK_GUID, String.class);
        if (guid != null) {
            ret.setGuid(guid);
        }

        String type = v.getProperty(Constants.TASK_TYPE, String.class);
        if (type != null) {
            ret.setType(type);
        }

        String status = v.getProperty(Constants.TASK_STATUS, String.class);
        if (status != null) {
            ret.setStatus(status);
        }

        String createdBy = v.getProperty(Constants.TASK_CREATED_BY, String.class);
        if (createdBy != null) {
            ret.setCreatedBy(createdBy);
        }

        Long createdTime = v.getProperty(Constants.TASK_CREATED_TIME, Long.class);
        if (createdTime != null) {
            ret.setCreatedTime(new Date(createdTime));
        }

        Long updatedTime = v.getProperty(Constants.TASK_UPDATED_TIME, Long.class);
        if (updatedTime != null) {
            ret.setUpdatedTime(new Date(updatedTime));
        }

        Long startTime = v.getProperty(Constants.TASK_START_TIME, Long.class);
        if (startTime != null) {
            ret.setStartTime(new Date(startTime));
        }

        Long endTime = v.getProperty(Constants.TASK_END_TIME, Long.class);
        if (endTime != null) {
            ret.setEndTime(new Date(endTime));
        }

        String parametersJson = v.getProperty(Constants.TASK_PARAMETERS, String.class);
        if (parametersJson != null) {
            ret.setParameters(AtlasType.fromJson(parametersJson, Map.class));
        }

        Integer attemptCount = v.getProperty(Constants.TASK_ATTEMPT_COUNT, Integer.class);
        if (attemptCount != null) {
            ret.setAttemptCount(attemptCount);
        }

        String errorMessage = v.getProperty(Constants.TASK_ERROR_MESSAGE, String.class);
        if (errorMessage != null) {
            ret.setErrorMessage(errorMessage);
        }

        return ret;
    }

    private AtlasVertex createVertex(AtlasTask task) {
        AtlasVertex ret = graph.addVertex();

        setEncodedProperty(ret, Constants.TASK_GUID, task.getGuid());
        setEncodedProperty(ret, Constants.TASK_TYPE_PROPERTY_KEY, Constants.TASK_TYPE_NAME);
        setEncodedProperty(ret, Constants.TASK_STATUS, task.getStatus().toString());
        setEncodedProperty(ret, Constants.TASK_TYPE, task.getType());
        setEncodedProperty(ret, Constants.TASK_CREATED_BY, task.getCreatedBy());
        setEncodedProperty(ret, Constants.TASK_CREATED_TIME, task.getCreatedTime());
        setEncodedProperty(ret, Constants.TASK_UPDATED_TIME, task.getUpdatedTime());

        if (task.getStartTime() != null) {
            setEncodedProperty(ret, Constants.TASK_START_TIME, task.getStartTime().getTime());
        }

        if (task.getEndTime() != null) {
            setEncodedProperty(ret, Constants.TASK_END_TIME, task.getEndTime().getTime());
        }

        setEncodedProperty(ret, Constants.TASK_PARAMETERS, AtlasJson.toJson(task.getParameters()));
        setEncodedProperty(ret, Constants.TASK_ATTEMPT_COUNT, task.getAttemptCount());
        setEncodedProperty(ret, Constants.TASK_ERROR_MESSAGE, task.getErrorMessage());

        return ret;
    }
}