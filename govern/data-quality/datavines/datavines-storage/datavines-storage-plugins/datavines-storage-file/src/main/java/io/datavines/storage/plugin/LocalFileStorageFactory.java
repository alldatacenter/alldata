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
package io.datavines.storage.plugin;

import io.datavines.common.utils.StringUtils;
import io.datavines.storage.api.StorageConnector;
import io.datavines.storage.api.StorageExecutor;
import io.datavines.storage.api.StorageFactory;

import java.util.Map;

public class LocalFileStorageFactory implements StorageFactory {

    @Override
    public StorageConnector getStorageConnector() {
        return new LocalFileStorageConnector();
    }

    @Override
    public String getCategory() {
        return "file";
    }

    @Override
    public StorageExecutor getStorageExecutor() {
        return new LocalFileStorageExecutor();
    }

    @Override
    public String getErrorDataScript(Map<String, String> configMap) {
        String errorDataFileName = configMap.get("error_data_file_name");
        if (StringUtils.isNotEmpty(errorDataFileName)) {
            return errorDataFileName + ".csv";
        }
        return null;
    }

    @Override
    public String getValidateResultDataScript(Map<String, String> configMap) {
        String executionId = configMap.get("execution_id");
        if (StringUtils.isNotEmpty(executionId)) {
            return executionId + "/validate_result.csv";
        }
        return null;
    }
}
