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
package com.qlangtech.tis.fullbuild.phasestatus.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 单Groupbuild状态
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月17日
 */
public class BuildSharedPhaseStatus extends AbstractChildProcessStatus {

    private static final String NAME = "build";

    private long allBuildSize;

    // 已经处理的记录数
    private long buildReaded;

    private Integer taskid;
    // 分组名称
    private String sharedName;

    public String getSharedName() {
        return this.sharedName;
    }

    public void setSharedName(String sharedName) {
        this.sharedName = sharedName;
    }

    public Integer getTaskid() {
        return taskid;
    }

    public void setTaskid(Integer taskid) {
        this.taskid = taskid;
    }

    @Override
    public String getAll() {
        return FileUtils.byteCountToDisplaySize(this.allBuildSize);
    }

    @Override
    public String getProcessed() {
        return buildReaded + "r";
    }

    @Override
    public int getPercent() {
        // FIXME: buildReaded 是记录条数，allBuildSize是总文件size
        return (int) ((this.buildReaded * 1f / this.allBuildSize) * 100);
    }

    @Override
    public final String getName() {
        if (StringUtils.isEmpty(getSharedName())) {
            return StringUtils.EMPTY;
        } else {
            return getSharedName() + "-" + NAME;
        }
    }

    public void setAllBuildSize(long allBuildSize) {
        this.allBuildSize = allBuildSize;
    }

    public void setBuildReaded(long buildReaded) {
        this.buildReaded = buildReaded;
    }

    public long getAllBuildSize() {
        return this.allBuildSize;
    }

    public long getBuildReaded() {
        return this.buildReaded;
    }
}
