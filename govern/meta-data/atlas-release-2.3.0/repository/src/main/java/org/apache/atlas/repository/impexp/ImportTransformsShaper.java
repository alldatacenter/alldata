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

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasExportRequest;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ImportTransformsShaper {
    private static final Logger LOG = LoggerFactory.getLogger(ImportTransformsShaper.class);

    private final AtlasTypeRegistry typeRegistry;
    private final AtlasTypeDefStore typeDefStore;

    @Inject
    public ImportTransformsShaper(AtlasTypeRegistry typeRegistry, AtlasTypeDefStore typeDefStore) {
        this.typeRegistry = typeRegistry;
        this.typeDefStore = typeDefStore;
    }

    public void shape(ImportTransforms importTransform, AtlasExportRequest request) throws AtlasBaseException {
        getCreateClassifications(importTransform, request);
        updateTransformsWithSubTypes(importTransform);
    }

    private void getCreateClassifications(ImportTransforms importTransform, AtlasExportRequest request) throws AtlasBaseException {
        Map<String, Map<String, List<ImportTransformer>>> mapMapList = importTransform.getTransforms();
        for (Map<String, List<ImportTransformer>> mapList : mapMapList.values()) {
            for (List<ImportTransformer> list : mapList.values()) {
                for (ImportTransformer importTransformer : list) {
                    if((importTransformer instanceof ImportTransformer.AddClassification)) {

                        ImportTransformer.AddClassification addClassification = (ImportTransformer.AddClassification) importTransformer;
                        addFilters(request, addClassification);
                        getCreateTag(addClassification.getClassificationName());
                    }
                }
            }
        }
    }

    private void addFilters(AtlasExportRequest request, ImportTransformer.AddClassification transformer) {
        for(AtlasObjectId objectId : request.getItemsToExport()) {
            transformer.addFilter(objectId);
        }
    }

    private void updateTransformsWithSubTypes(ImportTransforms importTransforms) {
        String[] transformTypes = importTransforms.getTypes().toArray(new String[importTransforms.getTypes().size()]);
        for (int i = 0; i < transformTypes.length; i++) {
            String typeName = transformTypes[i];
            AtlasEntityType entityType = typeRegistry.getEntityTypeByName(typeName);
            if (entityType == null) {
                continue;
            }

            importTransforms.addParentTransformsToSubTypes(typeName, entityType.getAllSubTypes());
        }
    }

    private String getCreateTag(String classificationName) throws AtlasBaseException {
        AtlasClassificationDef classificationDef = typeRegistry.getClassificationDefByName(classificationName);
        if(classificationDef != null) {
            return classificationName;
        }

        classificationDef = new AtlasClassificationDef(classificationName);
        AtlasTypesDef typesDef = new AtlasTypesDef();
        typesDef.setClassificationDefs(Collections.singletonList(classificationDef));
        typeDefStore.createTypesDef(typesDef);
        LOG.info("created classification: {}", classificationName);
        return classificationName;
    }
}
