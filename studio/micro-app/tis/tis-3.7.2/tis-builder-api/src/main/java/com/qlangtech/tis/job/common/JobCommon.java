package com.qlangtech.tis.job.common;

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

import org.apache.commons.lang.StringUtils;
import org.slf4j.MDC;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-11-27 11:12
 **/
public interface JobCommon {
    String KEY_TASK_ID = "taskid";
    String KEY_COLLECTION = "app";

    public static void setMDC(IPipelineExecContext pec) {
        MDC.put(JobCommon.KEY_TASK_ID, String.valueOf(pec.getTaskId()));
        if (pec.hasIndexName()) {
            MDC.put(JobCommon.KEY_COLLECTION, pec.getIndexName());
        }
    }

    public static void setMDC(int taskId) {
        setMDC(taskId, null);
    }

    public static void setMDC(int taskId, String pipelineName) {
        setMDC(new IPipelineExecContext() {
            @Override
            public int getTaskId() {
                return taskId;
            }

            @Override
            public String getIndexName() {
                if (!hasIndexName()) {
                    throw new IllegalStateException("pipelineName is empty");
                }
                return pipelineName;
            }

            @Override
            public boolean hasIndexName() {
                return StringUtils.isNotEmpty(pipelineName);
            }
        });
    }
}
