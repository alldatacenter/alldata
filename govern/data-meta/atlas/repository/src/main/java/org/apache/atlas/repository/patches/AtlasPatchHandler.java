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
package org.apache.atlas.repository.patches;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.patches.AtlasPatch.PatchStatus;

import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.UNKNOWN;

public abstract class AtlasPatchHandler {
    public static final String JAVA_PATCH_TYPE = "JAVA_PATCH";

    private final String             patchId;
    private final String             patchDescription;
    private final AtlasPatchRegistry patchRegistry;
    private       PatchStatus        status;

    public AtlasPatchHandler(AtlasPatchRegistry patchRegistry, String patchId, String patchDescription) {
        this.patchId          = patchId;
        this.patchDescription = patchDescription;
        this.patchRegistry    = patchRegistry;
        this.status           = getStatusFromRegistry();

        register();
    }

    private void register() {
        PatchStatus patchStatus = getStatus();

        if (patchStatus == null || patchStatus == UNKNOWN) {
            patchRegistry.register(patchId, patchDescription, JAVA_PATCH_TYPE, "apply", UNKNOWN);
        }
    }

    public PatchStatus getStatusFromRegistry() {
        return patchRegistry.getStatus(patchId);
    }

    public PatchStatus getStatus() {
        return status;
    }

    public void setStatus(PatchStatus status) {
        this.status = status;

        patchRegistry.updateStatus(patchId, status);
    }

    public String getPatchId() {
        return patchId;
    }

    public abstract void apply() throws AtlasBaseException;
}
