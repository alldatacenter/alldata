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

import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.lang3.StringUtils;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年11月30日
 */
public abstract class BasicTask implements ITask {

    protected String name;

    // 节点成功执行，之后执行的节点
    protected String successTo;

    // 依赖的dump表
    private List<EntityName> dependencyTables;

    public final String getName() {
        return this.name;
    }

    public void setDependencyTables(List<EntityName> dependencyTables) {
        this.dependencyTables = dependencyTables;
    }

    /**
     * 取得依赖的表
     *
     * @return
     */
    public List<EntityName> getDependencyTables() {
        return this.dependencyTables;
    }

    public void setName(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("parm name can not be null");
        }
        // StringUtils.defaultIfBlank(name, String.valueOf(UUID.randomUUID()));
        this.name = name;
    }

    public String getSuccessTo() {
        return successTo;
    }

    public void setSuccessTo(String successTo) {
        if (StringUtils.isBlank(successTo)) {
            throw new IllegalArgumentException("param successTo can not be null");
        }
        this.successTo = successTo;
    }
}
