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

package org.apache.griffin.core.util;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {
    private static final Logger LOGGER = LoggerFactory
        .getLogger(FileUtil.class);

    public static String getFilePath(String name, String location) {
        if (StringUtils.isEmpty(location)) {
            LOGGER.info("Location is empty. Read from default path.");
            return null;
        }
        File file = new File(location);
        LOGGER.info("File absolute path:" + file.getAbsolutePath());
        File[] files = file.listFiles();
        if (files == null) {
            LOGGER.warn("The external location '{}' does not exist.Read from"
                + "default path.", location);
            return null;
        }
        return getFilePath(name, files, location);
    }

    private static String getFilePath(String name, File[] files,
                                      String location) {
        String path = null;
        for (File f : files) {
            if (f.getName().equals(name)) {
                path = location + File.separator + name;
                LOGGER.info("config real path: {}", path);
            }
        }
        return path;
    }
}
