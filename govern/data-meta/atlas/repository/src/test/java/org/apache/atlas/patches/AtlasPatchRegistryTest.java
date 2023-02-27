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

package org.apache.atlas.patches;

import org.apache.atlas.TestModules;
import org.apache.atlas.model.patches.AtlasPatch;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.patches.AtlasPatchRegistry;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.apache.atlas.repository.store.bootstrap.AtlasTypeDefStoreInitializer.TYPEDEF_PATCH_TYPE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@Guice(modules = TestModules.TestOnlyModule.class)
public class AtlasPatchRegistryTest {
    @Inject
    private AtlasGraph graph;

    @Test
    public void noPatchesRegistered() {
        AtlasPatchRegistry registry = new AtlasPatchRegistry(graph);

        assertPatches(registry, 0);
    }

    @Test(dependsOnMethods = "noPatchesRegistered")
    public void registerPatch() {
        AtlasPatchRegistry registry = new AtlasPatchRegistry(graph);

        registry.register("1", "test patch", TYPEDEF_PATCH_TYPE, "apply", AtlasPatch.PatchStatus.UNKNOWN);

        assertPatches(registry, 1);
    }

    @Test(dependsOnMethods = "registerPatch")
    public void updateStatusForPatch() {
        final AtlasPatch.PatchStatus expectedStatus = AtlasPatch.PatchStatus.APPLIED;
        String                       patchId        = "1";

        AtlasPatchRegistry registry = new AtlasPatchRegistry(graph);

        registry.updateStatus(patchId, expectedStatus);

        AtlasPatch.AtlasPatches patches = assertPatches(registry, 1);

        assertEquals(patches.getPatches().get(0).getId(), patchId);
        assertEquals(patches.getPatches().get(0).getStatus(), expectedStatus);
    }


    private AtlasPatch.AtlasPatches assertPatches(AtlasPatchRegistry registry, int i) {
        AtlasPatch.AtlasPatches patches = registry.getAllPatches();

        assertNotNull(patches);
        assertEquals(patches.getPatches().size(), i);

        return patches;
    }
}
