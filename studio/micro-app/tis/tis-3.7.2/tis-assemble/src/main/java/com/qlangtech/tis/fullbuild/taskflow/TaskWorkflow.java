/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.fullbuild.taskflow;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.qlangtech.tis.fullbuild.taskflow.impl.EndTask;
import com.qlangtech.tis.fullbuild.taskflow.impl.ForkTask;
import com.qlangtech.tis.fullbuild.taskflow.impl.StartTask;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.lang3.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 一个简单的工作流实现
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年11月30日
 */
public class TaskWorkflow {

    final ConcurrentMap<String, TaskDependency> /* taskname */
    tasksMap = Maps.newConcurrentMap();

    private final ExecutorService forkExecutorService = Executors.newCachedThreadPool();

    public void init() {
        // Collections.unmodifiableMap(tasksMap);
        for (TaskDependency task : tasksMap.values()) {
            task.init(this);
        }
        System.out.println("=======================================");
        for (TaskDependency task : tasksMap.values()) {
            System.out.println(task.getTask().getName() + "->" + task.getPrecondition());
        }
        System.out.println("=======================================");
    }

    /**
     * 取得依赖的表
     *
     * @return
     */
    public Set<EntityName> getDependenciesTables() {
        Set<EntityName> tables = Sets.newHashSet();
        for (TaskDependency task : tasksMap.values()) {
            tables.addAll(task.getTask().getDependencyTables());
        }
        return tables;
    }

    public TaskDependency getTaskDependency(String taskname) {
        return tasksMap.get(taskname);
    }

    /**
     * 开始执行workflow
     */
    public void startExecute(Map<String, Object> params) {
        TaskDependency start = tasksMap.get(StartTask.NAME);
        if (start == null) {
            throw new IllegalStateException("start task have not been defined");
        }
        this.execute(start, params);
        this.findEndTaskAndWaittingStop();
    }

    /**
     * 找到工作流中的end节点，并且等待它结束
     */
    private void findEndTaskAndWaittingStop() {
        EndTask end = null;
        for (TaskDependency t : tasksMap.values()) {
            if (t.getTask() instanceof EndTask) {
                if (end == null) {
                    end = (EndTask) t.getTask();
                } else {
                    throw new IllegalStateException("there is another endtask ,name:" + t.getTask().getName());
                }
            }
        }
        if (end == null) {
            throw new IllegalStateException("end task have not been defined");
        }
        try {
            end.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void execute(TaskDependency task, Map<String, Object> params) {
        if (task.getTask() instanceof ForkTask) {
            ForkTask fork = (ForkTask) task.getTask();
            String[] tos = StringUtils.split(fork.getSuccessTo(), ",");
            TaskDependency toTask = null;
            for (String to : tos) {
                toTask = tasksMap.get(to);
                if (toTask == null) {
                    throw new IllegalStateException("to task:'" + to + "' can not be found in taskMap");
                }
                final TaskDependency doTask = toTask;
                forkExecutorService.execute(() -> {
                    doTask.satisfaction(fork.getName());
                    this.execute(doTask, params);
                });
            }
        } else {
            if (task.exexute(params)) {
                BasicTask t = task.getTask();
                String toTask = t.getSuccessTo();
                if (StringUtils.isBlank(toTask)) {
                    return;
                }
                TaskDependency to = tasksMap.get(toTask);
                if (to != null) {
                    to.satisfaction(t.getName());
                    this.execute(to, params);
                }
            }
        }
    }

    /**
     * @return
     */
    public List<BasicTask> getAllTask() {
        return this.tasksMap.values().stream().map((r) -> r.getTask()).collect(Collectors.toList());
    }

    public void addTask(BasicTask task) {
        TaskDependency pre = null;
        TaskDependency t = new TaskDependency(task);
        if ((pre = tasksMap.putIfAbsent(task.getName(), t)) != null) {
            throw new IllegalStateException("name:" + task.getName() + " has exist");
        }
    }
}
