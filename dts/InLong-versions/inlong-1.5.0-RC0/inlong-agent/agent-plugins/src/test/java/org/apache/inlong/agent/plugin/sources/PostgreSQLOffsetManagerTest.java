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

package org.apache.inlong.agent.plugin.sources;

import org.apache.commons.codec.binary.Base64;
import org.apache.inlong.agent.plugin.AgentBaseTestsHelper;
import org.apache.inlong.agent.plugin.sources.snapshot.PostgreSQLSnapshotBase;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test for PostgreSQL snapshot
 */
public class PostgreSQLOffsetManagerTest {

    private static AgentBaseTestsHelper helper;

    private static final String fileName = "testPostgreSQL.txt";

    private static Path filePath;

    @BeforeClass
    public static void setup() {
        helper = new AgentBaseTestsHelper(PostgreSQLOffsetManagerTest.class.getName()).setupAgentHome();
        Path testDir = helper.getTestRootDir();
        filePath = Paths.get(testDir.toString(), fileName);
    }

    @AfterClass
    public static void teardown() {
        helper.teardownAgentHome();
    }

    @Test
    public void testOffset() {
        PostgreSQLSnapshotBase snapshotManager = new PostgreSQLSnapshotBase(filePath.toString());
        byte[] snapshotBytes = new byte[]{-65, -14, 23};
        final Base64 base64 = new Base64();
        String encodeSnapshot = base64.encodeAsString(snapshotBytes);
        snapshotManager.save(encodeSnapshot, snapshotManager.getFile());
        Assert.assertEquals(snapshotManager.getSnapshot(), encodeSnapshot);
        File file = new File(filePath.toString());
        Assert.assertEquals(file.exists(), true);
        System.out.println(file.getAbsolutePath());
    }
}
