/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.tagsync.process;

import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.tagsync.source.atlas.AtlasAdlsResourceMapper;
import org.apache.ranger.tagsync.source.atlasrest.RangerAtlasEntity;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

import static org.apache.ranger.tagsync.source.atlas.AtlasAdlsResourceMapper.*;
import static org.apache.ranger.tagsync.source.atlas.AtlasResourceMapper.ENTITY_ATTRIBUTE_QUALIFIED_NAME;


public class TestAdlsResourceMapper {
    private static final String ACCOUNT_QUALIFIED_NAME        = "abfs://myaccount@cl1";
    private static final String CONTAINER_QUALIFIED_NAME      = "abfs://mycontainer@myaccount@cl1";
    private static final String RELATIVE_PATH_QUALIFIED_NAME  = "abfs://mycontainer@myaccount/tmp@cl1";
    private static final String CONTAINER2_QUALIFIED_NAME     = "abfs://mycontainer@myaccount.dfs.core.windows.net@cl1";
    private static final String RELATIVE_PATH2_QUALIFIED_NAME = "abfs://mycontainer@myaccount.dfs.core.windows.net/tmp@cl1";

    private static final String SERVICE_NAME                 = "cl1_adls";
    private static final String ACCOUNT_NAME                 = "myaccount";
    private static final String ACCOUNT2_NAME                = "myaccount.dfs.core.windows.net";
    private static final String CONTAINER_NAME               = "mycontainer";
    private static final String RELATIVE_PATH_NAME           = "/tmp";

    AtlasAdlsResourceMapper resourceMapper = new AtlasAdlsResourceMapper();

    @Test
    public void testAccountEntity() throws Exception {
        RangerAtlasEntity     entity   = getEntity(ENTITY_TYPE_ADLS_GEN2_ACCOUNT, ACCOUNT_QUALIFIED_NAME);
        RangerServiceResource resource = resourceMapper.buildResource(entity);

        Assert.assertEquals(SERVICE_NAME, resource.getServiceName());
        assertResourceElementCount(resource, 1);
        assertResourceElementValue(resource, RANGER_TYPE_ADLS_GEN2_ACCOUNT, ACCOUNT_NAME);
    }

    @Test
    public void testContainerEntity() throws Exception {
        RangerAtlasEntity     entity   = getEntity(ENTITY_TYPE_ADLS_GEN2_CONTAINER, CONTAINER_QUALIFIED_NAME);
        RangerServiceResource resource = resourceMapper.buildResource(entity);

        Assert.assertEquals(SERVICE_NAME, resource.getServiceName());
        assertResourceElementCount(resource, 2);
        assertResourceElementValue(resource, RANGER_TYPE_ADLS_GEN2_ACCOUNT, ACCOUNT_NAME);
        assertResourceElementValue(resource, RANGER_TYPE_ADLS_GEN2_CONTAINER, CONTAINER_NAME);
    }

    @Test
    public void testDirectoryEntity() throws Exception {
        RangerAtlasEntity     entity   = getEntity(ENTITY_TYPE_ADLS_GEN2_DIRECTORY, RELATIVE_PATH_QUALIFIED_NAME);
        RangerServiceResource resource = resourceMapper.buildResource(entity);

        Assert.assertEquals(SERVICE_NAME, resource.getServiceName());
        assertResourceElementCount(resource, 3);
        assertResourceElementValue(resource, RANGER_TYPE_ADLS_GEN2_ACCOUNT, ACCOUNT_NAME);
        assertResourceElementValue(resource, RANGER_TYPE_ADLS_GEN2_CONTAINER, CONTAINER_NAME);
        assertResourceElementValue(resource, RANGER_TYPE_ADLS_GEN2_RELATIVE_PATH, RELATIVE_PATH_NAME);
    }

    @Test
    public void testContainer2Entity() throws Exception {
        RangerAtlasEntity     entity   = getEntity(ENTITY_TYPE_ADLS_GEN2_CONTAINER, CONTAINER2_QUALIFIED_NAME);
        RangerServiceResource resource = resourceMapper.buildResource(entity);

        Assert.assertEquals(SERVICE_NAME, resource.getServiceName());
        assertResourceElementCount(resource, 2);
        assertResourceElementValue(resource, RANGER_TYPE_ADLS_GEN2_ACCOUNT, ACCOUNT2_NAME);
        assertResourceElementValue(resource, RANGER_TYPE_ADLS_GEN2_CONTAINER, CONTAINER_NAME);
    }

    @Test
    public void testDirectory2Entity() throws Exception {
        RangerAtlasEntity     entity   = getEntity(ENTITY_TYPE_ADLS_GEN2_DIRECTORY, RELATIVE_PATH2_QUALIFIED_NAME);
        RangerServiceResource resource = resourceMapper.buildResource(entity);

        Assert.assertEquals(SERVICE_NAME, resource.getServiceName());
        assertResourceElementCount(resource, 3);
        assertResourceElementValue(resource, RANGER_TYPE_ADLS_GEN2_ACCOUNT, ACCOUNT2_NAME);
        assertResourceElementValue(resource, RANGER_TYPE_ADLS_GEN2_CONTAINER, CONTAINER_NAME);
        assertResourceElementValue(resource, RANGER_TYPE_ADLS_GEN2_RELATIVE_PATH, RELATIVE_PATH_NAME);
    }

    @Test
    public void testInvalidEntityType() {
        assertException(getEntity("Unknown", RELATIVE_PATH_QUALIFIED_NAME), "unrecognized entity-type");
    }

    @Test
    public void testInvalidAccountEntity() {
        assertException(getEntity(ENTITY_TYPE_ADLS_GEN2_ACCOUNT, null), "attribute 'qualifiedName' not found");
        assertException(getEntity(ENTITY_TYPE_ADLS_GEN2_ACCOUNT, ""), "attribute 'qualifiedName' not found");
        assertException(getEntity(ENTITY_TYPE_ADLS_GEN2_ACCOUNT, "test"), "cluster-name not found");
        assertException(getEntity(ENTITY_TYPE_ADLS_GEN2_ACCOUNT, "test@cl1"), "account-name not found");
    }

    @Test
    public void testInvalidContainerEntity() {
        assertException(getEntity(ENTITY_TYPE_ADLS_GEN2_CONTAINER, null), "attribute 'qualifiedName' not found");
        assertException(getEntity(ENTITY_TYPE_ADLS_GEN2_CONTAINER, ""), "attribute 'qualifiedName' not found");
        assertException(getEntity(ENTITY_TYPE_ADLS_GEN2_CONTAINER, "test"), "cluster-name not found");
        assertException(getEntity(ENTITY_TYPE_ADLS_GEN2_CONTAINER, "test@cl1"), "account-name not found");
        assertException(getEntity(ENTITY_TYPE_ADLS_GEN2_CONTAINER, "abfs://test@cl1"), "container-name not found");
    }

    @Test
    public void testInvalidDirectoryEntity() {
        assertException(getEntity(ENTITY_TYPE_ADLS_GEN2_DIRECTORY, null), "attribute 'qualifiedName' not found");
        assertException(getEntity(ENTITY_TYPE_ADLS_GEN2_DIRECTORY, ""), "attribute 'qualifiedName' not found");
        assertException(getEntity(ENTITY_TYPE_ADLS_GEN2_DIRECTORY, "test"), "cluster-name not found");
        assertException(getEntity(ENTITY_TYPE_ADLS_GEN2_DIRECTORY, "test@cl1"), "account-name not found");
        assertException(getEntity(ENTITY_TYPE_ADLS_GEN2_DIRECTORY, "abfs://test@cl1"), "container-name not found");
        assertException(getEntity(ENTITY_TYPE_ADLS_GEN2_DIRECTORY, "abfs://a@test.dfs.core.windows.net@cl1"), "relative-path not found");
    }

    private RangerAtlasEntity getEntity(String entityType, String qualifiedName) {
        return new RangerAtlasEntity(entityType, "guid-" + entityType, Collections.singletonMap(ENTITY_ATTRIBUTE_QUALIFIED_NAME, qualifiedName));
    }

    private void assertResourceElementCount(RangerServiceResource resource, int count) {
        Assert.assertNotNull(resource);
        Assert.assertNotNull(resource.getResourceElements());
        Assert.assertEquals(count, resource.getResourceElements().size());
    }

    private void assertResourceElementValue(RangerServiceResource resource, String resourceName, String value) {
        Assert.assertTrue(resource.getResourceElements().containsKey(resourceName));
        Assert.assertNotNull(resource.getResourceElements().get(resourceName).getValues());
        Assert.assertEquals(1, resource.getResourceElements().get(resourceName).getValues().size());
        Assert.assertEquals(value, resource.getResourceElements().get(resourceName).getValues().get(0));
    }

    private void assertException(RangerAtlasEntity entity, String exceptionMessage) {
        try {
            RangerServiceResource resource = resourceMapper.buildResource(entity);

            Assert.assertFalse("Expected buildResource() to fail. But it returned " + resource, true);
        } catch (Exception excp) {
            Assert.assertTrue("Unexpected exception message: expected=" + exceptionMessage + "; found " + excp.getMessage(),
                    excp.getMessage().startsWith(exceptionMessage));
        }
    }
}
