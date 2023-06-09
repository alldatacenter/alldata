package com.qlangtech.tis.sql.parser;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.qlangtech.tis.fullbuild.taskflow.TaskAndMilestone;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
 * @create: 2023-02-14 10:08
 **/
public class DAGSessionSpec {
    Map<String, DAGSessionSpec> dptNodes = Maps.newHashMap();
    private static final String KEY_ROOT = "root";
    private final String id;

    List<DAGSessionSpec> attains = Lists.newArrayList();

    boolean milestone = false;

    private final Map<String /** taskid*/, TaskAndMilestone>
            taskMap;

    public Map<String, TaskAndMilestone> getTaskMap() {
        return taskMap;
    }

    public DAGSessionSpec setMilestone() {
        this.milestone = true;
        this.taskMap.put(this.id, TaskAndMilestone.createMilestone(this.id));
        return this;
    }

    public DAGSessionSpec(String id, Map<String, TaskAndMilestone> taskMap) {
        this.id = id;
        this.taskMap = taskMap;
    }

    public DAGSessionSpec() {
        this(KEY_ROOT, Maps.newHashMap());
    }

    public StringBuffer buildSpec() {
        return buildSpec(Sets.newHashSet());
    }

    private StringBuffer buildSpec(Set<String> collected) {

        StringBuffer specs = new StringBuffer();
        for (DAGSessionSpec spec : dptNodes.values()) {
            specs.append(spec.buildSpec(collected)).append(" ");
        }
        if (StringUtils.equals(this.id, KEY_ROOT)) {
            return specs;
        }
        if (!this.milestone && collected.add(this.id)) {
            specs.append(dptNodes.values().stream().map((n) -> n.id).collect(Collectors.joining(","))).append("->").append(this.id);
            if (CollectionUtils.isNotEmpty(this.attains)) {
                specs.append("->").append(this.attains.stream().map((a) -> a.id).collect(Collectors.joining(",")));
            }
        }
        return specs;
    }

    public DAGSessionSpec getDpt(String id) {
        DAGSessionSpec spec = null;
        if ((spec = dptNodes.get(id)) == null) {
            spec = this.addDpt(id);
            if (this.milestone) {
                spec.attains.add(this);
            }
            return spec;
        } else {
            return spec;
        }
    }

    private DAGSessionSpec addDpt(String id) {
        DAGSessionSpec spec = new DAGSessionSpec(id, this.taskMap);
        this.dptNodes.put(id, spec);
        return spec;
    }

    public DAGSessionSpec addDpt(DAGSessionSpec spec) {
        dptNodes.put(spec.id, spec);
        return this;
    }

    public void put(String taskName, TaskAndMilestone taskAndMilestone) {
        this.taskMap.put(taskName, taskAndMilestone);
    }
}
