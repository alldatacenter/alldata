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
package org.apache.atlas.web.resources;

import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.instance.EntityMutations;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.commons.collections.CollectionUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 *   Unit test of {@link EntityResource}
 */
public class EntityResourceTest {

    private static final String DELETED_GUID = "deleted_guid";


    @Mock
    AtlasEntityStore entitiesStore;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDeleteEntitiesDoesNotLookupDeletedEntity() throws Exception {
        List<String> guids = Collections.singletonList(DELETED_GUID);
        List<AtlasEntityHeader> deletedEntities = Collections.singletonList(new AtlasEntityHeader(null, DELETED_GUID, null));

        // Create EntityResult with a deleted guid and no other guids.
        EntityMutationResponse  resp    = new EntityMutationResponse();
        List<AtlasEntityHeader> headers = toAtlasEntityHeaders(guids);

        if (CollectionUtils.isNotEmpty(headers)) {
            for (AtlasEntityHeader entity : headers) {
                resp.addEntity(EntityMutations.EntityOperation.DELETE, entity);
            }
        }

        when(entitiesStore.deleteByIds(guids)).thenReturn(resp);

        EntityMutationResponse response = entitiesStore.deleteByIds(guids);

        List<AtlasEntityHeader> responseDeletedEntities = response.getDeletedEntities();

        Assert.assertEquals(responseDeletedEntities, deletedEntities);
    }

    private List<AtlasEntityHeader> toAtlasEntityHeaders(List<String> guids) {
        List<AtlasEntityHeader> ret = null;

        if (CollectionUtils.isNotEmpty(guids)) {
            ret = new ArrayList<>(guids.size());
            for (String guid : guids) {
                ret.add(new AtlasEntityHeader(null, guid, null));
            }
        }

        return ret;
    }
}
