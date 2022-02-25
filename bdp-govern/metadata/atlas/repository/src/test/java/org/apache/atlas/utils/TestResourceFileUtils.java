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
package org.apache.atlas.utils;

import org.apache.atlas.type.AtlasType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TestResourceFileUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TestResourceFileUtils.class);


    public static String getTestFilePath(String fileName) {
        final String userDir = System.getProperty("user.dir");
        String filePath = getTestFilePath(userDir, "", fileName);
        return filePath;
    }

    public static FileInputStream getFileInputStream(String fileName) {
        return getFileInputStream("", fileName);
    }

    public static FileInputStream getFileInputStream(String subDir, String fileName) {
        final String userDir = System.getProperty("user.dir");
        String filePath = getTestFilePath(userDir, subDir, fileName);
        File f = new File(filePath);
        FileInputStream fs = null;
        try {
            fs = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            LOG.error("File could not be found at: {}", filePath, e);
        }
        return fs;
    }

    public static String getDirectory(String subDir) {
        final String userDir = System.getProperty("user.dir");
        return getTestFilePath(userDir, subDir, "");
    }

    public static <T> T readObjectFromJson(String subDir, String filename, Class<T> objectClass) throws IOException {
        final String userDir = System.getProperty("user.dir");
        String filePath = getTestJsonPath(userDir, subDir, filename);
        String json = FileUtils.readFileToString(new File(filePath));
        return AtlasType.fromJson(json, objectClass);
    }

    public static <T> T readObjectFromJson(String filename, Class<T> objectClass) throws IOException {
        return readObjectFromJson("", filename, objectClass);
    }

    public static String getJson(String subDir, String filename) throws IOException {
        final String userDir = System.getProperty("user.dir");
        String filePath = getTestJsonPath(userDir, subDir, filename);
        return FileUtils.readFileToString(new File(filePath));
    }

    public static String getJson(String filename) throws IOException {
        return getJson("", filename);
    }

    private static String getTestFilePath(String startPath, String subDir, String fileName) {
        if (StringUtils.isNotEmpty(subDir)) {
            return startPath + "/src/test/resources/" + subDir + "/" + fileName;
        } else {
            return startPath + "/src/test/resources/" + fileName;
        }
    }

    private static String getTestJsonPath(String startPath, String subDir, String fileName) {
        return startPath + "/src/test/resources/json/" + subDir + "/" + fileName + ".json";
    }
}
