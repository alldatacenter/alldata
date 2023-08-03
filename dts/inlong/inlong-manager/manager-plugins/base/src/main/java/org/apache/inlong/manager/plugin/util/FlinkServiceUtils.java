/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.plugin.util;

import org.apache.inlong.manager.plugin.flink.dto.FlinkConfig;
import org.apache.inlong.manager.plugin.flink.enums.Constants;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.configuration.Configuration;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class FlinkServiceUtils {

    private static final String DEFAULT_PLUGINS = "plugins";

    private static final String FILE_PREFIX = "file://";

    public static Object getFlinkClientService(Configuration configuration, FlinkConfig flinkConfig) {
        log.info("Flink version {}", flinkConfig.getVersion());

        Path pluginPath = Paths.get(DEFAULT_PLUGINS).toAbsolutePath();
        String flinkJarName = String.format(Constants.FLINK_JAR_NAME, flinkConfig.getVersion());
        String flinkClientPath = FILE_PREFIX + pluginPath + File.separator + flinkJarName;
        log.info("Start to load Flink jar: {}", flinkClientPath);

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{new URL(flinkClientPath)}, Thread.currentThread()
                .getContextClassLoader())) {
            Class<?> flinkClientService = classLoader.loadClass(Constants.FLINK_CLIENT_CLASS);
            Object flinkService = flinkClientService.getDeclaredConstructor(Configuration.class)
                    .newInstance(configuration);
            log.info("Successfully loaded Flink service");
            return flinkService;
        } catch (Exception e) {
            log.error("Failed to loaded Flink service, please check flink client jar path: {}", flinkClientPath);
            throw new RuntimeException(e);
        }
    }
}
