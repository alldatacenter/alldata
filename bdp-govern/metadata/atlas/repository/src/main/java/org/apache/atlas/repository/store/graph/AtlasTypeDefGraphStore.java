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
package org.apache.atlas.repository.store.graph;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.GraphTransactionInterceptor;
import org.apache.atlas.annotation.GraphTransaction;
import org.apache.atlas.authorize.AtlasAuthorizationUtils;
import org.apache.atlas.authorize.AtlasPrivilege;
import org.apache.atlas.authorize.AtlasTypeAccessRequest;
import org.apache.atlas.authorize.AtlasTypesDefFilterRequest;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.listener.ChangedTypeDefs;
import org.apache.atlas.listener.TypeDefChangeListener;
import org.apache.atlas.model.SearchFilter;
import org.apache.atlas.model.typedef.*;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasConstraintDef;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.util.FilterUtil;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.*;
import org.apache.atlas.type.AtlasTypeRegistry.AtlasTransientTypeRegistry;
import org.apache.atlas.util.AtlasRepositoryConfiguration;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.atlas.model.discovery.SearchParameters.ALL_ENTITY_TYPES;
import static org.apache.atlas.model.discovery.SearchParameters.ALL_CLASSIFICATION_TYPES;
import static org.apache.atlas.repository.store.bootstrap.AtlasTypeDefStoreInitializer.getTypesToCreate;
import static org.apache.atlas.repository.store.bootstrap.AtlasTypeDefStoreInitializer.getTypesToUpdate;


/**
 * Abstract class for graph persistence store for TypeDef
 */
public abstract class AtlasTypeDefGraphStore implements AtlasTypeDefStore {

    private static final Logger LOG = LoggerFactory.getLogger(AtlasTypeDefGraphStore.class);

    private final AtlasTypeRegistry          typeRegistry;
    private final Set<TypeDefChangeListener> typeDefChangeListeners;
    private final int                        typeUpdateLockMaxWaitTimeSeconds;

    protected AtlasTypeDefGraphStore(AtlasTypeRegistry typeRegistry,
                                     Set<TypeDefChangeListener> typeDefChangeListeners) {
        this.typeRegistry                     = typeRegistry;
        this.typeDefChangeListeners           = typeDefChangeListeners;
        this.typeUpdateLockMaxWaitTimeSeconds = AtlasRepositoryConfiguration.getTypeUpdateLockMaxWaitTimeInSeconds();
    }

    protected abstract AtlasDefStore<AtlasEnumDef> getEnumDefStore(AtlasTypeRegistry typeRegistry);

    protected abstract AtlasDefStore<AtlasStructDef> getStructDefStore(AtlasTypeRegistry typeRegistry);

    protected abstract AtlasDefStore<AtlasClassificationDef> getClassificationDefStore(AtlasTypeRegistry typeRegistry);

    protected abstract AtlasDefStore<AtlasEntityDef> getEntityDefStore(AtlasTypeRegistry typeRegistry);

    protected abstract AtlasDefStore<AtlasRelationshipDef> getRelationshipDefStore(AtlasTypeRegistry typeRegistry);

    protected abstract AtlasDefStore<AtlasBusinessMetadataDef> getBusinessMetadataDefStore(AtlasTypeRegistry typeRegistry);

    public AtlasTypeRegistry getTypeRegistry() { return typeRegistry; }

    @Override
    public void init() throws AtlasBaseException {
        LOG.info("==> AtlasTypeDefGraphStore.init()");

        AtlasTransientTypeRegistry ttr           = null;
        boolean                    commitUpdates = false;

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate(typeUpdateLockMaxWaitTimeSeconds);

            ttr.clear();

            AtlasTypesDef typesDef = new AtlasTypesDef(getEnumDefStore(ttr).getAll(),
                    getStructDefStore(ttr).getAll(),
                    getClassificationDefStore(ttr).getAll(),
                    getEntityDefStore(ttr).getAll(),
                    getRelationshipDefStore(ttr).getAll(),
                    getBusinessMetadataDefStore(ttr).getAll());

            rectifyTypeErrorsIfAny(typesDef);

            ttr.addTypes(typesDef);

            commitUpdates = true;
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commitUpdates);

            LOG.info("<== AtlasTypeDefGraphStore.init()");
        }
    }

    @Override
    public AtlasEnumDef getEnumDefByName(String name) throws AtlasBaseException {
        AtlasEnumDef ret = typeRegistry.getEnumDefByName(name);
        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, name);
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_READ, ret), "read type ", name);

        return ret;
    }

    @Override
    public AtlasEnumDef getEnumDefByGuid(String guid) throws AtlasBaseException {
        AtlasEnumDef ret = typeRegistry.getEnumDefByGuid(guid);
        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_GUID_NOT_FOUND, guid);
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_READ, ret), "read type ", guid);

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasEnumDef updateEnumDefByName(String name, AtlasEnumDef enumDef) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = lockTypeRegistryAndReleasePostCommit();

        tryUpdateByName(name, enumDef, ttr);

        return getEnumDefStore(ttr).updateByName(name, enumDef);
    }

    @Override
    @GraphTransaction
    public AtlasEnumDef updateEnumDefByGuid(String guid, AtlasEnumDef enumDef) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = lockTypeRegistryAndReleasePostCommit();

        tryUpdateByGUID(guid, enumDef, ttr);

        return getEnumDefStore(ttr).updateByGuid(guid, enumDef);
    }

    @Override
    public AtlasStructDef getStructDefByName(String name) throws AtlasBaseException {
        AtlasStructDef ret = typeRegistry.getStructDefByName(name);

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, name);
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_READ, ret), "read type ", name);

        return ret;
    }

    @Override
    public AtlasStructDef getStructDefByGuid(String guid) throws AtlasBaseException {
        AtlasStructDef ret = typeRegistry.getStructDefByGuid(guid);

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_GUID_NOT_FOUND, guid);
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_READ, ret), "read type ", guid);

        return ret;
    }

    @Override
    public AtlasRelationshipDef getRelationshipDefByName(String name) throws AtlasBaseException {
        AtlasRelationshipDef ret = typeRegistry.getRelationshipDefByName(name);

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, name);
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_READ, ret), "read type ", name);

        return ret;
    }

    @Override
    public AtlasRelationshipDef getRelationshipDefByGuid(String guid) throws AtlasBaseException {
        AtlasRelationshipDef ret = typeRegistry.getRelationshipDefByGuid(guid);

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_GUID_NOT_FOUND, guid);
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_READ, ret), "read type ", guid);

        return ret;
    }

    @Override
    public AtlasBusinessMetadataDef getBusinessMetadataDefByName(String name) throws AtlasBaseException {
        AtlasBusinessMetadataDef ret = typeRegistry.getBusinessMetadataDefByName(name);

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, name);
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_READ, ret), "read type ", name);

        return ret;
    }

    @Override
    public AtlasBusinessMetadataDef getBusinessMetadataDefByGuid(String guid) throws AtlasBaseException {
        AtlasBusinessMetadataDef ret = typeRegistry.getBusinessMetadataDefByGuid(guid);

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_GUID_NOT_FOUND, guid);
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_READ, ret), "read type ", guid);

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasStructDef updateStructDefByName(String name, AtlasStructDef structDef) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = lockTypeRegistryAndReleasePostCommit();

        tryUpdateByName(name, structDef, ttr);

        return getStructDefStore(ttr).updateByName(name, structDef);
    }

    @Override
    @GraphTransaction
    public AtlasStructDef updateStructDefByGuid(String guid, AtlasStructDef structDef) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = lockTypeRegistryAndReleasePostCommit();

        tryUpdateByGUID(guid, structDef, ttr);

        return getStructDefStore(ttr).updateByGuid(guid, structDef);
    }

    @Override
    public AtlasClassificationDef getClassificationDefByName(String name) throws AtlasBaseException {
        AtlasClassificationDef ret = typeRegistry.getClassificationDefByName(name);

        if (ret == null) {
            ret = StringUtils.equalsIgnoreCase(name, ALL_CLASSIFICATION_TYPES) ? AtlasClassificationType.getClassificationRoot().getClassificationDef() : null;

            if (ret == null) {
                throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, name);
            }
            return ret;
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_READ, ret), "read type ", name);

        return ret;
    }

    @Override
    public AtlasClassificationDef getClassificationDefByGuid(String guid) throws AtlasBaseException {
        AtlasClassificationDef ret = typeRegistry.getClassificationDefByGuid(guid);

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_GUID_NOT_FOUND, guid);
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_READ, ret), "read type ", guid);

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasClassificationDef updateClassificationDefByName(String name, AtlasClassificationDef classificationDef)
            throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = lockTypeRegistryAndReleasePostCommit();

        tryUpdateByName(name, classificationDef, ttr);

        return getClassificationDefStore(ttr).updateByName(name, classificationDef);
    }

    @Override
    @GraphTransaction
    public AtlasClassificationDef updateClassificationDefByGuid(String guid, AtlasClassificationDef classificationDef)
            throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = lockTypeRegistryAndReleasePostCommit();

        tryUpdateByGUID(guid, classificationDef, ttr);

        return getClassificationDefStore(ttr).updateByGuid(guid, classificationDef);
    }

    @Override
    public AtlasEntityDef getEntityDefByName(String name) throws AtlasBaseException {
        AtlasEntityDef ret = typeRegistry.getEntityDefByName(name);

        if (ret == null) {
            ret = StringUtils.equals(name, ALL_ENTITY_TYPES) ? AtlasEntityType.getEntityRoot().getEntityDef() : null;

            if (ret == null) {
                throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, name);
            }
            return ret;
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_READ, ret), "read type ", name);

        return ret;
    }

    @Override
    public AtlasEntityDef getEntityDefByGuid(String guid) throws AtlasBaseException {
        AtlasEntityDef ret = typeRegistry.getEntityDefByGuid(guid);

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_GUID_NOT_FOUND, guid);
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_READ, ret), "read type ", guid);

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasEntityDef updateEntityDefByName(String name, AtlasEntityDef entityDef) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = lockTypeRegistryAndReleasePostCommit();

        tryUpdateByName(name, entityDef, ttr);

        return getEntityDefStore(ttr).updateByName(name, entityDef);
    }

    @Override
    @GraphTransaction
    public AtlasEntityDef updateEntityDefByGuid(String guid, AtlasEntityDef entityDef) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = lockTypeRegistryAndReleasePostCommit();
        tryUpdateByGUID(guid, entityDef, ttr);
        return getEntityDefStore(ttr).updateByGuid(guid, entityDef);
    }

    @Override
    @GraphTransaction
    public AtlasRelationshipDef updateRelationshipDefByName(String name, AtlasRelationshipDef relationshipDef) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = lockTypeRegistryAndReleasePostCommit();
        tryUpdateByName(name, relationshipDef, ttr);
        return getRelationshipDefStore(ttr).updateByName(name, relationshipDef);
    }

    @Override
    @GraphTransaction
    public AtlasRelationshipDef updateRelationshipDefByGuid(String guid, AtlasRelationshipDef relationshipDef) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = lockTypeRegistryAndReleasePostCommit();
        tryUpdateByGUID(guid, relationshipDef, ttr);
        return getRelationshipDefStore(ttr).updateByGuid(guid, relationshipDef);
    }

    @Override
    @GraphTransaction
    public AtlasTypesDef createTypesDef(AtlasTypesDef typesDef) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasTypeDefGraphStore.createTypesDef(enums={}, structs={}, classifications={}, entities={}, relationships={}, businessMetadataDefs={})",
                    CollectionUtils.size(typesDef.getEnumDefs()),
                    CollectionUtils.size(typesDef.getStructDefs()),
                    CollectionUtils.size(typesDef.getClassificationDefs()),
                    CollectionUtils.size(typesDef.getEntityDefs()),
                    CollectionUtils.size(typesDef.getRelationshipDefs()),
                    CollectionUtils.size(typesDef.getBusinessMetadataDefs()));
        }

        AtlasTransientTypeRegistry ttr = lockTypeRegistryAndReleasePostCommit();
        tryTypeCreation(typesDef, ttr);


        AtlasTypesDef ret = addToGraphStore(typesDef, ttr);

        try {
            ttr.updateTypes(ret);
        } catch (AtlasBaseException e) { // this shouldn't happen, as the types were already validated
            LOG.error("failed to update the registry after updating the store", e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasTypeDefGraphStore.createTypesDef(enums={}, structs={}, classfications={}, entities={}, relationships={}, businessMetadataDefs={})",
                    CollectionUtils.size(typesDef.getEnumDefs()),
                    CollectionUtils.size(typesDef.getStructDefs()),
                    CollectionUtils.size(typesDef.getClassificationDefs()),
                    CollectionUtils.size(typesDef.getEntityDefs()),
                    CollectionUtils.size(typesDef.getRelationshipDefs()),
                    CollectionUtils.size(typesDef.getBusinessMetadataDefs()));
        }

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasTypesDef createUpdateTypesDef(AtlasTypesDef typesDef) throws AtlasBaseException {
        AtlasTypesDef typesToCreate = getTypesToCreate(typesDef, typeRegistry);
        AtlasTypesDef typesToUpdate = getTypesToUpdate(typesDef, typeRegistry, false);

        return createUpdateTypesDef(typesToCreate, typesToUpdate);
    }

    @Override
    @GraphTransaction
    public AtlasTypesDef createUpdateTypesDef(AtlasTypesDef typesToCreate, AtlasTypesDef typesToUpdate) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasTypeDefGraphStore.createUpdateTypesDef({}, {})", typesToCreate, typesToUpdate);
        }

        AtlasTransientTypeRegistry ttr = lockTypeRegistryAndReleasePostCommit();

        if (!typesToUpdate.isEmpty()) {
            ttr.updateTypesWithNoRefResolve(typesToUpdate);
        }

        // Translate any NOT FOUND errors to BAD REQUEST
        tryTypeCreation(typesToCreate, ttr);

        AtlasTypesDef ret = addToGraphStore(typesToCreate, ttr);

        if (!typesToUpdate.isEmpty()) {
            AtlasTypesDef updatedTypes = updateGraphStore(typesToUpdate, ttr);

            if (CollectionUtils.isNotEmpty(updatedTypes.getEnumDefs())) {
                for (AtlasEnumDef enumDef : updatedTypes.getEnumDefs()) {
                    ret.getEnumDefs().add(enumDef);
                }
            }

            if (CollectionUtils.isNotEmpty(updatedTypes.getStructDefs())) {
                for (AtlasStructDef structDef : updatedTypes.getStructDefs()) {
                    ret.getStructDefs().add(structDef);
                }
            }

            if (CollectionUtils.isNotEmpty(updatedTypes.getClassificationDefs())) {
                for (AtlasClassificationDef classificationDef : updatedTypes.getClassificationDefs()) {
                    ret.getClassificationDefs().add(classificationDef);
                }
            }

            if (CollectionUtils.isNotEmpty(updatedTypes.getEntityDefs())) {
                for (AtlasEntityDef entityDef : updatedTypes.getEntityDefs()) {
                    ret.getEntityDefs().add(entityDef);
                }
            }
        }

        try {
            ttr.updateTypes(ret);
        } catch (AtlasBaseException e) { // this shouldn't happen, as the types were already validated
            LOG.error("failed to update the registry after updating the store", e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasTypeDefGraphStore.createUpdateTypesDef({}, {}): {}", typesToCreate, typesToUpdate, ret);
        }

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasTypesDef updateTypesDef(AtlasTypesDef typesDef) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasTypeDefGraphStore.updateTypesDef(enums={}, structs={}, classfications={}, entities={}, relationships{}, businessMetadataDefs={})",
                    CollectionUtils.size(typesDef.getEnumDefs()),
                    CollectionUtils.size(typesDef.getStructDefs()),
                    CollectionUtils.size(typesDef.getClassificationDefs()),
                    CollectionUtils.size(typesDef.getEntityDefs()),
                    CollectionUtils.size(typesDef.getRelationshipDefs()),
                    CollectionUtils.size(typesDef.getBusinessMetadataDefs()));
        }

        AtlasTransientTypeRegistry ttr = lockTypeRegistryAndReleasePostCommit();

        // Translate any NOT FOUND errors to BAD REQUEST
        try {
            ttr.updateTypes(typesDef);
        } catch (AtlasBaseException e) {
            if (AtlasErrorCode.TYPE_NAME_NOT_FOUND == e.getAtlasErrorCode()) {
                throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, e.getMessage());
            } else {
                throw e;
            }
        }

        AtlasTypesDef ret = updateGraphStore(typesDef, ttr);

        try {
            ttr.updateTypes(ret);
        } catch (AtlasBaseException e) { // this shouldn't happen, as the types were already validated
            LOG.error("failed to update the registry after updating the store", e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasTypeDefGraphStore.updateTypesDef(enums={}, structs={}, classfications={}, entities={}, relationships={}, businessMetadataDefs={})",
                    CollectionUtils.size(typesDef.getEnumDefs()),
                    CollectionUtils.size(typesDef.getStructDefs()),
                    CollectionUtils.size(typesDef.getClassificationDefs()),
                    CollectionUtils.size(typesDef.getEntityDefs()),
                    CollectionUtils.size(typesDef.getRelationshipDefs()),
                    CollectionUtils.size(typesDef.getBusinessMetadataDefs()));
        }

        return ret;

    }

    @Override
    @GraphTransaction
    public void deleteTypesDef(AtlasTypesDef typesDef) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasTypeDefGraphStore.deleteTypesDef(enums={}, structs={}, classfications={}, entities={}, relationships={}, businessMetadataDefs={})",
                    CollectionUtils.size(typesDef.getEnumDefs()),
                    CollectionUtils.size(typesDef.getStructDefs()),
                    CollectionUtils.size(typesDef.getClassificationDefs()),
                    CollectionUtils.size(typesDef.getEntityDefs()),
                    CollectionUtils.size(typesDef.getRelationshipDefs()),
                    CollectionUtils.size(typesDef.getBusinessMetadataDefs()));
        }

        AtlasTransientTypeRegistry ttr = lockTypeRegistryAndReleasePostCommit();

        AtlasDefStore<AtlasEnumDef>             enumDefStore             = getEnumDefStore(ttr);
        AtlasDefStore<AtlasStructDef>           structDefStore           = getStructDefStore(ttr);
        AtlasDefStore<AtlasClassificationDef>   classifiDefStore         = getClassificationDefStore(ttr);
        AtlasDefStore<AtlasEntityDef>           entityDefStore           = getEntityDefStore(ttr);
        AtlasDefStore<AtlasRelationshipDef>     relationshipDefStore     = getRelationshipDefStore(ttr);
        AtlasDefStore<AtlasBusinessMetadataDef> businessMetadataDefStore = getBusinessMetadataDefStore(ttr);

        List<AtlasVertex> preDeleteStructDefs   = new ArrayList<>();
        List<AtlasVertex> preDeleteClassifiDefs = new ArrayList<>();
        List<AtlasVertex> preDeleteEntityDefs   = new ArrayList<>();
        List<AtlasVertex> preDeleteRelationshipDefs = new ArrayList<>();

        // pre deletes

        // do the relationships first.
        if (CollectionUtils.isNotEmpty(typesDef.getRelationshipDefs())) {
            for (AtlasRelationshipDef relationshipDef : typesDef.getRelationshipDefs()) {
                if (StringUtils.isNotBlank(relationshipDef.getGuid())) {
                    preDeleteRelationshipDefs.add(relationshipDefStore.preDeleteByGuid(relationshipDef.getGuid()));
                } else {
                    preDeleteRelationshipDefs.add(relationshipDefStore.preDeleteByName(relationshipDef.getName()));
                }
            }
        }
        if (CollectionUtils.isNotEmpty(typesDef.getStructDefs())) {
            for (AtlasStructDef structDef : typesDef.getStructDefs()) {
                if (StringUtils.isNotBlank(structDef.getGuid())) {
                    preDeleteStructDefs.add(structDefStore.preDeleteByGuid(structDef.getGuid()));
                } else {
                    preDeleteStructDefs.add(structDefStore.preDeleteByName(structDef.getName()));
                }
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getClassificationDefs())) {
            for (AtlasClassificationDef classifiDef : typesDef.getClassificationDefs()) {
                if (StringUtils.isNotBlank(classifiDef.getGuid())) {
                    preDeleteClassifiDefs.add(classifiDefStore.preDeleteByGuid(classifiDef.getGuid()));
                } else {
                    preDeleteClassifiDefs.add(classifiDefStore.preDeleteByName(classifiDef.getName()));
                }
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getEntityDefs())) {
            for (AtlasEntityDef entityDef : typesDef.getEntityDefs()) {
                if (StringUtils.isNotBlank(entityDef.getGuid())) {
                    preDeleteEntityDefs.add(entityDefStore.preDeleteByGuid(entityDef.getGuid()));
                } else {
                    preDeleteEntityDefs.add(entityDefStore.preDeleteByName(entityDef.getName()));
                }
            }
        }

        // run the actual deletes

        // run the relationshipDef delete first - in case there is a enumDef or entityDef dependancy that is going to be deleted.
        if (CollectionUtils.isNotEmpty(typesDef.getRelationshipDefs())) {
            int i = 0;
            for (AtlasRelationshipDef relationshipDef : typesDef.getRelationshipDefs()) {
                if (StringUtils.isNotBlank(relationshipDef.getGuid())) {
                    relationshipDefStore.deleteByGuid(relationshipDef.getGuid(), preDeleteRelationshipDefs.get(i));
                } else {
                    relationshipDefStore.deleteByName(relationshipDef.getName(), preDeleteRelationshipDefs.get(i));
                }
                i++;
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getStructDefs())) {
            int i = 0;
            for (AtlasStructDef structDef : typesDef.getStructDefs()) {
                if (StringUtils.isNotBlank(structDef.getGuid())) {
                    structDefStore.deleteByGuid(structDef.getGuid(), preDeleteStructDefs.get(i));
                } else {
                    structDefStore.deleteByName(structDef.getName(), preDeleteStructDefs.get(i));
                }
                i++;
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getClassificationDefs())) {
            int i = 0;
            for (AtlasClassificationDef classifiDef : typesDef.getClassificationDefs()) {
                if (StringUtils.isNotBlank(classifiDef.getGuid())) {
                    classifiDefStore.deleteByGuid(classifiDef.getGuid(), preDeleteClassifiDefs.get(i));
                } else {
                    classifiDefStore.deleteByName(classifiDef.getName(), preDeleteClassifiDefs.get(i));
                }
                i++;
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getEntityDefs())) {
            int i = 0;
            for (AtlasEntityDef entityDef : typesDef.getEntityDefs()) {
                if (StringUtils.isNotBlank(entityDef.getGuid())) {
                    entityDefStore.deleteByGuid(entityDef.getGuid(), preDeleteEntityDefs.get(i));
                } else {
                    entityDefStore.deleteByName(entityDef.getName(), preDeleteEntityDefs.get(i));
                }
                i++;
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getEnumDefs())) {
            for (AtlasEnumDef enumDef : typesDef.getEnumDefs()) {
                if (StringUtils.isNotBlank(enumDef.getGuid())) {
                    enumDefStore.deleteByGuid(enumDef.getGuid(), null);
                } else {
                    enumDefStore.deleteByName(enumDef.getName(), null);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getBusinessMetadataDefs())) {
            for (AtlasBusinessMetadataDef businessMetadataDef : typesDef.getBusinessMetadataDefs()) {
                if (StringUtils.isNotBlank(businessMetadataDef.getGuid())) {
                    businessMetadataDefStore.deleteByGuid(businessMetadataDef.getGuid(), null);
                } else {
                    businessMetadataDefStore.deleteByName(businessMetadataDef.getName(), null);
                }
            }
        }

        // Remove all from
        ttr.removeTypesDef(typesDef);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasTypeDefGraphStore.deleteTypesDef(enums={}, structs={}, classfications={}, entities={})",
                    CollectionUtils.size(typesDef.getEnumDefs()),
                    CollectionUtils.size(typesDef.getStructDefs()),
                    CollectionUtils.size(typesDef.getClassificationDefs()),
                    CollectionUtils.size(typesDef.getEntityDefs()));
        }
    }


    @Override
    @GraphTransaction
    public void deleteTypeByName(String typeName) throws AtlasBaseException {
        AtlasType atlasType = typeRegistry.getType(typeName);
        if (atlasType == null) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS.TYPE_NAME_NOT_FOUND, typeName);
        }

        AtlasTypesDef typesDef = new AtlasTypesDef();
        AtlasBaseTypeDef baseTypeDef = getByNameNoAuthz(typeName);

        if (baseTypeDef instanceof AtlasClassificationDef) {
            typesDef.setClassificationDefs(Collections.singletonList((AtlasClassificationDef) baseTypeDef));
        } else if (baseTypeDef instanceof AtlasEntityDef) {
            typesDef.setEntityDefs(Collections.singletonList((AtlasEntityDef) baseTypeDef));
        } else if (baseTypeDef instanceof AtlasEnumDef) {
            typesDef.setEnumDefs(Collections.singletonList((AtlasEnumDef) baseTypeDef));
        } else if (baseTypeDef instanceof AtlasRelationshipDef) {
            typesDef.setRelationshipDefs(Collections.singletonList((AtlasRelationshipDef) baseTypeDef));
        } else if (baseTypeDef instanceof AtlasBusinessMetadataDef) {
            typesDef.setBusinessMetadataDefs(Collections.singletonList((AtlasBusinessMetadataDef) baseTypeDef));
        } else if (baseTypeDef instanceof AtlasStructDef) {
            typesDef.setStructDefs(Collections.singletonList((AtlasStructDef) baseTypeDef));
        }

        deleteTypesDef(typesDef);
    }

    @Override
    public AtlasTypesDef searchTypesDef(SearchFilter searchFilter) throws AtlasBaseException {
        final AtlasTypesDef typesDef = new AtlasTypesDef();
        Predicate searchPredicates = FilterUtil.getPredicateFromSearchFilter(searchFilter);

        for(AtlasEnumType enumType : typeRegistry.getAllEnumTypes()) {
            if (searchPredicates.evaluate(enumType)) {
                typesDef.getEnumDefs().add(enumType.getEnumDef());
            }
        }

        for(AtlasStructType structType : typeRegistry.getAllStructTypes()) {
            if (searchPredicates.evaluate(structType)) {
                typesDef.getStructDefs().add(structType.getStructDef());
            }
        }

        for(AtlasClassificationType classificationType : typeRegistry.getAllClassificationTypes()) {
            if (searchPredicates.evaluate(classificationType)) {
                typesDef.getClassificationDefs().add(classificationType.getClassificationDef());
            }
        }

        for(AtlasEntityType entityType : typeRegistry.getAllEntityTypes()) {
            if (searchPredicates.evaluate(entityType)) {
                typesDef.getEntityDefs().add(entityType.getEntityDef());
            }
        }

        for(AtlasRelationshipType relationshipType : typeRegistry.getAllRelationshipTypes()) {
            if (searchPredicates.evaluate(relationshipType)) {
                typesDef.getRelationshipDefs().add(relationshipType.getRelationshipDef());
            }
        }

        for(AtlasBusinessMetadataType businessMetadataType : typeRegistry.getAllBusinessMetadataTypes()) {
            if (searchPredicates.evaluate(businessMetadataType)) {
                typesDef.getBusinessMetadataDefs().add(businessMetadataType.getBusinessMetadataDef());
            }
        }

        AtlasAuthorizationUtils.filterTypesDef(new AtlasTypesDefFilterRequest(typesDef));

        return typesDef;
    }

    @Override
    public AtlasBaseTypeDef getByName(String name) throws AtlasBaseException {
        if (StringUtils.isBlank(name)) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_INVALID, "", name);
        }
        AtlasType type = typeRegistry.getType(name);
        AtlasBaseTypeDef ret = getTypeDefFromTypeWithNoAuthz(type);

        if (ret != null) {
            AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_READ, ret), "read type ", name);
        }

        return ret;
    }

    @Override
    public AtlasBaseTypeDef getByGuid(String guid) throws AtlasBaseException {
        if (StringUtils.isBlank(guid)) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_GUID_NOT_FOUND, guid);
        }
        AtlasType type = typeRegistry.getTypeByGuid(guid);
        AtlasBaseTypeDef ret = getTypeDefFromTypeWithNoAuthz(type);

        if (ret != null) {
            AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_READ, ret), "read type ", guid);
        }

        return ret;
    }

    private AtlasBaseTypeDef getByNameNoAuthz(String name) throws AtlasBaseException {
        if (StringUtils.isBlank(name)) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_INVALID, "", name);
        }

        AtlasType type = typeRegistry.getType(name);

        return getTypeDefFromTypeWithNoAuthz(type);
    }

    private AtlasBaseTypeDef getTypeDefFromTypeWithNoAuthz(AtlasType type) throws AtlasBaseException {
        AtlasBaseTypeDef ret;
        switch (type.getTypeCategory()) {
            case ENUM:
                ret = ((AtlasEnumType) type).getEnumDef();
                break;
            case STRUCT:
                ret = ((AtlasStructType) type).getStructDef();
                break;
            case CLASSIFICATION:
                ret = ((AtlasClassificationType) type).getClassificationDef();
                break;
            case ENTITY:
                ret = ((AtlasEntityType) type).getEntityDef();
                break;
            case RELATIONSHIP:
                ret = ((AtlasRelationshipType) type).getRelationshipDef();
                break;
            case BUSINESS_METADATA:
                ret = ((AtlasBusinessMetadataType) type).getBusinessMetadataDef();
                break;
            case PRIMITIVE:
            case OBJECT_ID_TYPE:
            case ARRAY:
            case MAP:
            default:
                throw new AtlasBaseException(AtlasErrorCode.SYSTEM_TYPE, type.getTypeCategory().name());
        }

        return ret;
    }

    private AtlasTransientTypeRegistry lockTypeRegistryAndReleasePostCommit() throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.lockTypeRegistryForUpdate(typeUpdateLockMaxWaitTimeSeconds);

        new TypeRegistryUpdateHook(ttr);

        return ttr;
    }

    private void rectifyTypeErrorsIfAny(AtlasTypesDef typesDef) {
        final Set<String> entityNames = new HashSet<>();

        if (CollectionUtils.isNotEmpty(typesDef.getEntityDefs())) {
            for (AtlasEntityDef entityDef : typesDef.getEntityDefs()) {
                entityNames.add(entityDef.getName());
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getStructDefs())) {
            for (AtlasStructDef structDef : typesDef.getStructDefs()) {
                rectifyAttributesIfNeeded(entityNames, structDef);
            }
            removeDuplicateTypeIfAny(typesDef.getStructDefs());
        }

        if (CollectionUtils.isNotEmpty(typesDef.getClassificationDefs())) {
            for (AtlasClassificationDef classificationDef : typesDef.getClassificationDefs()) {
                rectifyAttributesIfNeeded(entityNames, classificationDef);
            }
            removeDuplicateTypeIfAny(typesDef.getClassificationDefs());
        }

        if (CollectionUtils.isNotEmpty(typesDef.getEntityDefs())) {
            for (AtlasEntityDef entityDef : typesDef.getEntityDefs()) {
                rectifyAttributesIfNeeded(entityNames, entityDef);
            }
            removeDuplicateTypeIfAny(typesDef.getEntityDefs());
        }
    }

    private <T extends AtlasBaseTypeDef> void removeDuplicateTypeIfAny(List<T> defList) {
        final Set<String> entityDefNames = new HashSet<>();

        for (int i = 0; i < defList.size(); i++) {
            if (!entityDefNames.add((defList.get(i)).getName())) {
                LOG.warn(" Found Duplicate Type => " + defList.get(i).getName());
                defList.remove(i);
                i--;
            }
        }
    }


    private void rectifyAttributesIfNeeded(final Set<String> entityNames, AtlasStructDef structDef) {
        List<AtlasAttributeDef> attributeDefs = structDef.getAttributeDefs();

        if (CollectionUtils.isNotEmpty(attributeDefs)) {
            for (AtlasAttributeDef attributeDef : attributeDefs) {
                if (!hasOwnedReferenceConstraint(attributeDef.getConstraints())) {
                    continue;
                }

                Set<String> referencedTypeNames = AtlasTypeUtil.getReferencedTypeNames(attributeDef.getTypeName());

                boolean valid = false;

                for (String referencedTypeName : referencedTypeNames) {
                    if (entityNames.contains(referencedTypeName)) {
                        valid = true;
                        break;
                    }
                }

                if (!valid) {
                    rectifyOwnedReferenceError(structDef, attributeDef);
                }
            }
        }
    }

    private boolean hasOwnedReferenceConstraint(List<AtlasConstraintDef> constraints) {
        if (CollectionUtils.isNotEmpty(constraints)) {
            for (AtlasConstraintDef constraint : constraints) {
                if (constraint.isConstraintType(AtlasConstraintDef.CONSTRAINT_TYPE_OWNED_REF)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void rectifyOwnedReferenceError(AtlasStructDef structDef, AtlasAttributeDef attributeDef) {
        List<AtlasConstraintDef> constraints = attributeDef.getConstraints();

        if (CollectionUtils.isNotEmpty(constraints)) {
            for (int i = 0; i < constraints.size(); i++) {
                AtlasConstraintDef constraint = constraints.get(i);

                if (constraint.isConstraintType(AtlasConstraintDef.CONSTRAINT_TYPE_OWNED_REF)) {
                    LOG.warn("Invalid constraint ownedRef for attribute {}.{}", structDef.getName(), attributeDef.getName());

                    constraints.remove(i);
                    i--;
                }
            }
        }
    }

    private AtlasTypesDef addToGraphStore(AtlasTypesDef typesDef, AtlasTransientTypeRegistry ttr) throws AtlasBaseException {
        AtlasTypesDef ret = new AtlasTypesDef();

        AtlasDefStore<AtlasEnumDef>             enumDefStore             = getEnumDefStore(ttr);
        AtlasDefStore<AtlasStructDef>           structDefStore           = getStructDefStore(ttr);
        AtlasDefStore<AtlasClassificationDef>   classifiDefStore         = getClassificationDefStore(ttr);
        AtlasDefStore<AtlasEntityDef>           entityDefStore           = getEntityDefStore(ttr);
        AtlasDefStore<AtlasRelationshipDef>     relationshipDefStore     = getRelationshipDefStore(ttr);
        AtlasDefStore<AtlasBusinessMetadataDef> businessMetadataDefStore = getBusinessMetadataDefStore(ttr);

        List<AtlasVertex> preCreateStructDefs           = new ArrayList<>();
        List<AtlasVertex> preCreateClassifiDefs         = new ArrayList<>();
        List<AtlasVertex> preCreateEntityDefs           = new ArrayList<>();
        List<AtlasVertex> preCreateRelationshipDefs     = new ArrayList<>();
        List<AtlasVertex> preCreateBusinessMetadataDefs = new ArrayList<>();

        // for enumerations run the create
        if (CollectionUtils.isNotEmpty(typesDef.getEnumDefs())) {
            for (AtlasEnumDef enumDef : typesDef.getEnumDefs()) {
                AtlasEnumDef createdDef = enumDefStore.create(enumDef, null);

                ttr.updateGuid(createdDef.getName(), createdDef.getGuid());

                ret.getEnumDefs().add(createdDef);
            }
        }
        // run the preCreates

        if (CollectionUtils.isNotEmpty(typesDef.getStructDefs())) {
            for (AtlasStructDef structDef : typesDef.getStructDefs()) {
                preCreateStructDefs.add(structDefStore.preCreate(structDef));
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getClassificationDefs())) {
            for (AtlasClassificationDef classifiDef : typesDef.getClassificationDefs()) {
                preCreateClassifiDefs.add(classifiDefStore.preCreate(classifiDef));
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getEntityDefs())) {
            for (AtlasEntityDef entityDef : typesDef.getEntityDefs()) {
                preCreateEntityDefs.add(entityDefStore.preCreate(entityDef));
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getRelationshipDefs())) {
            for (AtlasRelationshipDef relationshipDef : typesDef.getRelationshipDefs()) {
                preCreateRelationshipDefs.add(relationshipDefStore.preCreate(relationshipDef));
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getBusinessMetadataDefs())) {
            for (AtlasBusinessMetadataDef businessMetadataDef : typesDef.getBusinessMetadataDefs()) {
                preCreateBusinessMetadataDefs.add(businessMetadataDefStore.preCreate(businessMetadataDef));
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getStructDefs())) {
            int i = 0;
            for (AtlasStructDef structDef : typesDef.getStructDefs()) {
                AtlasStructDef createdDef = structDefStore.create(structDef, preCreateStructDefs.get(i));

                ttr.updateGuid(createdDef.getName(), createdDef.getGuid());

                ret.getStructDefs().add(createdDef);
                i++;
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getClassificationDefs())) {
            int i = 0;
            for (AtlasClassificationDef classifiDef : typesDef.getClassificationDefs()) {
                AtlasClassificationDef createdDef = classifiDefStore.create(classifiDef, preCreateClassifiDefs.get(i));

                ttr.updateGuid(createdDef.getName(), createdDef.getGuid());

                ret.getClassificationDefs().add(createdDef);
                i++;
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getEntityDefs())) {
            int i = 0;
            for (AtlasEntityDef entityDef : typesDef.getEntityDefs()) {
                AtlasEntityDef createdDef = entityDefStore.create(entityDef, preCreateEntityDefs.get(i));

                ttr.updateGuid(createdDef.getName(), createdDef.getGuid());

                ret.getEntityDefs().add(createdDef);
                i++;
            }
        }
        if (CollectionUtils.isNotEmpty(typesDef.getRelationshipDefs())) {
            int i = 0;
            for (AtlasRelationshipDef relationshipDef : typesDef.getRelationshipDefs()) {
                AtlasRelationshipDef createdDef = relationshipDefStore.create(relationshipDef, preCreateRelationshipDefs.get(i));

                ttr.updateGuid(createdDef.getName(), createdDef.getGuid());

                ret.getRelationshipDefs().add(createdDef);
                i++;
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getBusinessMetadataDefs())) {
            int i = 0;
            for (AtlasBusinessMetadataDef businessMetadataDef : typesDef.getBusinessMetadataDefs()) {
                AtlasBusinessMetadataDef createdDef = businessMetadataDefStore.create(businessMetadataDef, preCreateBusinessMetadataDefs.get(i));

                ttr.updateGuid(createdDef.getName(), createdDef.getGuid());

                ret.getBusinessMetadataDefs().add(createdDef);
                i++;
            }
        }

        return ret;
    }

    private AtlasTypesDef updateGraphStore(AtlasTypesDef typesDef, AtlasTransientTypeRegistry ttr) throws AtlasBaseException {
        AtlasTypesDef ret = new AtlasTypesDef();

        AtlasDefStore<AtlasEnumDef>             enumDefStore             = getEnumDefStore(ttr);
        AtlasDefStore<AtlasStructDef>           structDefStore           = getStructDefStore(ttr);
        AtlasDefStore<AtlasClassificationDef>   classifiDefStore         = getClassificationDefStore(ttr);
        AtlasDefStore<AtlasEntityDef>           entityDefStore           = getEntityDefStore(ttr);
        AtlasDefStore<AtlasRelationshipDef>     relationDefStore         = getRelationshipDefStore(ttr);
        AtlasDefStore<AtlasBusinessMetadataDef> businessMetadataDefStore = getBusinessMetadataDefStore(ttr);

        if (CollectionUtils.isNotEmpty(typesDef.getEnumDefs())) {
            for (AtlasEnumDef enumDef : typesDef.getEnumDefs()) {
                ret.getEnumDefs().add(enumDefStore.update(enumDef));
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getStructDefs())) {
            for (AtlasStructDef structDef : typesDef.getStructDefs()) {
                ret.getStructDefs().add(structDefStore.update(structDef));
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getClassificationDefs())) {
            for (AtlasClassificationDef classifiDef : typesDef.getClassificationDefs()) {
                ret.getClassificationDefs().add(classifiDefStore.update(classifiDef));
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getEntityDefs())) {
            for (AtlasEntityDef entityDef : typesDef.getEntityDefs()) {
                ret.getEntityDefs().add(entityDefStore.update(entityDef));
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getRelationshipDefs())) {
            for (AtlasRelationshipDef relationshipDef : typesDef.getRelationshipDefs()) {
                ret.getRelationshipDefs().add(relationDefStore.update(relationshipDef));
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getBusinessMetadataDefs())) {
            for (AtlasBusinessMetadataDef businessMetadataDef : typesDef.getBusinessMetadataDefs()) {
                ret.getBusinessMetadataDefs().add(businessMetadataDefStore.update(businessMetadataDef));
            }
        }

        return ret;
    }

    private class TypeRegistryUpdateHook extends GraphTransactionInterceptor.PostTransactionHook {

        private final AtlasTransientTypeRegistry ttr;

        private TypeRegistryUpdateHook(AtlasTransientTypeRegistry ttr) {
            super();

            this.ttr = ttr;
        }
        @Override
        public void onComplete(boolean isSuccess) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("==> TypeRegistryUpdateHook.onComplete({})", isSuccess);
            }

            typeRegistry.releaseTypeRegistryForUpdate(ttr, isSuccess);

            if (isSuccess) {
                notifyListeners(ttr);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== TypeRegistryUpdateHook.onComplete({})", isSuccess);
            }
        }

        private void notifyListeners(AtlasTransientTypeRegistry ttr) {
            if (CollectionUtils.isNotEmpty(typeDefChangeListeners)) {
                ChangedTypeDefs changedTypeDefs = new ChangedTypeDefs(ttr.getAddedTypes(),
                        ttr.getUpdatedTypes(),
                        ttr.getDeleteedTypes());

                for (TypeDefChangeListener changeListener : typeDefChangeListeners) {
                    try {
                        changeListener.onChange(changedTypeDefs);
                    } catch (Throwable t) {
                        LOG.error("OnChange failed for listener {}", changeListener.getClass().getName(), t);
                    }
                }
            }
        }

    }

    @Override
    public void notifyLoadCompletion(){
        for (TypeDefChangeListener changeListener : typeDefChangeListeners) {
            try {
                changeListener.onLoadCompletion();
            } catch (Throwable t) {
                LOG.error("OnLoadCompletion failed for listener {}", changeListener.getClass().getName(), t);
            }
        }
    }

    private void tryUpdateByName(String name, AtlasBaseTypeDef typeDef, AtlasTransientTypeRegistry ttr) throws AtlasBaseException {
        try {
            ttr.updateTypeByName(name, typeDef);
        } catch (AtlasBaseException e) {
            if (AtlasErrorCode.TYPE_NAME_NOT_FOUND == e.getAtlasErrorCode()) {
                throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, e.getMessage());
            } else {
                throw e;
            }
        }
    }

    private void tryUpdateByGUID(String guid, AtlasBaseTypeDef typeDef, AtlasTransientTypeRegistry ttr) throws AtlasBaseException {
        try {
            ttr.updateTypeByGuid(guid, typeDef);
        } catch (AtlasBaseException e) {
            if (AtlasErrorCode.TYPE_GUID_NOT_FOUND == e.getAtlasErrorCode()) {
                throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, e.getMessage());
            } else {
                throw e;
            }
        }
    }

    private void tryTypeCreation(AtlasTypesDef typesDef, AtlasTransientTypeRegistry ttr) throws AtlasBaseException {
        // Translate any NOT FOUND errors to BAD REQUEST
        try {
            ttr.addTypes(typesDef);
        } catch (AtlasBaseException e) {
            if (AtlasErrorCode.TYPE_NAME_NOT_FOUND == e.getAtlasErrorCode() ||
                    AtlasErrorCode.TYPE_GUID_NOT_FOUND == e.getAtlasErrorCode()) {
                throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, e.getMessage());
            } else {
                throw e;
            }
        }
    }
}
