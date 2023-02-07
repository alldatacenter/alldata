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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Util of flink.
 */
@Slf4j
public class FlinkUtils {

    public static final String BASE_DIRECTORY = "config";

    public static final List<String> FLINK_VERSION_COLLECTION = Collections.singletonList("Flink-1.13");

    /**
     * getLatestFlinkVersion
     */
    public static String getLatestFlinkVersion(String[] supportedFlink) {
        if (Objects.isNull(supportedFlink)) {
            return null;
        }
        Arrays.sort(supportedFlink, Collections.reverseOrder());
        String latestFinkVersion = null;
        for (String flinkVersion : supportedFlink) {
            latestFinkVersion = FLINK_VERSION_COLLECTION.stream()
                    .filter(v -> v.equals(flinkVersion)).findFirst().orElse(null);
            if (Objects.nonNull(latestFinkVersion)) {
                return latestFinkVersion;
            }
        }
        return latestFinkVersion;
    }

    /**
     * print exception
     */
    public static String getExceptionStackMsg(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter, true));
        return stringWriter.getBuffer().toString();
    }

    /**
     * fetch sort-single-tenant jar path
     *
     * @param baseDirName base directory name.
     * @param pattern pattern of file
     * @return sort-single-tenant jar path
     */
    public static String findFile(String baseDirName, String pattern) {
        List<String> files = listFiles(baseDirName, pattern, 1);
        if (CollectionUtils.isEmpty(files)) {
            return null;
        }
        return files.get(0);
    }

    /**
     * fetch target file path
     *
     * @param baseDirName base directory name.
     * @param pattern pattern of file
     * @return matched files
     */
    public static List<String> listFiles(String baseDirName, String pattern, int limit) {
        List<String> result = new ArrayList<>();

        File baseDir = new File(baseDirName);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            log.error("baseDirName find fail: {}", baseDirName);
            return result;
        }
        String tempName;
        File tempFile;
        File[] files = baseDir.listFiles();
        if (files == null || files.length == 0) {
            log.info("baseDirName is empty");
            return result;
        }
        for (File file : files) {
            tempFile = file;
            tempName = tempFile.getName();
            Pattern jarPathPattern = Pattern.compile(pattern);
            Matcher matcher = jarPathPattern.matcher(tempName);
            boolean matches = matcher.matches();
            if (matches) {
                result.add(tempFile.getAbsoluteFile().toString());
            }
            if (limit > 0 && result.size() >= limit) {
                return result;
            }
        }
        return result;
    }

    /**
     * get value
     */
    public static String getValue(String key, String defaultValue) {
        return StringUtils.isNotEmpty(key) ? key : defaultValue;
    }

    /**
     * getConfigDirectory
     *
     * @param name config file name
     * @return config file directory
     */
    public static String getConfigDirectory(String name) {
        return BASE_DIRECTORY + File.separator + name;
    }

    /**
     * writeConfigToFile
     *
     * @param configJobDirectory job config directory
     * @param configFileName config file name
     * @param content contents of the file to be written
     * @return whether success
     */
    public static boolean writeConfigToFile(String configJobDirectory, String configFileName, String content) {
        File file = new File(configJobDirectory);
        if (!file.exists()) {
            file.mkdirs();
        }
        String filePath = configJobDirectory + File.separator + configFileName;
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(content);
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            log.error("saveConfigToLocal failed", e);
            return false;
        }
        return true;
    }

    /**
     * Delete configuration file
     *
     * @param name file config info
     * @return whether sucess
     */
    public static boolean deleteConfigFile(String name) {
        String configDirectory = getConfigDirectory(name);
        File file = new File(configDirectory);
        if (file.exists()) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                log.error("delete {} failed", configDirectory, e);
                return false;
            }
        }
        return true;
    }
}
