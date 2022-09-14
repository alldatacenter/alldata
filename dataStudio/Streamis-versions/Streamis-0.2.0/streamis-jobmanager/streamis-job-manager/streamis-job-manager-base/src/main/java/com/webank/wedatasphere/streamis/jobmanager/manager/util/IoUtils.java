/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.manager.util;

import org.apache.linkis.common.conf.CommonVars;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


public class IoUtils {
    private static Logger logger = LoggerFactory.getLogger(IoUtils.class);
    private static final String dateFormat_day = "yyyyMMdd";
    private static final String dateFormat_time = "HHmmss";
    private static final String IOUrl = CommonVars.apply("wds.streamis.zip.dir", "/tmp").getValue();

    public static String generateIOPath(String userName, String projectName, String subDir) {
        String baseIOUrl = IOUrl;
        String file = subDir.substring(0,subDir.lastIndexOf("."));
        String dayStr = new SimpleDateFormat(dateFormat_day).format(new Date());
        String timeStr = new SimpleDateFormat(dateFormat_time).format(new Date());
        return addFileSeparator(baseIOUrl, projectName, dayStr, userName, file + "_" + timeStr, subDir);
    }

    private static String addFileSeparator(String... str) {
        return Arrays.stream(str).reduce((a, b) -> a + File.separator + b).orElse("");
    }

    public static OutputStream generateExportOutputStream(String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            logger.warn(String.format("%s is exist,delete it", path));
            boolean success = file.delete();
            if (!success) {
                throw new IOException("Failed to delete existing file: \"" + file.getAbsolutePath() + "\"");
            }
        }
        file.getParentFile().mkdirs();
        boolean success = file.createNewFile();
        if (!success) {
            throw new IOException("Failed to create file: \"" + file.getAbsolutePath() + "\"");
        }
        return FileUtils.openOutputStream(file, true);
    }

    public static InputStream generateInputInputStream(String path) throws IOException {
        return new FileInputStream(path);
    }
}
