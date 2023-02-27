/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.notification.spool;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class BaseTest {
    public static String spoolDir = System.getProperty("user.dir") + "/src/test/resources/spool";
    public static String spoolDirTest = spoolDir + "-test";
    protected final String SOURCE_TEST = "test-src";
    protected final String SOURCE_TEST_HANDLER = "1";

    protected final String knownIndexFilePath = "index-test-src-1.json";
    protected final String knownIndexDoneFilePath = "index-test-src-1_closed.json";

    protected File archiveDir = new File(spoolDir, "archive");
    protected File indexFile = new File(spoolDir, knownIndexFilePath);
    protected File indexDoneFile = new File(spoolDir, knownIndexDoneFilePath);

    public SpoolConfiguration getSpoolConfiguration() {
        return getSpoolConfiguration(spoolDir, SOURCE_TEST_HANDLER);
    }

    public SpoolConfiguration getSpoolConfigurationTest() {
        return getSpoolConfiguration(spoolDirTest, SOURCE_TEST_HANDLER);
    }
    public SpoolConfiguration getSpoolConfigurationTest(Integer testId) {
        return getSpoolConfiguration(spoolDirTest, testId.toString());
    }

    public SpoolConfiguration getSpoolConfiguration(String spoolDir, String handlerName) {
        SpoolConfiguration cfg = new SpoolConfiguration(getConfiguration(spoolDir), handlerName);
        cfg.setSource(SOURCE_TEST, "testuser");
        return cfg;
    }

    public Configuration getConfiguration(String spoolDir) {
        final int destinationRetry = 2000;

        PropertiesConfiguration props = new PropertiesConfiguration();
        props.setProperty(SpoolConfiguration.PROP_FILE_SPOOL_LOCAL_DIR, spoolDir);
        props.setProperty(SpoolConfiguration.PROP_FILE_SPOOL_DEST_RETRY_MS, Integer.toString(destinationRetry));
        props.setProperty(SpoolConfiguration.PROP_FILE_SPOOL_FILE_ROLLOVER_SEC, Integer.toString(2));
        props.setProperty(SpoolConfiguration.PROP_FILE_SPOOL_PAUSE_BEFORE_SEND_SEC, 0);
        return props;
    }

    protected File getNewIndexFile(char id) throws IOException {
        File f = new File(spoolDirTest, knownIndexFilePath.replace('1', id));
        FileUtils.copyFile(indexFile, f);
        return f;
    }

    protected File getNewIndexDoneFile(char id) throws IOException {
        File f = new File(spoolDirTest, knownIndexDoneFilePath.replace('1', id));
        FileUtils.copyFile(indexDoneFile, f);
        return f;
    }
}
