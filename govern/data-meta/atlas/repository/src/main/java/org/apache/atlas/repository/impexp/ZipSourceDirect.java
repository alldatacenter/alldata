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

package org.apache.atlas.repository.impexp;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.atlas.entitytransform.BaseEntityHandler;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasExportResult;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.store.graph.v2.EntityImportStream;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.utils.AtlasJson;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.atlas.AtlasErrorCode.IMPORT_ATTEMPTING_EMPTY_ZIP;

public class ZipSourceDirect implements EntityImportStream {
    private static final Logger LOG = LoggerFactory.getLogger(ZipSourceDirect.class);
    private static final String ZIP_ENTRY_ENTITIES = "entities.json";

    private final ZipInputStream zipInputStream;
    private int currentPosition;

    private ImportTransforms importTransform;
    private List<BaseEntityHandler> entityHandlers;
    private AtlasTypesDef typesDef;
    private int streamSize = 1;

    EntitiesArrayParser entitiesArrayParser;

    public ZipSourceDirect(InputStream inputStream, int streamSize) throws IOException, AtlasBaseException {
        this.zipInputStream = new ZipInputStream(inputStream);
        this.streamSize = streamSize;
        prepareStreamForFetch();

        if (this.streamSize == 1) {
            LOG.info("ZipSourceDirect: Stream Size set to: {}. This will cause inaccurate percentage reporting.", this.streamSize);
        }
    }

    @Override
    public ImportTransforms getImportTransform() {
        return this.importTransform;
    }

    @Override
    public void setImportTransform(ImportTransforms importTransform) {
        this.importTransform = importTransform;
    }

    @Override
    public List<BaseEntityHandler> getEntityHandlers() {
        return entityHandlers;
    }

    @Override
    public void setEntityHandlers(List<BaseEntityHandler> entityHandlers) {
        this.entityHandlers = entityHandlers;
    }

    @Override
    public AtlasTypesDef getTypesDef() throws AtlasBaseException {
        return this.typesDef;
    }

    @Override
    public AtlasExportResult getExportResult() throws AtlasBaseException {
        return new AtlasExportResult();
    }

    @Override
    public List<String> getCreationOrder() {
        return new ArrayList<>();
    }

    @Override
    public int getPosition() {
        return currentPosition;
    }

    @Override
    public AtlasEntity.AtlasEntityWithExtInfo getEntityWithExtInfo(String json) throws AtlasBaseException {
        if (StringUtils.isEmpty(json)) {
            return null;
        }

        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = convertFromJson(AtlasEntity.AtlasEntityWithExtInfo.class, json);

        if (importTransform != null) {
            entityWithExtInfo = importTransform.apply(entityWithExtInfo);
        }

        if (entityHandlers != null) {
            applyTransformers(entityWithExtInfo);
        }

        return entityWithExtInfo;
    }

    @Override
    public boolean hasNext() {
        return (this.entitiesArrayParser != null && entitiesArrayParser.hasNext());
    }

    @Override
    public AtlasEntity next() {
        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = getNextEntityWithExtInfo();

        return entityWithExtInfo != null ? entityWithExtInfo.getEntity() : null;
    }

    @Override
    public AtlasEntity.AtlasEntityWithExtInfo getNextEntityWithExtInfo() {
        try {
            if (hasNext()) {
                String json = moveNext();
                return getEntityWithExtInfo(json);
            }
        } catch (AtlasBaseException e) {
            LOG.error("getNextEntityWithExtInfo", e);
        }
        return null;
    }

    @Override
    public void reset() {
        currentPosition = 0;
    }

    @Override
    public AtlasEntity getByGuid(String guid) {
        try {
            return getEntity(guid);
        } catch (AtlasBaseException e) {
            LOG.error("getByGuid: {} failed!", guid, e);
            return null;
        }
    }

    @Override
    public void onImportComplete(String guid) {
    }

    @Override
    public void setPosition(int index) {
        for (int i = 0; i < index; i++) {
            moveNext();
        }
    }

    @Override
    public void setPositionUsingEntityGuid(String guid) {
    }

    @Override
    public void close() {
        if (this.entitiesArrayParser != null) {
            this.entitiesArrayParser.close();
        }
    }

    private void applyTransformers(AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo) {
        if (entityWithExtInfo == null) {
            return;
        }

        transform(entityWithExtInfo.getEntity());

        if (MapUtils.isNotEmpty(entityWithExtInfo.getReferredEntities())) {
            for (AtlasEntity e : entityWithExtInfo.getReferredEntities().values()) {
                transform(e);
            }
        }
    }

    private void transform(AtlasEntity e) {
        for (BaseEntityHandler handler : entityHandlers) {
            handler.transform(e);
        }
    }

    private <T> T convertFromJson(Class<T> clazz, String jsonData) throws AtlasBaseException {
        try {
            return AtlasType.fromJson(jsonData, clazz);

        } catch (Exception e) {
            throw new AtlasBaseException("Error converting file to JSON.", e);
        }
    }

    private AtlasEntity getEntity(String guid) throws AtlasBaseException {
        AtlasEntity.AtlasEntityWithExtInfo extInfo = getEntityWithExtInfo(guid);
        return (extInfo != null) ? extInfo.getEntity() : null;
    }

    public int size() {
        if (this.streamSize == 1) {
            return currentPosition;
        }

        return this.streamSize;
    }

    private String moveNext() {
        try {
            moveNextEntry();
            return entitiesArrayParser.next();
        } catch (IOException e) {
            LOG.error("moveNext failed!", e);
        }

        return null;
    }

    private void moveNextEntry() throws IOException {
        this.currentPosition++;
    }

    private void prepareStreamForFetch() throws AtlasBaseException, IOException {
        ZipEntry zipEntryNext = zipInputStream.getNextEntry();
        if (zipEntryNext == null) {
            throw new AtlasBaseException(IMPORT_ATTEMPTING_EMPTY_ZIP, "Attempting to import empty ZIP.");
        }

        if (zipEntryNext.getName().equals(ZipExportFileNames.ATLAS_TYPESDEF_NAME.toEntryFileName())) {
            String json = getJsonPayloadFromZipEntryStream(this.zipInputStream);
            this.typesDef = AtlasType.fromJson(json, AtlasTypesDef.class);
            zipEntryNext = zipInputStream.getNextEntry();
        }

        if (zipEntryNext.getName().equals(ZIP_ENTRY_ENTITIES)) {
            this.entitiesArrayParser = new EntitiesArrayParser(zipInputStream);
        } else {
            throw new AtlasBaseException(IMPORT_ATTEMPTING_EMPTY_ZIP, "Attempting to import empty ZIP. " + ZIP_ENTRY_ENTITIES + " could not be found!");
        }
    }

    private String getJsonPayloadFromZipEntryStream(ZipInputStream zipInputStream) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            IOUtils.copy(zipInputStream, bos);
        } catch (IOException e) {
            LOG.error("Streaming copying failed!", e);
            return null;
        }
        return bos.toString();
    }

    static class EntitiesArrayParser {
        private static final String EMPTY_OBJECT = "{}";

        private final JsonFactory factory;
        private final JsonParser parser;
        private boolean hasNext;

        public EntitiesArrayParser(InputStream inputStream) throws IOException {
            this.factory = AtlasJson.getMapper().getFactory();
            this.parser = factory.createParser(inputStream);

            parseNext();
        }

        public String next() throws IOException {
            JsonToken jsonToken = parseNext();
            if (!hasNext) {
                return null;
            }

            if (jsonToken != null && jsonToken == JsonToken.START_OBJECT) {
                JsonNode node = parser.readValueAsTree();
                return validate(node.toString());
            }
            return null;

        }

        private JsonToken parseNext() throws IOException {
            JsonToken jsonToken = this.parser.nextToken();
            hasNext = (jsonToken != null) && (jsonToken != JsonToken.END_ARRAY);
            return jsonToken;
        }

        private String validate(String payload) {
            if (payload.equals(EMPTY_OBJECT)) {
                hasNext = false;
                close();
                return null;
            }

            return payload;
        }

        public boolean hasNext() {
            return hasNext;
        }

        public void close() {
            try {
                this.parser.close();
            } catch (IOException e) {
                LOG.error("Error closing parser!", e);
            }
        }
    }
}
