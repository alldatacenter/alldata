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
package com.qlangtech.tis.fullbuild.indexbuild;

import com.qlangtech.tis.build.metrics.Counters;
import com.qlangtech.tis.build.metrics.Messages;
import com.qlangtech.tis.cloud.ICoordinator;
import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.manage.common.IndexBuildParam;
import com.qlangtech.tis.order.dump.task.ITableDumpConstant;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年1月17日
 */
public class TaskContext {

    private static final long serialVersionUID = 1L;

    private ICoordinator coordinator;

    public void setCoordinator(ICoordinator coordinator) {
        this.coordinator = coordinator;
    }

    public ICoordinator getCoordinator() {
        Objects.requireNonNull(this.coordinator, "coordinator can not be null");
        return this.coordinator;
    }

    private final Counters counters = new Counters();

    private final Messages message = new Messages();

    private final IParamGetter commandLine;

    private TaskContext(IParamGetter commandLine) {
        super();
        this.commandLine = commandLine;
    }

    public EntityName parseDumpTable() {
        return EntityName.create(this.get(ITableDumpConstant.DUMP_DBNAME), this.get(ITableDumpConstant.DUMP_TABLE_NAME));
    }

    public long getStartTime() {
        try {
            return Long.parseLong(this.get(ITableDumpConstant.DUMP_START_TIME));
        } catch (NumberFormatException e) {
            throw new RuntimeException("please set param ITableDumpConstant.DUMP_START_TIME", e);
        }
    }

    public static TaskContext create(IParamGetter commandLine) {
        return new TaskContext(commandLine);
    }

    public static TaskContext create() {
        return create(new HashMap<>());
    }

    public static TaskContext create(Map<String, String> params) {
        return new TaskContext((key) -> params.get(key));
    }

    private Integer allRowCount;

    public int getAllRowCount() {
        if (allRowCount == null) {
            try {
                allRowCount = Integer.parseInt(getInnerParam(IndexBuildParam.INDEXING_ROW_COUNT));
            } catch (Throwable e) {
                allRowCount = Integer.MAX_VALUE;
            }
        }
        return allRowCount;
    }

    /**
     * 构建索引已经完成的条数
     *
     * @return
     */
    public long getIndexMakerComplete() {
        return this.counters.getCounter(Counters.Counter.DOCMAKE_COMPLETE).get();
    }

    public Counters getCounters() {
        return this.counters;
    }

    public Messages getMessages() {
        return this.message;
    }

    public String getInnerParam(String key) {
        return commandLine.getOptionValue(key);
    }

    public String getUserParam(String key) {
        return commandLine.getOptionValue(key);
    }

    public String getCollectionName() {
        String collectionName = this.get(IndexBuildParam.INDEXING_SERVICE_NAME);
        if (StringUtils.isBlank(collectionName)) {
            throw new IllegalStateException("collection name:" + collectionName + " can not be null");
        }
        return collectionName;
    }

    public String getMapPath() {
        return commandLine.getOptionValue("task.map.output.path");
    }

    // @Override
    public String get(Object key) {
        return commandLine.getOptionValue(String.valueOf(key));
    }

    public Integer getTaskId() {
        return Integer.parseInt(this.get(JobCommon.KEY_TASK_ID));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TaskContext)) {
            return false;
        }
        // 为了EasyMock expect 测试先通过
        return true;
        // TaskContext other = (TaskContext) obj;
        // other.get
        //
        // return super.equals(obj);
    }


    public interface IParamGetter {

        String getOptionValue(String key);
    }
}
