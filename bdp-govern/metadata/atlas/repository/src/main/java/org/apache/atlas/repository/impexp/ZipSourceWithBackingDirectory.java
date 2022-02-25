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

import org.apache.atlas.entitytransform.BaseEntityHandler;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasExportResult;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.store.graph.v2.EntityImportStream;
import org.apache.atlas.type.AtlasType;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.atlas.AtlasErrorCode.IMPORT_ATTEMPTING_EMPTY_ZIP;

public class ZipSourceWithBackingDirectory implements EntityImportStream {
    private static final Logger LOG = LoggerFactory.getLogger(ZipSourceWithBackingDirectory.class);
    private static final String TEMPORARY_DIRECTORY_PREFIX = "atlas-import-temp-";
    private static final String EXT_JSON = ".json";

    private Path tempDirectory;

    private ImportTransforms importTransform;
    private List<BaseEntityHandler> entityHandlers;

    private ArrayList<String> creationOrder = new ArrayList<>();
    private int currentPosition;
    private int numberOfEntries;

    public ZipSourceWithBackingDirectory(InputStream inputStream) throws IOException, AtlasBaseException {
        this(inputStream, null);
    }

    public ZipSourceWithBackingDirectory(InputStream inputStream, String backingDirectory) throws IOException, AtlasBaseException {
        setupBackingStore(inputStream, backingDirectory);
        if (isZipFileEmpty()) {
            throw new AtlasBaseException(IMPORT_ATTEMPTING_EMPTY_ZIP, "Attempting to import empty ZIP.");
        }
    }

    @Override
    public ImportTransforms getImportTransform() { return this.importTransform; }

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
        return getJsonFromEntry(ZipExportFileNames.ATLAS_TYPESDEF_NAME.toString(), AtlasTypesDef.class);
    }

    @Override
    public AtlasExportResult getExportResult() throws AtlasBaseException {
        return getJsonFromEntry(ZipExportFileNames.ATLAS_EXPORT_INFO_NAME.toString(), AtlasExportResult.class);
    }

    @Override
    public List<String> getCreationOrder() {
        return creationOrder;
    }

    @Override
    public int getPosition() {
        return currentPosition;
    }

    @Override
    public AtlasEntity.AtlasEntityWithExtInfo getEntityWithExtInfo(String guid) throws AtlasBaseException {
        final File file = getFileFromTemporaryDirectory(guid + EXT_JSON);
        if (!file.exists()) {
            return null;
        }

        String json = getJsonStringForFile(file);
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
        return (currentPosition < numberOfEntries);
    }

    @Override
    public AtlasEntity next() {
        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = getNextEntityWithExtInfo();

        return entityWithExtInfo != null ? entityWithExtInfo.getEntity() : null;
    }

    @Override
    public AtlasEntity.AtlasEntityWithExtInfo getNextEntityWithExtInfo() {
        try {
            return getEntityWithExtInfo(moveNext());
        } catch (AtlasBaseException e) {
            LOG.error("getNextEntityWithExtInfo", e);
            return null;
        }
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
        getFileFromTemporaryDirectory(guid + EXT_JSON).delete();
    }

    @Override
    public void setPosition(int index) {
        reset();
        for (int i = 0; i < numberOfEntries && i <= index; i++) {
            onImportComplete(moveNext());
        }
    }

    @Override
    public void setPositionUsingEntityGuid(String guid) {
        if (StringUtils.isEmpty(guid)) {
            return;
        }

        String current;
        while (currentPosition < numberOfEntries) {
            current = creationOrder.get(currentPosition);
            if (current.equals(guid)) {
                return;
            }

            moveNext();
        }
    }

    @Override
    public void close() {
        creationOrder.clear();
        try {
            LOG.error("Import: Removing temporary directory: {}", tempDirectory.toString());
            FileUtils.deleteDirectory(tempDirectory.toFile());
        } catch (IOException e) {
            LOG.error("Import: Error deleting: {}", tempDirectory.toString(), e);
        }
    }

    private boolean isZipFileEmpty() {
        return (numberOfEntries == 0);
    }

    private <T> T getJsonFromEntry(String entryName, Class<T> clazz) throws AtlasBaseException {
        final File file = getFileFromTemporaryDirectory(entryName + EXT_JSON);
        if (!file.exists()) {
            throw new AtlasBaseException(entryName + " not found!");
        }

        return convertFromJson(clazz, getJsonStringForFile(file));
    }

    private void setupBackingStore(InputStream inputStream, String backingDirectory) throws AtlasBaseException, IOException {
        initTempDirectory(backingDirectory);
        unzipToTempDirectory(inputStream);
        setupIterator();
    }

    private void initTempDirectory(String backingDirectory) throws AtlasBaseException {
        try {
            tempDirectory = Files.createDirectory(Paths.get(backingDirectory, getChildDirectoryForSession()));
            if (!permissionChecks(tempDirectory.toFile())) {
                throw new AtlasBaseException(
                        String.format("Import: Temporary directory: %s does not have permissions for operation!", tempDirectory.toString()));
            }
        }
        catch(Exception ex) {
            throw new AtlasBaseException(String.format("Error fetching temporary directory: %s", tempDirectory.toString()), ex);
        }
    }

    private String getChildDirectoryForSession() {
        return String.format("%s%s", TEMPORARY_DIRECTORY_PREFIX, UUID.randomUUID());
    }

    private boolean permissionChecks(File f) {
        return f.exists() && f.isDirectory() && f.canWrite();
    }

    private void unzipToTempDirectory(InputStream inputStream) throws IOException {
        LOG.info("Import: Temporary directory: {}", tempDirectory.toString());

        ZipInputStream zis = new ZipInputStream(inputStream);
        try {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                String entryName = zipEntry.getName();

                writeJsonToFile(entryName,  getJsonPayloadFromZipEntryStream(zis));
                numberOfEntries++;

                zipEntry = zis.getNextEntry();
            }

            numberOfEntries -= ZipExportFileNames.values().length;
        }
        finally {
            zis.close();
            inputStream.close();
        }
    }

    private void writeJsonToFile(String entryName, byte[] jsonPayload) throws IOException {
        File f = getFileFromTemporaryDirectory(entryName);
        Files.write(f.toPath(), jsonPayload);
    }

    private File getFileFromTemporaryDirectory(String entryName) {
        return new File(tempDirectory.toFile(), entryName);
    }

    private void setupIterator() {
        try {
            creationOrder = getJsonFromEntry(ZipExportFileNames.ATLAS_EXPORT_ORDER_NAME.toString(), ArrayList.class);
        } catch (AtlasBaseException e) {
            LOG.error("Error fetching: {}. Error generating order.", ZipExportFileNames.ATLAS_EXPORT_ORDER_NAME.toString(), e);
        }

        reset();
    }

    private byte[] getJsonPayloadFromZipEntryStream(ZipInputStream zipInputStream) {
        try {
            byte[] buf = new byte[1024];

            int n = 0;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while ((n = zipInputStream.read(buf, 0, 1024)) > -1) {
                bos.write(buf, 0, n);
            }

            return bos.toByteArray();
        } catch (IOException ex) {
            LOG.error("Error fetching string from entry.", ex);
        }

        return null;
    }

    private String getJsonStringForFile(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return new String(bytes);
        } catch (IOException e) {
            LOG.warn("Error fetching: {}", file.toString(), e);
            return null;
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
        return numberOfEntries;
    }

    private String moveNext() {
        if (currentPosition < numberOfEntries) {
            return creationOrder.get(currentPosition++);
        }

        return null;
    }
}
