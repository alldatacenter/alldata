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
package org.apache.atlas.repository.ogm;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.AtlasBaseModelObject;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStream;
import org.apache.atlas.utils.AtlasPerfTracer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Component
public class DataAccess {
    private static final Logger LOG      = LoggerFactory.getLogger(DataAccess.class);
    private static final Logger PERF_LOG = AtlasPerfTracer.getPerfLogger("repository.DataAccess");

    private final AtlasEntityStore entityStore;
    private final DTORegistry      dtoRegistry;

    @Inject
    public DataAccess(AtlasEntityStore entityStore, DTORegistry dtoRegistry) {
        this.entityStore = entityStore;
        this.dtoRegistry = dtoRegistry;
    }

    public <T extends AtlasBaseModelObject> T save(T obj) throws AtlasBaseException {
        saveNoLoad(obj);
        return this.load(obj);
    }

    public <T extends AtlasBaseModelObject> void saveNoLoad(T obj) throws AtlasBaseException {
        Objects.requireNonNull(obj, "Can't save a null object");

        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "DataAccess.save()");
            }

            DataTransferObject<T> dto = (DataTransferObject<T>) dtoRegistry.get(obj.getClass());

            AtlasEntityWithExtInfo entityWithExtInfo      = dto.toEntityWithExtInfo(obj);
            EntityMutationResponse entityMutationResponse = entityStore.createOrUpdate(new AtlasEntityStream(entityWithExtInfo), false);

            // Update GUID assignment for newly created entity
            if (CollectionUtils.isNotEmpty(entityMutationResponse.getCreatedEntities())) {
                String assignedGuid = entityMutationResponse.getGuidAssignments().get(obj.getGuid());
                if (!obj.getGuid().equals(assignedGuid)) {
                    obj.setGuid(assignedGuid);
                }
            }
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    public <T extends AtlasBaseModelObject> Iterable<T> save(Iterable<T> obj) throws AtlasBaseException {
        Objects.requireNonNull(obj, "Can't save a null object");

        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "DataAccess.multiSave()");
            }

            List<T> ret = new ArrayList<>();
            for (T o : obj) {
                ret.add(save(o));
            }
            return ret;

        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    public <T extends AtlasBaseModelObject> Iterable<T> load(final Iterable<T> objects) throws AtlasBaseException {
        Objects.requireNonNull(objects, "Objects to load");

        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "DataAccess.multiLoad()");
            }

            List<AtlasBaseModelObject> ret = new ArrayList<>();

            for (T object : objects) {
                try {
                    ret.add(load(object));
                } catch (AtlasBaseException e) {
                    // In case of bulk load, some entities might be in deleted state causing an exception to be thrown
                    // by the single load API call
                    LOG.warn("Bulk load encountered an error.", e);
                }
            }

            return (Iterable<T>) ret;

        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    public <T extends AtlasBaseModelObject> T load(T obj) throws AtlasBaseException {
        return load(obj, false);
    }

    public <T extends AtlasBaseModelObject> T load(T obj, boolean loadDeleted) throws AtlasBaseException {
        Objects.requireNonNull(obj, "Can't load a null object");

        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "DataAccess.load()");
            }

            DataTransferObject<T> dto = (DataTransferObject<T>) dtoRegistry.get(obj.getClass());

            AtlasEntityWithExtInfo entityWithExtInfo;

            String guid = obj.getGuid();
            // GUID can be null/empty/-ve
            if (StringUtils.isNotEmpty(guid) && guid.charAt(0) != '-') {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Load using GUID");
                }
                entityWithExtInfo = entityStore.getById(guid);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Load using unique attributes");
                }
                entityWithExtInfo = entityStore.getByUniqueAttributes(dto.getEntityType(), dto.getUniqueAttributes(obj));
            }

            // Since GUID alone can't be used to determine what ENTITY TYPE is loaded from the graph
            String actualTypeName   = entityWithExtInfo.getEntity().getTypeName();
            String expectedTypeName = dto.getEntityType().getTypeName();
            if (!actualTypeName.equals(expectedTypeName)) {
                throw new AtlasBaseException(AtlasErrorCode.UNEXPECTED_TYPE, expectedTypeName, actualTypeName);
            }

            if (!loadDeleted && entityWithExtInfo.getEntity().getStatus() == AtlasEntity.Status.DELETED) {
                throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_DELETED, guid);
            }

            return dto.from(entityWithExtInfo);

        } finally {
            AtlasPerfTracer.log(perf);
        }

    }

    public <T extends AtlasBaseModelObject> T load(String guid, Class<? extends AtlasBaseModelObject> clazz) throws AtlasBaseException {
        DataTransferObject<T>  dto = (DataTransferObject<T>)dtoRegistry.get(clazz);

        AtlasEntityWithExtInfo entityWithExtInfo = null;

        if (StringUtils.isNotEmpty(guid)) {
            entityWithExtInfo = entityStore.getById(guid);
        }

        if(entityWithExtInfo == null) {
            return null;
        }

        return dto.from(entityWithExtInfo);
    }

    public void deleteUsingGuid(String guid) throws AtlasBaseException {
        entityStore.deleteById(guid);
    }

    public void delete(String guid) throws AtlasBaseException {
        Objects.requireNonNull(guid, "guid");
        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "DataAccess.delete()");
            }

            entityStore.deleteById(guid);

        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    public void delete(List<String> guids) throws AtlasBaseException {
        Objects.requireNonNull(guids, "guids");

        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "DataAccess.multiDelete()");
            }

            entityStore.deleteByIds(guids);

        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    public <T extends AtlasBaseModelObject> void delete(T obj) throws AtlasBaseException {
        Objects.requireNonNull(obj, "Can't delete a null object");

        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "DataAccess.delete()");
            }

            T object = load(obj);

            if (object != null) {
                delete(object.getGuid());
            }

        } finally {
            AtlasPerfTracer.log(perf);
        }
    }


}
