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
package com.qlangtech.tis.offline.pojo;

import com.qlangtech.tis.git.GitUtils.JoinRule;

/**
 *  Git仓库保存的workflow信息
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class WorkflowPojo {

    private String name;
    private JoinRule task;

    public WorkflowPojo() {
    }

    public WorkflowPojo(String name, JoinRule task) {
        this.name = name;
        this.task = task;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JoinRule getTask() {
        return task;
    }

    public void setTask(JoinRule task) {
        this.task = task;
    }
}
