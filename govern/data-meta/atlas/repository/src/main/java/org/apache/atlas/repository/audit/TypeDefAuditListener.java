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
package org.apache.atlas.repository.audit;

import org.apache.atlas.RequestContext;
import org.apache.atlas.authorize.AtlasAuthorizationUtils;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.listener.ChangedTypeDefs;
import org.apache.atlas.listener.TypeDefChangeListener;
import org.apache.atlas.model.TypeCategory;
import org.apache.atlas.model.audit.AtlasAuditEntry;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.utils.AtlasJson;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Component
@Order(2)
public class TypeDefAuditListener implements TypeDefChangeListener {

    AtlasAuditService auditService;

    @Inject
    TypeDefAuditListener(AtlasAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void onChange(ChangedTypeDefs changedTypeDefs) throws AtlasBaseException {
        createAuditEntry(changedTypeDefs);
    }

    @Override
    public void onLoadCompletion() throws AtlasBaseException {
    }

    private void createAuditEntry(ChangedTypeDefs changedTypeDefs) throws AtlasBaseException {
        List<AtlasBaseTypeDef> createdTypes = (List<AtlasBaseTypeDef>) changedTypeDefs.getCreatedTypeDefs();
        List<AtlasBaseTypeDef> updatedTypes = (List<AtlasBaseTypeDef>) changedTypeDefs.getUpdatedTypeDefs();
        List<AtlasBaseTypeDef> deletedTypes = (List<AtlasBaseTypeDef>) changedTypeDefs.getDeletedTypeDefs();

        updatedTypes = removeDuplicateEntries(createdTypes, updatedTypes);

        createAuditEntry(createdTypes, AtlasAuditEntry.AuditOperation.TYPE_DEF_CREATE);
        createAuditEntry(updatedTypes, AtlasAuditEntry.AuditOperation.TYPE_DEF_UPDATE);
        createAuditEntry(deletedTypes, AtlasAuditEntry.AuditOperation.TYPE_DEF_DELETE);
    }

    private List<AtlasBaseTypeDef> removeDuplicateEntries(List<AtlasBaseTypeDef> createdTypes, List<AtlasBaseTypeDef> updatedTypes) {
        if (CollectionUtils.isNotEmpty(createdTypes)) {
            List<String> createdTypeNames = createdTypes.stream()
                    .map(obj -> obj.getName()).collect(Collectors.toList());
            updatedTypes.removeIf(obj -> createdTypeNames.contains(obj.getName()));
        }
        if (CollectionUtils.isNotEmpty(updatedTypes)) {
            Set<AtlasBaseTypeDef> baseTypeDefs = updatedTypes.stream()
                    .collect(Collectors.toCollection(() ->
                            new TreeSet<>(Comparator.comparing(AtlasBaseTypeDef::getName))));
            updatedTypes = new ArrayList<>(baseTypeDefs);
        }
        return updatedTypes;
    }

    private void createAuditEntry(List<AtlasBaseTypeDef> baseTypeDefList, AtlasAuditEntry.AuditOperation auditOperation) throws AtlasBaseException {
        if (CollectionUtils.isEmpty(baseTypeDefList)) {
            return;
        }

        Map<TypeCategory, List<AtlasBaseTypeDef>> groupByCategoryMap =
                baseTypeDefList.stream().collect(Collectors.groupingBy(AtlasBaseTypeDef::getCategory));

        List<String> categories = new ArrayList<>();
        for (TypeCategory category : groupByCategoryMap.keySet()) {
            categories.add(category.name());
        }

        String typeDefJson = AtlasJson.toJson(groupByCategoryMap);

        auditService.add(auditOperation, String.join(",", categories), typeDefJson, baseTypeDefList.size());
    }
}
