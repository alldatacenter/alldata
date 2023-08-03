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
import org.apache.ranger.tagsync.source.atlas.AtlasOzoneResourceMapper;
import org.apache.ranger.tagsync.source.atlasrest.RangerAtlasEntity;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

import static org.apache.ranger.tagsync.source.atlas.AtlasOzoneResourceMapper.*;
import static org.apache.ranger.tagsync.source.atlas.AtlasResourceMapper.ENTITY_ATTRIBUTE_QUALIFIED_NAME;


public class TestOzoneResourceMapper {
    private static final String VOLUME_QUALIFIED_NAME       = "o3fs://myvolume@cl1";
    private static final String BUCKET_QUALIFIED_NAME       = "o3fs://myvolume.mybucket@cl1";
    private static final String KEY_QUALIFIED_NAME          = "o3fs://mybucket.myvolume.ozone1/mykey.txt@cl1" ;
    private static final String KEY_PATH_QUALIFIED_NAME     = "o3fs://mybucket.myvolume.ozone1/mykey/key1/@cl1";

    private static final String SERVICE_NAME                = "cl1_ozone";
    private static final String VOLUME_NAME                 = "myvolume";
    private static final String BUCKET_NAME                 = "mybucket";
    private static final String KEY_NAME                    = "/mykey.txt";
    private static final String KEY_PATH                    = "/mykey/key1/";

    AtlasOzoneResourceMapper resourceMapper = new AtlasOzoneResourceMapper();

    @Test
    public void testVolumeEntity() throws Exception {
        RangerAtlasEntity     entity   = getEntity(ENTITY_TYPE_OZONE_VOLUME, VOLUME_QUALIFIED_NAME);
        RangerServiceResource resource = resourceMapper.buildResource(entity);

        Assert.assertEquals(SERVICE_NAME, resource.getServiceName());
        assertResourceElementCount(resource, 1);
        assertResourceElementValue(resource, RANGER_TYPE_OZONE_VOLUME, VOLUME_NAME);
    }

    @Test
    public void testBucketEntity() throws Exception {
        RangerAtlasEntity     entity   = getEntity(ENTITY_TYPE_OZONE_BUCKET, BUCKET_QUALIFIED_NAME);
        RangerServiceResource resource = resourceMapper.buildResource(entity);

        Assert.assertEquals(SERVICE_NAME, resource.getServiceName());
        assertResourceElementCount(resource, 2);
        assertResourceElementValue(resource, RANGER_TYPE_OZONE_VOLUME, VOLUME_NAME);
        assertResourceElementValue(resource, RANGER_TYPE_OZONE_BUCKET, BUCKET_NAME);
    }

    @Test
    public void testKeyEntity() throws Exception {
        RangerAtlasEntity     entity   = getEntity(ENTITY_TYPE_OZONE_KEY, KEY_QUALIFIED_NAME);
        RangerServiceResource resource = resourceMapper.buildResource(entity);

        Assert.assertEquals(SERVICE_NAME, resource.getServiceName());
        assertResourceElementCount(resource, 3);
        assertResourceElementValue(resource, RANGER_TYPE_OZONE_VOLUME, VOLUME_NAME);
        assertResourceElementValue(resource, RANGER_TYPE_OZONE_BUCKET, BUCKET_NAME);
        assertResourceElementValue(resource, RANGER_TYPE_OZONE_KEY, KEY_NAME);
    }

    @Test
    public void testKey2Entity() throws Exception {
        RangerAtlasEntity     entity   = getEntity(ENTITY_TYPE_OZONE_KEY, KEY_PATH_QUALIFIED_NAME);
        RangerServiceResource resource = resourceMapper.buildResource(entity);

        Assert.assertEquals(SERVICE_NAME, resource.getServiceName());
        assertResourceElementCount(resource, 3);
        assertResourceElementValue(resource, RANGER_TYPE_OZONE_VOLUME, VOLUME_NAME);
        assertResourceElementValue(resource, RANGER_TYPE_OZONE_BUCKET, BUCKET_NAME);
        assertResourceElementValue(resource, RANGER_TYPE_OZONE_KEY, KEY_PATH);
    }

    @Test
    public void testInvalidEntityType() {
        assertException(getEntity("Unknown", KEY_PATH_QUALIFIED_NAME), "unrecognized entity-type");
    }

    @Test
    public void testInvalidVolumeEntity() {
        assertException(getEntity(ENTITY_TYPE_OZONE_VOLUME, null), "attribute 'qualifiedName' not found");
        assertException(getEntity(ENTITY_TYPE_OZONE_VOLUME, ""), "attribute 'qualifiedName' not found");
        assertException(getEntity(ENTITY_TYPE_OZONE_VOLUME, "abfs://test"), "cluster-name not found");
        assertException(getEntity(ENTITY_TYPE_OZONE_VOLUME, "abfs://@cl1"), "volume-name not found");
    }

    @Test
    public void testInvalidBucketEntity() {
        assertException(getEntity(ENTITY_TYPE_OZONE_BUCKET, null), "attribute 'qualifiedName' not found");
        assertException(getEntity(ENTITY_TYPE_OZONE_BUCKET, ""), "attribute 'qualifiedName' not found");
        assertException(getEntity(ENTITY_TYPE_OZONE_BUCKET, "abfs://test"), "cluster-name not found");
        assertException(getEntity(ENTITY_TYPE_OZONE_BUCKET, "abfs://.test@cl1"), "volume-name not found");
        assertException(getEntity(ENTITY_TYPE_OZONE_BUCKET, "abfs://test@cl1"), "bucket-name not found");
        assertException(getEntity(ENTITY_TYPE_OZONE_BUCKET, "abfs://test.@cl1"), "bucket-name not found");
    }

    @Test
    public void testInvalidKeyEntity() {
        assertException(getEntity(ENTITY_TYPE_OZONE_KEY, null), "attribute 'qualifiedName' not found");
        assertException(getEntity(ENTITY_TYPE_OZONE_KEY, ""), "attribute 'qualifiedName' not found");
        assertException(getEntity(ENTITY_TYPE_OZONE_KEY, "abfs://test"), "cluster-name not found");
        assertException(getEntity(ENTITY_TYPE_OZONE_KEY, "abfs://.test@cl1"), "bucket-name not found");
        assertException(getEntity(ENTITY_TYPE_OZONE_KEY, "abfs://test@cl1"), "volume-name not found");
        assertException(getEntity(ENTITY_TYPE_OZONE_KEY, "abfs://test.@cl1"), "volume-name not found");
        assertException(getEntity(ENTITY_TYPE_OZONE_KEY, "abfs://buck.vol.ozone@cl1"), "key-name not found");
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
