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
package io.datavines.engine.local.api;

import io.datavines.common.config.enums.SinkType;
import io.datavines.common.config.enums.SourceType;
import io.datavines.common.config.enums.TransformType;
import io.datavines.common.exception.DataVinesException;
import io.datavines.engine.api.env.Execution;
import io.datavines.engine.local.api.entity.ResultList;
import org.apache.commons.collections4.CollectionUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static io.datavines.engine.api.EngineConstants.PLUGIN_TYPE;

public class LocalExecution implements Execution<LocalSource, LocalTransform, LocalSink> {

    private final LocalRuntimeEnvironment localRuntimeEnvironment;

    public LocalExecution(LocalRuntimeEnvironment localRuntimeEnvironment){
        this.localRuntimeEnvironment = localRuntimeEnvironment;
    }

    @Override
    public void prepare() throws Exception {

    }

    @Override
    public void execute(List<LocalSource> sources, List<LocalTransform> transforms, List<LocalSink> sinks) throws Exception {
        if (CollectionUtils.isEmpty(sources)) {
            return;
        }

        sources.forEach(localSource -> {
            switch (SourceType.of(localSource.getConfig().getString(PLUGIN_TYPE))){
                case SOURCE:
                    localRuntimeEnvironment.setSourceConnection(localSource.getConnectionItem(localRuntimeEnvironment));
                    if (!localSource.checkTableExist()) {
                        throw new DataVinesException("source table is not exist");
                    }
                    break;
                case TARGET:
                    localRuntimeEnvironment.setTargetConnection(localSource.getConnectionItem(localRuntimeEnvironment));
                    if (!localSource.checkTableExist()) {
                        throw new DataVinesException("target table is not exist");
                    }
                    break;
                case METADATA:
                    localRuntimeEnvironment.setMetadataConnection(localSource.getConnectionItem(localRuntimeEnvironment));
                    break;
                default:
                    break;
            }
        });

        List<ResultList> taskResult = new ArrayList<>();
        List<ResultList> actualValue = new ArrayList<>();
        transforms.forEach(localTransform -> {
            switch (TransformType.of(localTransform.getConfig().getString(PLUGIN_TYPE))){
                case INVALIDATE_ITEMS:
                    localTransform.process(localRuntimeEnvironment);
                    break;
                case ACTUAL_VALUE:
                    ResultList actualValueResult = localTransform.process(localRuntimeEnvironment);
                    actualValue.add(actualValueResult);
                    taskResult.add(actualValueResult);
                    break;
                case EXPECTED_VALUE_FROM_METADATA_SOURCE:
                case EXPECTED_VALUE_FROM_SOURCE:
                case EXPECTED_VALUE_FROM_TARGET_SOURCE:
                    ResultList expectedResult = localTransform.process(localRuntimeEnvironment);
                    taskResult.add(expectedResult);
                    break;
                default:
                    break;
            }
        });

        for(LocalSink localSink : sinks) {
            switch (SinkType.of(localSink.getConfig().getString(PLUGIN_TYPE))){
                case ERROR_DATA:
                    localSink.output(null, localRuntimeEnvironment);
                    break;
                case ACTUAL_VALUE:
                    localSink.output(actualValue, localRuntimeEnvironment);
                    break;
                case VALIDATE_RESULT:
                    localSink.output(taskResult, localRuntimeEnvironment);
                    break;
                case PROFILE_VALUE:
                    localSink.output(actualValue, localRuntimeEnvironment);
                    break;
                default:
                    break;
            }
        }
        localRuntimeEnvironment.close();
    }

    @Override
    public void stop() throws Exception {
        localRuntimeEnvironment.close();
    }
}
