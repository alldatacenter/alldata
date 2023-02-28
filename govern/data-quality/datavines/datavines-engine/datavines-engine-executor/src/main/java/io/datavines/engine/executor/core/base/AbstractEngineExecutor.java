/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.engine.executor.core.base;

import java.util.List;

import io.datavines.common.entity.JobExecutionRequest;
import org.slf4j.Logger;

import io.datavines.common.entity.ProcessResult;
import io.datavines.engine.api.engine.EngineExecutor;

public abstract class AbstractEngineExecutor implements EngineExecutor {

    protected JobExecutionRequest jobExecutionRequest;

    protected Logger logger;

    protected volatile boolean cancel = false;

    protected ProcessResult processResult;

    public AbstractEngineExecutor() {

    }

    /**
     * log handle
     * @param logs log list
     */
    public void logHandle(List<String> logs) {
        // note that the "new line" is added here to facilitate log parsing
        logger.info(" -> {}", String.join("\n\t", logs));
    }

    @Override
    public boolean isCancel() throws Exception {
        return cancel;
    }

    protected abstract String buildCommand();
}
