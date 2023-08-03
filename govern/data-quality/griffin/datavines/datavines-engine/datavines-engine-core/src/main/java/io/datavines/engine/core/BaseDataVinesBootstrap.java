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
package io.datavines.engine.core;

import io.datavines.common.entity.ProcessResult;
import io.datavines.common.enums.ExecutionStatus;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;
import io.datavines.common.config.CheckResult;
import io.datavines.common.config.ConfigRuntimeException;
import io.datavines.engine.api.component.Component;
import io.datavines.engine.api.env.Execution;
import io.datavines.engine.api.env.RuntimeEnvironment;
import io.datavines.engine.core.config.ConfigParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseDataVinesBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(BaseDataVinesBootstrap.class);

    private Execution execution;

    public ProcessResult execute(String[] args) {
        if (args.length == 1) {
            try {
                parseConfigAndExecute(args[0]);
                return new ProcessResult(ExecutionStatus.SUCCESS.getCode());
            } catch (ConfigRuntimeException e) {
                showConfigError(e);
            } catch (Exception e) {
                showFatalError(e);
            } finally {
                stop();
            }
        }
        return new ProcessResult();
    }

    public void stop() {
        try {
            if (execution != null) {
                execution.stop();
            }
        } catch (Exception e) {
            logger.error("close execution error : ", e);
        }
    }

    private void parseConfigAndExecute(String configFile) throws Exception {

        ConfigParser configParser = new ConfigParser(configFile);
        List<Component> sources = configParser.getSourcePlugins();
        List<Component> transforms = configParser.getTransformPlugins();
        List<Component> sinks = configParser.getSinkPlugins();
        execution = configParser.getRuntimeEnvironment().getExecution();
        checkConfig(sources, transforms, sinks);
        prepare(configParser.getRuntimeEnvironment(), sources, transforms, sinks);
        if (execution == null) {
            throw new Exception("can not create execution , please check the config");
        }

        execution.execute(sources, transforms, sinks);
    }

    @SafeVarargs
    private final void checkConfig(List<? extends Component>... components) throws Exception{
        boolean configValid = true;
        for (List<? extends Component> componentList : components) {
            for (Component component : componentList) {
                CheckResult checkResult;
                try {
                    checkResult = component.checkConfig();
                } catch (Exception e) {
                    checkResult = new CheckResult(false, e.getMessage());
                }

                if (!checkResult.isSuccess()) {
                    configValid = false;
                    logger.info(String.format("Component[%s] contains invalid config, error: %s\n"
                            , component.getClass().getName(), checkResult.getMsg()));
                }

                if (!configValid) {
                    // invalid configuration
                    throw new Exception("config is invalid , please check the config");
                }
            }
        }
    }

    @SafeVarargs
    private final void prepare(RuntimeEnvironment env, List<? extends Component>... components) throws Exception{
        for (List<? extends Component> componentList : components) {
            for (Component component : componentList) {
                component.prepare(env);
            }
        }
    }

    private void showConfigError(Throwable throwable) {
        String errorMsg = throwable.getMessage();
        logger.info("Config Error:\n");
        logger.info("Reason: " + errorMsg + "\n");
    }

    private void showFatalError(Throwable throwable) {
        String errorMsg = throwable.getMessage();
        logger.info("Fatal Error, \n");
        logger.info("Reason: " + errorMsg + "\n");
        logger.info("Exception StackTrace: " + ExceptionUtils.getStackTrace(throwable));
    }
}
