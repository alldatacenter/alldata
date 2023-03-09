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
package org.apache.griffin.core.job;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.griffin.core.job.entity.SegmentPredicate;
import org.apache.tools.ant.util.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileExistPredicatorTest {

    static String fileName = "_SUCCESS";
    static String rootPath = "/tmp/";

    @BeforeClass
    public static void mkFile() throws IOException {
        File fileDirectory = new File(rootPath); // to fix createFileExclusively exception
        File file = new File(rootPath + fileName);
        if (!fileDirectory.exists()) {
            fileDirectory.mkdir();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    @AfterClass
    public static void deleteFile() {
        File file = new File(rootPath + fileName);
        if (file.exists()) {
            FileUtils.delete(file);
        }
    }

    @Test(expected = NullPointerException.class)
    public void test_predicate_null() throws IOException {
        SegmentPredicate predicate = new SegmentPredicate();
        predicate.setConfig("test config");

        Map<String, Object> configMap = new HashMap<>();
        predicate.setConfigMap(configMap);

        FileExistPredicator predicator = new FileExistPredicator(predicate);
        assertTrue(predicator.predicate());
    }

    @Test
    public void test_predicate() throws IOException {
        SegmentPredicate predicate = new SegmentPredicate();
        predicate.setConfig("test config");

        Map<String, Object> configMap = new HashMap<>();
        configMap.put("path", fileName);
        configMap.put("root.path", rootPath);

        predicate.setConfigMap(configMap);

        FileExistPredicator predicator = new FileExistPredicator(predicate);
        assertTrue(predicator.predicate());

        configMap.put("path", "fileName");
        predicate.setConfigMap(configMap);
        assertFalse(predicator.predicate());

    }
}
