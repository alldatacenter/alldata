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
import org.apache.atlas.listener.ChangedTypeDefs;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.repository.graph.SolrIndexHelper;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.APPLIED;

/**
 * This patch handler installs free text request handlers for already running Atlas instances.
 * --instances that were deployed with older versions.
 */
public class FreeTextRequestHandlerPatch extends AtlasPatchHandler {
    private static final Logger LOG = LoggerFactory.getLogger(FreeTextRequestHandlerPatch.class);

    private static final String PATCH_ID          = "JAVA_PATCH_0000_003";
    private static final String PATCH_DESCRIPTION = "Creates Solr request handler for use in free-text searches";

    private final PatchContext context;

    public FreeTextRequestHandlerPatch(PatchContext context) {
        super(context.getPatchRegistry(), PATCH_ID, PATCH_DESCRIPTION);

        this.context = context;
    }

    @Override
    public void apply() throws AtlasBaseException {
        AtlasTypeRegistry          typeRegistry = context.getTypeRegistry();
        Collection<AtlasEntityDef> entityDefs   = typeRegistry.getAllEntityDefs();

        if (CollectionUtils.isNotEmpty(entityDefs)) {
            SolrIndexHelper indexHelper     = new SolrIndexHelper(typeRegistry);
            ChangedTypeDefs changedTypeDefs = new ChangedTypeDefs(null, new ArrayList<>(entityDefs), null);

            indexHelper.onChange(changedTypeDefs);
        }

        setStatus(APPLIED);

        LOG.info("FreeTextRequestHandlerPatch.apply(): patchId={}, status={}", getPatchId(), getStatus());
    }
}
