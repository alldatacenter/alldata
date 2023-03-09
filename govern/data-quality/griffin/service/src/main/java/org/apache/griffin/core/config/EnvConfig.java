/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.config;

import static org.apache.griffin.core.util.FileUtil.getFilePath;
import static org.apache.griffin.core.util.JsonUtil.toEntity;
import static org.apache.griffin.core.util.JsonUtil.toJsonWithFormat;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

public class EnvConfig {
    private static final Logger LOGGER = LoggerFactory
        .getLogger(EnvConfig.class);
    public static String ENV_BATCH;
    public static String ENV_STREAMING;

    /**
     * read env config from resource.
     *
     * @param path resource path
     * @return String
     * @throws IOException io exception
     */
    private static String readEnvFromResource(String path)
        throws IOException {
        if (path == null) {
            LOGGER.warn("Parameter path is null.");
            return null;
        }
        // Be careful, here we use getInputStream() to convert
        // path file to stream.
        // It'll cause FileNotFoundException if you use  getFile()
        // to convert path file to File Object
        InputStream in = new ClassPathResource(path).getInputStream();
        Object result = null;
        try {
            result = toEntity(in, new TypeReference<Object>() {
            });
        } finally {
            in.close();
        }
        return toJsonWithFormat(result);
    }

    /**
     * read env config from resource.
     *
     * @param path resource path
     * @return String
     * @throws IOException io exception
     */
    private static String readEnvFromAbsolutePath(String path)
        throws IOException {
        if (path == null) {
            LOGGER.warn("Parameter path is null.");
            return null;
        }

        FileInputStream in = new FileInputStream(path);
        Object result = null;
        try {
            result = toEntity(in, new TypeReference<Object>() {
            });
        } finally {
            in.close();
        }
        return toJsonWithFormat(result);
    }

    /**
     * read batch env.
     *
     * @param name        batch env name that you need to search
     * @param defaultPath If there is no target file in location directory,
     *                    it'll read from default path.
     * @param location    env path that you configure in application.properties
     * @return String
     * @throws IOException io exception
     */
    static String getBatchEnv(String name, String defaultPath, String location)
        throws IOException {
        if (ENV_BATCH != null) {
            return ENV_BATCH;
        }
        String path = getFilePath(name, location);
        if (path == null) {
            path = defaultPath;
            ENV_BATCH = readEnvFromResource(path);
        } else {
            ENV_BATCH = readEnvFromAbsolutePath(path);
        }
        LOGGER.info(ENV_BATCH);
        return ENV_BATCH;
    }

    static String getStreamingEnv(String name,
                                  String defaultPath,
                                  String location)
        throws IOException {
        if (ENV_STREAMING != null) {
            return ENV_STREAMING;
        }
        String path = getFilePath(name, location);
        if (path == null) {
            path = defaultPath;
            ENV_STREAMING = readEnvFromResource(path);
        } else {
            ENV_STREAMING = readEnvFromAbsolutePath(path);
        }
        LOGGER.info(ENV_STREAMING);
        return ENV_STREAMING;
    }
}
