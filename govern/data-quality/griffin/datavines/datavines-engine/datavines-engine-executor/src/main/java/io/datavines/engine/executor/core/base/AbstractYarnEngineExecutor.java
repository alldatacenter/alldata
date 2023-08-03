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

import io.datavines.common.utils.YarnUtils;
import io.datavines.engine.executor.core.executor.ShellCommandProcess;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public abstract class AbstractYarnEngineExecutor extends AbstractEngineExecutor {

    protected ShellCommandProcess shellCommandProcess;

    @Override
    public void cancel() throws Exception {

        cancel = true;
        // cancel process
        shellCommandProcess.cancel();

        killYarnApplication();

    }

    private void killYarnApplication() {

        try {
            String applicationId = YarnUtils.getYarnAppId(jobExecutionRequest.getTenantCode(), jobExecutionRequest.getJobExecutionUniqueId());

            if (StringUtils.isNotEmpty(applicationId)) {
                // sudo -u user command to run command
                String cmd = String.format("sudo -u %s yarn application -kill %s", jobExecutionRequest.getTenantCode(), applicationId);

                logger.info("yarn application -kill {}", applicationId);

                Runtime.getRuntime().exec(cmd);
            }

        } catch (IOException e) {
            logger.info("kill attempt failed." + e.getMessage(), e);
        }

    }
}
