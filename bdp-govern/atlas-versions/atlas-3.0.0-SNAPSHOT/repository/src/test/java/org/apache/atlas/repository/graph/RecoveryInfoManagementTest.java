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
package org.apache.atlas.repository.graph;

import com.google.inject.Inject;
import org.apache.atlas.RequestContext;
import org.apache.atlas.TestModules;
import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

@Guice(modules = TestModules.TestOnlyModule.class)
public class RecoveryInfoManagementTest extends AtlasTestBase {

    @Inject
    private AtlasGraph atlasGraph;


    @BeforeTest
    public void setupTest() {
        RequestContext.clear();
        RequestContext.get().setUser(TestUtilsV2.TEST_USER, null);
    }

    @BeforeClass
    public void initialize() throws Exception {
        super.initialize();
    }

    @AfterClass
    public void cleanup() throws Exception {
        super.cleanup();
    }

    @Test
    public void verifyCreateUpdate() {
        IndexRecoveryService.RecoveryInfoManagement rm = new IndexRecoveryService.RecoveryInfoManagement(atlasGraph);
        long now = System.currentTimeMillis();
        rm.updateStartTime(now);

        long storedTime = rm.getStartTime();
        Assert.assertEquals(now, storedTime);
    }
}
