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
package org.apache.atlas.repository.impexp;

import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;

import static org.apache.atlas.repository.impexp.HdfsPathEntityCreator.HDFS_PATH_ATTRIBUTE_NAME_CLUSTER_NAME;
import static org.apache.atlas.repository.impexp.HdfsPathEntityCreator.HDFS_PATH_ATTRIBUTE_NAME_NAME;
import static org.apache.atlas.repository.impexp.HdfsPathEntityCreator.HDFS_PATH_ATTRIBUTE_QUALIFIED_NAME;
import static org.apache.atlas.repository.impexp.HdfsPathEntityCreator.getQualifiedName;
import static org.apache.atlas.utils.TestLoadModelUtils.loadFsModel;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@Guice(modules = TestModules.TestOnlyModule.class)
public class HdfsPathEntityCreatorTest extends AtlasTestBase {

    @Inject
    AtlasTypeRegistry typeRegistry;

    @Inject
    private AtlasTypeDefStore typeDefStore;

    @Inject
    private HdfsPathEntityCreator hdfsPathEntityCreator;

    private static final String expectedPath = "hdfs://server-name/warehouse/hr";
    private static final String expectedClusterName = "cl1";

    @BeforeClass
    public void setup() throws IOException, AtlasBaseException {
        basicSetup(typeDefStore, typeRegistry);
        loadFsModel(typeDefStore, typeRegistry);
    }

    @Test
    public void verifyCreate() throws AtlasBaseException {

        String expectedQualifiedName = getQualifiedName(expectedPath + "/", expectedClusterName);
        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = hdfsPathEntityCreator.getCreateEntity(expectedPath, expectedClusterName);

        assertNotNull(entityWithExtInfo);
        AtlasEntity entity = entityWithExtInfo.getEntity();
        assertEquals(entity.getAttribute(HdfsPathEntityCreator.HDFS_PATH_ATTRIBUTE_NAME_PATH), expectedPath + "/");
        assertEquals(entity.getAttribute(HDFS_PATH_ATTRIBUTE_QUALIFIED_NAME),expectedQualifiedName);
        assertEquals(entity.getAttribute(HDFS_PATH_ATTRIBUTE_NAME_NAME), expectedPath + "/");
        assertEquals(entity.getAttribute(HDFS_PATH_ATTRIBUTE_NAME_CLUSTER_NAME), expectedClusterName);
    }

    @Test(dependsOnMethods = "verifyCreate")
    public void verifyGet() throws AtlasBaseException {
        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = hdfsPathEntityCreator.getCreateEntity(expectedPath, expectedClusterName);

        assertNotNull(entityWithExtInfo);
    }
}
