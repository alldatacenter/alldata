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
package org.apache.atlas.repository.store.bootstrap;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.AtlasException;
import org.apache.atlas.RequestContext;
import org.apache.atlas.authorize.AtlasAuthorizerFactory;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.ha.HAConfiguration;
import org.apache.atlas.listener.ActiveStateChangeHandler;
import org.apache.atlas.model.TypeCategory;
import org.apache.atlas.model.patches.AtlasPatch.PatchStatus;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasBusinessMetadataDef;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasEnumDef.AtlasEnumElementDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef.RelationshipCategory;
import org.apache.atlas.model.typedef.AtlasRelationshipEndDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graph.GraphBackedSearchIndexer;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.patches.AddMandatoryAttributesPatch;
import org.apache.atlas.repository.patches.SuperTypesUpdatePatch;
import org.apache.atlas.repository.patches.AtlasPatchManager;
import org.apache.atlas.repository.patches.AtlasPatchRegistry;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;
import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.APPLIED;
import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.FAILED;
import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.SKIPPED;
import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.UNKNOWN;

/**
 * Class that handles initial loading of models and patches into typedef store
 */
@Service
@Order(2)
public class AtlasTypeDefStoreInitializer implements ActiveStateChangeHandler {
    public static final Logger LOG                    = LoggerFactory.getLogger(AtlasTypeDefStoreInitializer.class);
    public static final String PATCHES_FOLDER_NAME    = "patches";
    public static final String RELATIONSHIP_LABEL     = "relationshipLabel";
    public static final String RELATIONSHIP_CATEGORY  = "relationshipCategory";
    public static final String RELATIONSHIP_SWAP_ENDS = "swapEnds";
    public static final String TYPEDEF_PATCH_TYPE     = "TYPEDEF_PATCH";

    private final AtlasTypeDefStore typeDefStore;
    private final AtlasTypeRegistry typeRegistry;
    private final Configuration     conf;
    private final AtlasGraph        graph;
    private final AtlasPatchManager patchManager;

    @Inject
    public AtlasTypeDefStoreInitializer(AtlasTypeDefStore typeDefStore, AtlasTypeRegistry typeRegistry,
                                        AtlasGraph graph, Configuration conf, AtlasPatchManager patchManager) {
        this.typeDefStore  = typeDefStore;
        this.typeRegistry  = typeRegistry;
        this.conf          = conf;
        this.graph         = graph;
        this.patchManager  = patchManager;
    }

    @PostConstruct
    public void init() {
        LOG.info("==> AtlasTypeDefStoreInitializer.init()");

        if (!HAConfiguration.isHAEnabled(conf)) {
            startInternal();
        } else {
            LOG.info("AtlasTypeDefStoreInitializer.init(): deferring type loading until instance activation");
        }

        LOG.info("<== AtlasTypeDefStoreInitializer.init()");
    }

    /**
     * This method is looking for folders in alphabetical order in the models directory. It loads each of these folders and their associated patches in order.
     * It then loads any models in the top level folder and its patches.
     *
     * This allows models to be grouped into folders to help managability.
     *
     */
    private void loadBootstrapTypeDefs() {
        LOG.info("==> AtlasTypeDefStoreInitializer.loadBootstrapTypeDefs()");

        String atlasHomeDir  = System.getProperty("atlas.home");
        String modelsDirName = (StringUtils.isEmpty(atlasHomeDir) ? "." : atlasHomeDir) + File.separator + "models";

        if (modelsDirName == null || modelsDirName.length() == 0) {
            LOG.info("Types directory {} does not exist or not readable or has no typedef files", modelsDirName);
        } else {
            // look for folders we need to load models from
            File               topModeltypesDir  = new File(modelsDirName);
            File[]             modelsDirContents = topModeltypesDir.exists() ? topModeltypesDir.listFiles() : null;
            AtlasPatchRegistry patchRegistry     = new AtlasPatchRegistry(graph);

            if (modelsDirContents != null && modelsDirContents.length > 0) {
	            Arrays.sort(modelsDirContents);

	            for (File folder : modelsDirContents) {
	                    if (folder.isFile()) {
	                        // ignore files
	                        continue;
	                    } else if (!folder.getName().equals(PATCHES_FOLDER_NAME)){
	                        // load the models alphabetically in the subfolders apart from patches
	                        loadModelsInFolder(folder, patchRegistry);
	                    }
	            }
            }

            // load any files in the top models folder and any associated patches.
            loadModelsInFolder(topModeltypesDir, patchRegistry);
        }

        LOG.info("<== AtlasTypeDefStoreInitializer.loadBootstrapTypeDefs()");
    }

    /**
     * Load all the model files in the supplied folder followed by the contents of the patches folder.
     * @param typesDir
     */
    private void loadModelsInFolder(File typesDir, AtlasPatchRegistry patchRegistry) {
        LOG.info("==> AtlasTypeDefStoreInitializer({})", typesDir);

        String typesDirName = typesDir.getName();
        File[] typeDefFiles = typesDir.exists() ? typesDir.listFiles() : null;

        if (typeDefFiles == null || typeDefFiles.length == 0) {
            LOG.info("Types directory {} does not exist or not readable or has no typedef files", typesDirName );
        } else {
            // sort the files by filename
            Arrays.sort(typeDefFiles);

            for (File typeDefFile : typeDefFiles) {
                if (typeDefFile.isFile()) {
                    try {
                        String        jsonStr  = new String(Files.readAllBytes(typeDefFile.toPath()), StandardCharsets.UTF_8);
                        AtlasTypesDef typesDef = AtlasType.fromJson(jsonStr, AtlasTypesDef.class);

                        if (typesDef == null || typesDef.isEmpty()) {
                            LOG.info("No type in file {}", typeDefFile.getAbsolutePath());

                            continue;
                        }

                        AtlasTypesDef typesToCreate = getTypesToCreate(typesDef, typeRegistry);
                        AtlasTypesDef typesToUpdate = getTypesToUpdate(typesDef, typeRegistry, true);

                        if (!typesToCreate.isEmpty() || !typesToUpdate.isEmpty()) {
                            typeDefStore.createUpdateTypesDef(typesToCreate, typesToUpdate);

                            LOG.info("Created/Updated types defined in file {}", typeDefFile.getAbsolutePath());
                        } else {
                            LOG.info("No new type in file {}", typeDefFile.getAbsolutePath());
                        }

                    } catch (Throwable t) {
                        LOG.error("error while registering types in file {}", typeDefFile.getAbsolutePath(), t);
                    }
                }
            }

            applyTypePatches(typesDir.getPath(), patchRegistry);
        }
        LOG.info("<== AtlasTypeDefStoreInitializer({})", typesDir);
    }

    public static AtlasTypesDef getTypesToCreate(AtlasTypesDef typesDef, AtlasTypeRegistry typeRegistry) {
        AtlasTypesDef typesToCreate = new AtlasTypesDef();

        if (CollectionUtils.isNotEmpty(typesDef.getEnumDefs())) {
            for (AtlasEnumDef enumDef : typesDef.getEnumDefs()) {
                if (!typeRegistry.isRegisteredType(enumDef.getName())) {
                    typesToCreate.getEnumDefs().add(enumDef);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getStructDefs())) {
            for (AtlasStructDef structDef : typesDef.getStructDefs()) {
                if (!typeRegistry.isRegisteredType(structDef.getName())) {
                    typesToCreate.getStructDefs().add(structDef);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getClassificationDefs())) {
            for (AtlasClassificationDef classificationDef : typesDef.getClassificationDefs()) {
                if (!typeRegistry.isRegisteredType(classificationDef.getName())) {
                    typesToCreate.getClassificationDefs().add(classificationDef);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getEntityDefs())) {
            for (AtlasEntityDef entityDef : typesDef.getEntityDefs()) {
                if (!typeRegistry.isRegisteredType(entityDef.getName())) {
                    typesToCreate.getEntityDefs().add(entityDef);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getRelationshipDefs())) {
            for (AtlasRelationshipDef relationshipDef : typesDef.getRelationshipDefs()) {
                if (!typeRegistry.isRegisteredType(relationshipDef.getName())) {
                    typesToCreate.getRelationshipDefs().add(relationshipDef);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getBusinessMetadataDefs())) {
            for (AtlasBusinessMetadataDef businessMetadataDef : typesDef.getBusinessMetadataDefs()) {
                if (!typeRegistry.isRegisteredType(businessMetadataDef.getName())) {
                    typesToCreate.getBusinessMetadataDefs().add(businessMetadataDef);
                }
            }
        }

        return typesToCreate;
    }

    public static AtlasTypesDef getTypesToUpdate(AtlasTypesDef typesDef, AtlasTypeRegistry typeRegistry, boolean checkTypeVersion) {
        AtlasTypesDef typesToUpdate = new AtlasTypesDef();

        if (CollectionUtils.isNotEmpty(typesDef.getStructDefs())) {
            for (AtlasStructDef newStructDef : typesDef.getStructDefs()) {
                AtlasStructDef  oldStructDef = typeRegistry.getStructDefByName(newStructDef.getName());

                if (oldStructDef == null) {
                    continue;
                }

                if (updateTypeAttributes(oldStructDef, newStructDef, checkTypeVersion)) {
                    typesToUpdate.getStructDefs().add(newStructDef);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getClassificationDefs())) {
            for (AtlasClassificationDef newClassifDef : typesDef.getClassificationDefs()) {
                AtlasClassificationDef  oldClassifDef = typeRegistry.getClassificationDefByName(newClassifDef.getName());

                if (oldClassifDef == null) {
                    continue;
                }

                if (updateTypeAttributes(oldClassifDef, newClassifDef, checkTypeVersion)) {
                    typesToUpdate.getClassificationDefs().add(newClassifDef);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getEntityDefs())) {
            for (AtlasEntityDef newEntityDef : typesDef.getEntityDefs()) {
                AtlasEntityDef  oldEntityDef = typeRegistry.getEntityDefByName(newEntityDef.getName());

                if (oldEntityDef == null) {
                    continue;
                }

                if (updateTypeAttributes(oldEntityDef, newEntityDef, checkTypeVersion)) {
                    typesToUpdate.getEntityDefs().add(newEntityDef);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getEnumDefs())) {
            for (AtlasEnumDef newEnumDef : typesDef.getEnumDefs()) {
                AtlasEnumDef  oldEnumDef = typeRegistry.getEnumDefByName(newEnumDef.getName());

                if (oldEnumDef == null) {
                    continue;
                }

                if (isTypeUpdateApplicable(oldEnumDef, newEnumDef, checkTypeVersion)) {
                    if (CollectionUtils.isNotEmpty(oldEnumDef.getElementDefs())) {
                        for (AtlasEnumElementDef oldEnumElem : oldEnumDef.getElementDefs()) {
                            if (!newEnumDef.hasElement(oldEnumElem.getValue())) {
                                newEnumDef.addElement(oldEnumElem);
                            }
                        }
                    }

                    typesToUpdate.getEnumDefs().add(newEnumDef);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getRelationshipDefs())) {
            for (AtlasRelationshipDef relationshipDef : typesDef.getRelationshipDefs()) {
                AtlasRelationshipDef  oldRelationshipDef = typeRegistry.getRelationshipDefByName(relationshipDef.getName());

                if (oldRelationshipDef == null) {
                    continue;
                }

                if (updateTypeAttributes(oldRelationshipDef, relationshipDef, checkTypeVersion)) {
                    typesToUpdate.getRelationshipDefs().add(relationshipDef);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(typesDef.getBusinessMetadataDefs())) {
            for (AtlasBusinessMetadataDef businessMetadataDef : typesDef.getBusinessMetadataDefs()) {
                AtlasBusinessMetadataDef oldDef = typeRegistry.getBusinessMetadataDefByName(businessMetadataDef.getName());

                if (oldDef == null) {
                    continue;
                }

                if (updateTypeAttributes(oldDef, businessMetadataDef, checkTypeVersion)) {
                    typesToUpdate.getBusinessMetadataDefs().add(businessMetadataDef);
                }
            }
        }

        return typesToUpdate;
    }

    @Override
    public void instanceIsActive() {
        LOG.info("==> AtlasTypeDefStoreInitializer.instanceIsActive()");

        startInternal();

        LOG.info("<== AtlasTypeDefStoreInitializer.instanceIsActive()");
    }

    private void startInternal() {
        try {
            typeDefStore.init();
            loadBootstrapTypeDefs();
            typeDefStore.notifyLoadCompletion();
            try {
                AtlasAuthorizerFactory.getAtlasAuthorizer();
            } catch (Throwable t) {
                LOG.error("AtlasTypeDefStoreInitializer.instanceIsActive(): Unable to obtain AtlasAuthorizer", t);
            }
        } catch (AtlasBaseException e) {
            LOG.error("Failed to init after becoming active", e);
        } finally {
            RequestContext.clear();
        }
    }

    @Override
    public void instanceIsPassive() throws AtlasException {
        LOG.info("==> AtlasTypeDefStoreInitializer.instanceIsPassive()");

        LOG.info("<== AtlasTypeDefStoreInitializer.instanceIsPassive()");
    }

    @Override
    public int getHandlerOrder() {
        return HandlerOrder.TYPEDEF_STORE_INITIALIZER.getOrder();
    }

    private static boolean updateTypeAttributes(AtlasStructDef oldStructDef, AtlasStructDef newStructDef, boolean checkTypeVersion) {
        boolean ret = isTypeUpdateApplicable(oldStructDef, newStructDef, checkTypeVersion);

        if (ret) {
            // make sure that all attributes in oldDef are in newDef as well
            if (CollectionUtils.isNotEmpty(oldStructDef.getAttributeDefs())){
                for (AtlasAttributeDef oldAttrDef : oldStructDef.getAttributeDefs()) {
                    if (!newStructDef.hasAttribute(oldAttrDef.getName())) {
                        newStructDef.addAttribute(oldAttrDef);
                    }
                }
            }
        }

        return ret;
    }

    private static boolean isTypeUpdateApplicable(AtlasBaseTypeDef oldTypeDef, AtlasBaseTypeDef newTypeDef, boolean checkVersion) {
        boolean ret = true;

        if (checkVersion) {
            String oldTypeVersion = oldTypeDef.getTypeVersion();
            String newTypeVersion = newTypeDef.getTypeVersion();

            ret = ObjectUtils.compare(newTypeVersion, oldTypeVersion) > 0;
        }

        return ret;
    }

    private void applyTypePatches(String typesDirName, AtlasPatchRegistry patchRegistry) {
        String typePatchesDirName = typesDirName + File.separator + PATCHES_FOLDER_NAME;
        File   typePatchesDir     = new File(typePatchesDirName);
        File[] typePatchFiles     = typePatchesDir.exists() ? typePatchesDir.listFiles() : null;

        if (typePatchFiles == null || typePatchFiles.length == 0) {
            LOG.info("Type patches directory {} does not exist or not readable or has no patches", typePatchesDirName);
        } else {
            LOG.info("Type patches directory {} is being processed", typePatchesDirName);

            // sort the files by filename
            Arrays.sort(typePatchFiles);

            PatchHandler[] patchHandlers = new PatchHandler[] {
                    new UpdateEnumDefPatchHandler(typeDefStore, typeRegistry),
                    new AddAttributePatchHandler(typeDefStore, typeRegistry),
                    new UpdateAttributePatchHandler(typeDefStore, typeRegistry),
                    new RemoveLegacyRefAttributesPatchHandler(typeDefStore, typeRegistry),
                    new UpdateTypeDefOptionsPatchHandler(typeDefStore, typeRegistry),
                    new SetServiceTypePatchHandler(typeDefStore, typeRegistry),
                    new UpdateAttributeMetadataHandler(typeDefStore, typeRegistry),
                    new AddSuperTypePatchHandler(typeDefStore, typeRegistry),
                    new AddMandatoryAttributePatchHandler(typeDefStore, typeRegistry)
            };

            Map<String, PatchHandler> patchHandlerRegistry = new HashMap<>();

            for (PatchHandler patchHandler : patchHandlers) {
                for (String supportedAction : patchHandler.getSupportedActions()) {
                    patchHandlerRegistry.put(supportedAction, patchHandler);
                }
            }

            for (File typePatchFile : typePatchFiles) {
                if (typePatchFile.isFile()) {
                    String patchFile = typePatchFile.getAbsolutePath();

                    LOG.info("Applying patches in file {}", patchFile);

                    try {
                        String         jsonStr = new String(Files.readAllBytes(typePatchFile.toPath()), StandardCharsets.UTF_8);
                        TypeDefPatches patches = AtlasType.fromJson(jsonStr, TypeDefPatches.class);

                        if (patches == null || CollectionUtils.isEmpty(patches.getPatches())) {
                            LOG.info("No patches in file {}", patchFile);

                            continue;
                        }

                        int patchIndex = 0;
                        for (TypeDefPatch patch : patches.getPatches()) {
                            PatchHandler patchHandler = patchHandlerRegistry.get(patch.getAction());

                            if (patchHandler == null) {
                                LOG.error("Unknown patch action {} in file {}. Ignored", patch.getAction(), patchFile);
                                continue;
                            }

                            if (patchRegistry.isApplicable(patch.getId(), patchFile, patchIndex++)) {
                                PatchStatus status;

                                try {
                                    status = patchHandler.applyPatch(patch);
                                } catch (AtlasBaseException ex) {
                                    status = FAILED;

                                    LOG.error("Failed to apply {} (status: {}; action: {}) in file: {}. Ignored.",
                                               patch.getId(), status.toString(), patch.getAction(), patchFile);
                                }

                                patchRegistry.register(patch.id, patch.description, TYPEDEF_PATCH_TYPE, patch.action, status);
                                LOG.info("{} (status: {}; action: {}) in file: {}", patch.getId(), status.toString(), patch.getAction(), patchFile);
                            } else {
                                LOG.info("{} in file: {} already {}. Ignoring.", patch.getId(), patchFile, patchRegistry.getStatus(patch.getId()).toString());
                            }
                        }
                    } catch (Throwable t) {
                        LOG.error("Failed to apply patches in file {}. Ignored", patchFile, t);
                    }
                }
            }
        }
    }

    /**
     * typedef patch details
     */
    @JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    static class TypeDefPatch {
        private String                  id;
        private String                  description;
        private String                  action;
        private String                  typeName;
        private String                  applyToVersion;
        private String                  updateToVersion;
        private Map<String, Object>     params;
        private List<AtlasAttributeDef> attributeDefs;
        private List<AtlasEnumElementDef> elementDefs;
        private Map<String, String>     typeDefOptions;
        private String                  serviceType;
        private String attributeName;
        private Set<String> superTypes;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public String getApplyToVersion() {
            return applyToVersion;
        }

        public void setApplyToVersion(String applyToVersion) {
            this.applyToVersion = applyToVersion;
        }

        public String getUpdateToVersion() {
            return updateToVersion;
        }

        public void setUpdateToVersion(String updateToVersion) {
            this.updateToVersion = updateToVersion;
        }

        public Map<String, Object> getParams() {
            return params;
        }

        public void setParams(Map<String, Object> params) {
            this.params = params;
        }

        public List<AtlasAttributeDef> getAttributeDefs() {
            return attributeDefs;
        }

        public void setAttributeDefs(List<AtlasAttributeDef> attributeDefs) {
            this.attributeDefs = attributeDefs;
        }

        public List<AtlasEnumElementDef> getElementDefs() {
            return elementDefs;
        }

        public void setElementDefs(List<AtlasEnumElementDef> elementDefs) {
            this.elementDefs = elementDefs;
        }

        public Map<String, String> getTypeDefOptions() {
            return typeDefOptions;
        }

        public void setTypeDefOptions(Map<String, String> typeDefOptions) {
            this.typeDefOptions = typeDefOptions;
        }

        public String getServiceType() {
            return serviceType;
        }

        public void setServiceType(String serviceType) {
            this.serviceType = serviceType;
        }

        public String getAttributeName() { return attributeName; }

        public void setAttributeName(String attributeName) { this.attributeName = attributeName; }

        public Set<String> getSuperTypes() {
            return superTypes;
        }

        public void setSuperTypes(Set<String> superTypes) {
            this.superTypes = superTypes;
        }
    }

    /**
     * list of typedef patches
     */
    @JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    static class TypeDefPatches {
        private List<TypeDefPatch> patches;

        public List<TypeDefPatch> getPatches() {
            return patches;
        }

        public void setPatches(List<TypeDefPatch> patches) {
            this.patches = patches;
        }
    }

    abstract class PatchHandler {
        protected final AtlasTypeDefStore typeDefStore;
        protected final AtlasTypeRegistry typeRegistry;
        protected final String[]          supportedActions;

        protected PatchHandler(AtlasTypeDefStore typeDefStore, AtlasTypeRegistry typeRegistry, String[] supportedActions) {
            this.typeDefStore     = typeDefStore;
            this.typeRegistry     = typeRegistry;
            this.supportedActions = supportedActions;
        }

        public String[] getSupportedActions() { return supportedActions; }

        public abstract PatchStatus applyPatch(TypeDefPatch patch) throws AtlasBaseException;

        protected boolean isPatchApplicable(TypeDefPatch patch, AtlasBaseTypeDef currentTypeDef) {
            String currentVersion = currentTypeDef.getTypeVersion();
            String applyToVersion = patch.getApplyToVersion();

            return currentVersion == null ||
                   currentVersion.equalsIgnoreCase(applyToVersion) ||
                   currentVersion.startsWith(applyToVersion + ".");
        }
    }

    class UpdateEnumDefPatchHandler extends PatchHandler {
        public UpdateEnumDefPatchHandler(AtlasTypeDefStore typeDefStore, AtlasTypeRegistry typeRegistry) {
            super(typeDefStore, typeRegistry, new String[]{"UPDATE_ENUMDEF"});
        }

        @Override
        public PatchStatus applyPatch(TypeDefPatch patch) throws AtlasBaseException {
            String typeName = patch.getTypeName();
            AtlasBaseTypeDef typeDef = typeRegistry.getTypeDefByName(typeName);
            PatchStatus ret;
            if (typeDef == null) {
                throw new AtlasBaseException(AtlasErrorCode.PATCH_FOR_UNKNOWN_TYPE, patch.getAction(), typeName);
            }
            if (isPatchApplicable(patch, typeDef)) {
                if (typeDef.getClass().equals(AtlasEnumDef.class)) {
                    AtlasEnumDef updatedDef = new AtlasEnumDef((AtlasEnumDef) typeDef);

                    for (AtlasEnumElementDef elementDef : patch.getElementDefs()) {
                        updatedDef.addElement(elementDef);
                    }
                    updatedDef.setTypeVersion(patch.getUpdateToVersion());
                    typeDefStore.updateEnumDefByName(typeName, updatedDef);
                    ret = APPLIED;
                } else {
                    throw new AtlasBaseException(AtlasErrorCode.PATCH_NOT_APPLICABLE_FOR_TYPE, patch.getAction(), typeDef.getClass().getSimpleName());
                }
            } else {
                LOG.info("patch skipped: typeName={}; applyToVersion={}; updateToVersion={}",
                        patch.getTypeName(), patch.getApplyToVersion(), patch.getUpdateToVersion());
                ret = SKIPPED;
            }
            return ret;
        }
    }

    class AddAttributePatchHandler extends PatchHandler {
        public AddAttributePatchHandler(AtlasTypeDefStore typeDefStore, AtlasTypeRegistry typeRegistry) {
            super(typeDefStore, typeRegistry, new String[] { "ADD_ATTRIBUTE" });
        }

            @Override
            public PatchStatus applyPatch(TypeDefPatch patch) throws AtlasBaseException {
            String           typeName       = patch.getTypeName();
            AtlasBaseTypeDef typeDef        = typeRegistry.getTypeDefByName(typeName);
            PatchStatus      ret;

            if (typeDef == null) {
                throw new AtlasBaseException(AtlasErrorCode.PATCH_FOR_UNKNOWN_TYPE, patch.getAction(), typeName);
            }

            if (isPatchApplicable(patch, typeDef)) {
                if (typeDef.getClass().equals(AtlasEntityDef.class)) {
                    AtlasEntityDef updatedDef = new AtlasEntityDef((AtlasEntityDef)typeDef);

                    for (AtlasAttributeDef attributeDef : patch.getAttributeDefs()) {
                        updatedDef.addAttribute(attributeDef);
                    }

                    updatedDef.setTypeVersion(patch.getUpdateToVersion());

                    typeDefStore.updateEntityDefByName(typeName, updatedDef);

                    ret = APPLIED;
                } else if (typeDef.getClass().equals(AtlasClassificationDef.class)) {
                    AtlasClassificationDef updatedDef = new AtlasClassificationDef((AtlasClassificationDef)typeDef);

                    for (AtlasAttributeDef attributeDef : patch.getAttributeDefs()) {
                        updatedDef.addAttribute(attributeDef);
                    }

                    updatedDef.setTypeVersion(patch.getUpdateToVersion());

                    typeDefStore.updateClassificationDefByName(typeName, updatedDef);

                    ret = APPLIED;
                } else if (typeDef.getClass().equals(AtlasStructDef.class)) {
                    AtlasStructDef updatedDef = new AtlasStructDef((AtlasStructDef)typeDef);

                    for (AtlasAttributeDef attributeDef : patch.getAttributeDefs()) {
                        updatedDef.addAttribute(attributeDef);
                    }

                    updatedDef.setTypeVersion(patch.getUpdateToVersion());

                    typeDefStore.updateStructDefByName(typeName, updatedDef);

                    ret = APPLIED;
                } else {
                    throw new AtlasBaseException(AtlasErrorCode.PATCH_NOT_APPLICABLE_FOR_TYPE, patch.getAction(), typeDef.getClass().getSimpleName());
                }
            } else {
                LOG.info("patch skipped: typeName={}; applyToVersion={}; updateToVersion={}",
                          patch.getTypeName(), patch.getApplyToVersion(), patch.getUpdateToVersion());

                ret = SKIPPED;

            }

            return ret;
        }
    }

    class AddMandatoryAttributePatchHandler extends PatchHandler {
        public AddMandatoryAttributePatchHandler(AtlasTypeDefStore typeDefStore, AtlasTypeRegistry typeRegistry) {
            super(typeDefStore, typeRegistry, new String[] { Constants.TYPEDEF_PATCH_ADD_MANDATORY_ATTRIBUTE });
        }

        @Override
        public PatchStatus applyPatch(TypeDefPatch patch) throws AtlasBaseException {
            String           typeName  = patch.getTypeName();
            AtlasBaseTypeDef typeDef   = typeRegistry.getTypeDefByName(typeName);
            PatchStatus      ret;

            if (typeDef == null) {
                throw new AtlasBaseException(AtlasErrorCode.PATCH_FOR_UNKNOWN_TYPE, patch.getAction(), typeName);
            }

            if (isPatchApplicable(patch, typeDef)) {
                List<AtlasAttributeDef> attributesToAdd = getAttributesToAdd(patch, (AtlasStructDef) typeDef);

                if (CollectionUtils.isEmpty(attributesToAdd)) {
                    LOG.info("patch skipped: typeName={}; mandatory attributes are not valid in patch {}",patch.getTypeName(), patch.getId());

                    ret = SKIPPED;
                } else {
                    try {
                        RequestContext.get().setInTypePatching(true);

                        RequestContext.get().setCurrentTypePatchAction(Constants.TYPEDEF_PATCH_ADD_MANDATORY_ATTRIBUTE);

                        if (typeDef.getClass().equals(AtlasEntityDef.class)) {
                            AtlasEntityDef updatedDef = new AtlasEntityDef((AtlasEntityDef) typeDef);

                            updateTypeDefWithPatch(patch, updatedDef, attributesToAdd);

                            typeDefStore.updateEntityDefByName(typeName, updatedDef);
                        } else if (typeDef.getClass().equals(AtlasClassificationDef.class)) {
                            AtlasClassificationDef updatedDef = new AtlasClassificationDef((AtlasClassificationDef) typeDef);

                            updateTypeDefWithPatch(patch, updatedDef, attributesToAdd);

                            typeDefStore.updateClassificationDefByName(typeName, updatedDef);
                        } else if (typeDef.getClass().equals(AtlasStructDef.class)) {
                            AtlasStructDef updatedDef = new AtlasStructDef((AtlasStructDef) typeDef);

                            updateTypeDefWithPatch(patch, updatedDef, attributesToAdd);

                            typeDefStore.updateStructDefByName(typeName, updatedDef);
                        } else {
                            throw new AtlasBaseException(AtlasErrorCode.PATCH_NOT_APPLICABLE_FOR_TYPE, patch.getAction(), typeDef.getClass().getSimpleName());
                        }

                        LOG.info("adding a Java patch to update entities of {} with new mandatory attributes", typeName);

                        // Java patch handler to add mandatory attributes
                        patchManager.addPatchHandler(new AddMandatoryAttributesPatch(patchManager.getContext(), patch.getId(), typeName, attributesToAdd));

                        ret = APPLIED;
                    } finally {
                        RequestContext.get().setInTypePatching(false);

                        RequestContext.clear();
                    }
                }
            } else {
                LOG.info("patch skipped: typeName={}; applyToVersion={}; updateToVersion={}",
                        patch.getTypeName(), patch.getApplyToVersion(), patch.getUpdateToVersion());

                ret = SKIPPED;

            }

            return ret;
        }

        // Validate mandatory attribute with non-empty default value if PRIMITIVE, not unique and doesn't exists
        private List<AtlasAttributeDef> getAttributesToAdd(TypeDefPatch patch, AtlasStructDef updatedDef) throws AtlasBaseException {
            List<AtlasAttributeDef> ret = new ArrayList<>();

            for (AtlasAttributeDef attributeDef : patch.getAttributeDefs()) {
                TypeCategory attributeType = typeRegistry.getType(attributeDef.getTypeName()).getTypeCategory();

                if (updatedDef.hasAttribute(attributeDef.getName())) {
                    LOG.warn("AddMandatoryAttributePatchHandler(id={}, typeName={}, attribute={}): already exists in type {}. Ignoring attribute", patch.getId(), patch.getTypeName(), attributeDef.getName(), updatedDef.getName());
                } else if (attributeDef.getIsOptional()) {
                    LOG.warn("AddMandatoryAttributePatchHandler(id={}, typeName={}, attribute={}): is not mandatory attribute. Ignoring attribute", patch.getId(), patch.getTypeName(), attributeDef.getName());
                } else if (StringUtils.isEmpty(attributeDef.getDefaultValue())) {
                    LOG.warn("AddMandatoryAttributePatchHandler(id={}, typeName={}, attribute={}): default value is missing. Ignoring attribute", patch.getId(), patch.getTypeName(), attributeDef.getName());
                } else if (!TypeCategory.PRIMITIVE.equals(attributeType)) {
                    LOG.warn("AddMandatoryAttributePatchHandler(id={}, typeName={}, attribute={}): type {} is not primitive. Ignoring attribute", patch.getId(), patch.getTypeName(), attributeDef.getName(), attributeDef.getTypeName());
                } else if (attributeDef.getIsUnique()) {
                    LOG.warn("AddMandatoryAttributePatchHandler(id={}, typeName={}, attribute={}): is not unique. Ignoring attribute", patch.getId(), patch.getTypeName(), attributeDef.getName());
                } else {
                    ret.add(attributeDef);
                }
            }

            return ret;
        }

        private void updateTypeDefWithPatch(TypeDefPatch patch, AtlasStructDef updatedDef, List<AtlasAttributeDef> attributesToAdd) {
            for (AtlasAttributeDef attributeDef : attributesToAdd) {
                updatedDef.addAttribute(attributeDef);
            }

            updatedDef.setTypeVersion(patch.getUpdateToVersion());
        }
    }

    class UpdateAttributePatchHandler extends PatchHandler {
        public UpdateAttributePatchHandler(AtlasTypeDefStore typeDefStore, AtlasTypeRegistry typeRegistry) {
            super(typeDefStore, typeRegistry, new String[] { "UPDATE_ATTRIBUTE" });
        }

        @Override
        public PatchStatus applyPatch(TypeDefPatch patch) throws AtlasBaseException {
            String           typeName       = patch.getTypeName();
            AtlasBaseTypeDef typeDef        = typeRegistry.getTypeDefByName(typeName);
            PatchStatus      ret;

            if (typeDef == null) {
                throw new AtlasBaseException(AtlasErrorCode.PATCH_FOR_UNKNOWN_TYPE, patch.getAction(), typeName);
            }

            if (isPatchApplicable(patch, typeDef)) {
                if (typeDef.getClass().equals(AtlasEntityDef.class)) {
                    AtlasEntityDef updatedDef = new AtlasEntityDef((AtlasEntityDef)typeDef);

                    addOrUpdateAttributes(updatedDef, patch.getAttributeDefs());

                    updatedDef.setTypeVersion(patch.getUpdateToVersion());

                    typeDefStore.updateEntityDefByName(typeName, updatedDef);

                    ret = APPLIED;
                } else if (typeDef.getClass().equals(AtlasClassificationDef.class)) {
                    AtlasClassificationDef updatedDef = new AtlasClassificationDef((AtlasClassificationDef)typeDef);

                    addOrUpdateAttributes(updatedDef, patch.getAttributeDefs());

                    updatedDef.setTypeVersion(patch.getUpdateToVersion());

                    typeDefStore.updateClassificationDefByName(typeName, updatedDef);

                    ret = APPLIED;
                } else if (typeDef.getClass().equals(AtlasStructDef.class)) {
                    AtlasStructDef updatedDef = new AtlasStructDef((AtlasStructDef)typeDef);

                    addOrUpdateAttributes(updatedDef, patch.getAttributeDefs());

                    updatedDef.setTypeVersion(patch.getUpdateToVersion());

                    typeDefStore.updateStructDefByName(typeName, updatedDef);

                    ret = APPLIED;
                } else {
                    throw new AtlasBaseException(AtlasErrorCode.PATCH_NOT_APPLICABLE_FOR_TYPE, patch.getAction(), typeDef.getClass().getSimpleName());
                }
            } else {
                LOG.info("patch skipped: typeName={}; applyToVersion={}; updateToVersion={}",
                          patch.getTypeName(), patch.getApplyToVersion(), patch.getUpdateToVersion());

                ret = SKIPPED;
            }

            return ret;
        }

        private void addOrUpdateAttributes(AtlasStructDef structDef, List<AtlasAttributeDef> attributesToUpdate) {
            for (AtlasAttributeDef attributeToUpdate : attributesToUpdate) {
                String attrName = attributeToUpdate.getName();

                if (structDef.hasAttribute(attrName)) {
                    structDef.removeAttribute(attrName);
                }

                structDef.addAttribute(attributeToUpdate);
            }
        }
    }

    class RemoveLegacyRefAttributesPatchHandler extends PatchHandler {
        public RemoveLegacyRefAttributesPatchHandler(AtlasTypeDefStore typeDefStore, AtlasTypeRegistry typeRegistry) {
            super(typeDefStore, typeRegistry, new String[] { "REMOVE_LEGACY_REF_ATTRIBUTES" });
        }

        @Override
        public PatchStatus applyPatch(TypeDefPatch patch) throws AtlasBaseException {
            String           typeName       = patch.getTypeName();
            AtlasBaseTypeDef typeDef        = typeRegistry.getTypeDefByName(typeName);
            PatchStatus      ret            = UNKNOWN;

            if (typeDef == null) {
                throw new AtlasBaseException(AtlasErrorCode.PATCH_FOR_UNKNOWN_TYPE, patch.getAction(), typeName);
            }

            if (isPatchApplicable(patch, typeDef)) {
                if (typeDef.getClass().equals(AtlasRelationshipDef.class)) {
                    AtlasRelationshipDef    relationshipDef = (AtlasRelationshipDef) typeDef;
                    AtlasRelationshipEndDef end1Def         = relationshipDef.getEndDef1();
                    AtlasRelationshipEndDef end2Def         = relationshipDef.getEndDef2();
                    AtlasEntityType         end1Type        = typeRegistry.getEntityTypeByName(end1Def.getType());
                    AtlasEntityType         end2Type        = typeRegistry.getEntityTypeByName(end2Def.getType());

                    String               newRelationshipLabel    = null;
                    RelationshipCategory newRelationshipCategory = null;
                    boolean              swapEnds                = false;

                    if (patch.getParams() != null) {
                        Object relLabel    = patch.getParams().get(RELATIONSHIP_LABEL);
                        Object relCategory = patch.getParams().get(RELATIONSHIP_CATEGORY);
                        Object relSwapEnds = patch.getParams().get(RELATIONSHIP_SWAP_ENDS);

                        if (relLabel != null) {
                            newRelationshipLabel = relLabel.toString();
                        }

                        if (relCategory != null) {
                            newRelationshipCategory = RelationshipCategory.valueOf(relCategory.toString());
                        }

                        if (relSwapEnds != null) {
                            swapEnds = Boolean.valueOf(relSwapEnds.toString());
                        }
                    }

                    if (StringUtils.isEmpty(newRelationshipLabel)) {
                        if (end1Def.getIsLegacyAttribute()) {
                            if (!end2Def.getIsLegacyAttribute()) {
                                AtlasAttribute legacyAttribute = end1Type.getAttribute(end1Def.getName());

                                newRelationshipLabel = "__" + legacyAttribute.getQualifiedName();
                            } else { // if both ends are legacy attributes, RELATIONSHIP_LABEL should be specified in the patch
                                throw new AtlasBaseException(AtlasErrorCode.PATCH_MISSING_RELATIONSHIP_LABEL, patch.getAction(), typeName);
                            }
                        } else if (end2Def.getIsLegacyAttribute()) {
                            AtlasAttribute legacyAttribute = end2Type.getAttribute(end2Def.getName());

                            newRelationshipLabel = "__" + legacyAttribute.getQualifiedName();
                        } else {
                            newRelationshipLabel = relationshipDef.getRelationshipLabel();
                        }
                    }

                    AtlasRelationshipDef updatedDef = new AtlasRelationshipDef(relationshipDef);

                    if (swapEnds) {
                        AtlasRelationshipEndDef tmp = updatedDef.getEndDef1();

                        updatedDef.setEndDef1(updatedDef.getEndDef2());
                        updatedDef.setEndDef2(tmp);
                    }

                    end1Def  = updatedDef.getEndDef1();
                    end2Def  = updatedDef.getEndDef2();
                    end1Type = typeRegistry.getEntityTypeByName(end1Def.getType());
                    end2Type = typeRegistry.getEntityTypeByName(end2Def.getType());

                    updatedDef.setRelationshipLabel(newRelationshipLabel);

                    if (newRelationshipCategory != null) {
                        updatedDef.setRelationshipCategory(newRelationshipCategory);
                    }

                    updatedDef.setTypeVersion(patch.getUpdateToVersion());

                    AtlasEntityDef updatedEntityDef1 = new AtlasEntityDef(end1Type.getEntityDef());
                    AtlasEntityDef updatedEntityDef2 = new AtlasEntityDef(end2Type.getEntityDef());

                    updatedEntityDef1.removeAttribute(end1Def.getName());
                    updatedEntityDef2.removeAttribute(end2Def.getName());

                    AtlasTypesDef typesDef = new AtlasTypesDef();

                    typesDef.setEntityDefs(Arrays.asList(updatedEntityDef1, updatedEntityDef2));
                    typesDef.setRelationshipDefs(Collections.singletonList(updatedDef));

                    try {
                        RequestContext.get().setInTypePatching(true); // to allow removal of attributes

                        typeDefStore.updateTypesDef(typesDef);

                        ret = APPLIED;
                    } finally {
                        RequestContext.get().setInTypePatching(false);
                        RequestContext.clear();
                    }
                }
            } else {
                LOG.info("patch skipped: typeName={}; applyToVersion={}; updateToVersion={}",
                         patch.getTypeName(), patch.getApplyToVersion(), patch.getUpdateToVersion());

                ret = SKIPPED;
            }

            return ret;
        }
    }

    class UpdateTypeDefOptionsPatchHandler extends PatchHandler {
        public UpdateTypeDefOptionsPatchHandler(AtlasTypeDefStore typeDefStore, AtlasTypeRegistry typeRegistry) {
            super(typeDefStore, typeRegistry, new String[] { "UPDATE_TYPEDEF_OPTIONS" });
        }

        @Override
        public PatchStatus applyPatch(TypeDefPatch patch) throws AtlasBaseException {
            String           typeName = patch.getTypeName();
            AtlasBaseTypeDef typeDef  = typeRegistry.getTypeDefByName(typeName);
            PatchStatus      ret;

            if (typeDef == null) {
                throw new AtlasBaseException(AtlasErrorCode.PATCH_FOR_UNKNOWN_TYPE, patch.getAction(), typeName);
            }

            if (MapUtils.isEmpty(patch.getTypeDefOptions())) {
                throw new AtlasBaseException(AtlasErrorCode.PATCH_INVALID_DATA, patch.getAction(), typeName);
            }

            if (isPatchApplicable(patch, typeDef)) {
                if (typeDef.getOptions() == null) {
                    typeDef.setOptions(patch.getTypeDefOptions());
                } else {
                    typeDef.getOptions().putAll(patch.getTypeDefOptions());
                }
                typeDef.setTypeVersion(patch.getUpdateToVersion());

                typeDefStore.updateTypesDef(AtlasTypeUtil.getTypesDef(typeDef));

                ret = APPLIED;
            } else {
                LOG.info("patch skipped: typeName={}; applyToVersion={}; updateToVersion={}",
                         patch.getTypeName(), patch.getApplyToVersion(), patch.getUpdateToVersion());

                ret = SKIPPED;
            }

            return ret;
        }
    }

    class SetServiceTypePatchHandler extends PatchHandler {
        public SetServiceTypePatchHandler(AtlasTypeDefStore typeDefStore, AtlasTypeRegistry typeRegistry) {
            super(typeDefStore, typeRegistry, new String[] { "SET_SERVICE_TYPE" });
        }

        @Override
        public PatchStatus applyPatch(TypeDefPatch patch) throws AtlasBaseException {
            String           typeName       = patch.getTypeName();
            AtlasBaseTypeDef typeDef        = typeRegistry.getTypeDefByName(typeName);
            PatchStatus      ret;

            if (typeDef == null) {
                throw new AtlasBaseException(AtlasErrorCode.PATCH_FOR_UNKNOWN_TYPE, patch.getAction(), typeName);
            }

            if (isPatchApplicable(patch, typeDef)) {
                typeDef.setServiceType(patch.getServiceType());
                typeDef.setTypeVersion(patch.getUpdateToVersion());

                typeDefStore.updateTypesDef(AtlasTypeUtil.getTypesDef(typeDef));

                ret = APPLIED;
            } else {
                LOG.info("patch skipped: typeName={}; applyToVersion={}; updateToVersion={}", patch.getTypeName(),
                          patch.getApplyToVersion(), patch.getUpdateToVersion());

                ret = SKIPPED;
            }

            return ret;
        }
    }

    class UpdateAttributeMetadataHandler extends PatchHandler {
        public UpdateAttributeMetadataHandler(AtlasTypeDefStore typeDefStore, AtlasTypeRegistry typeRegistry) {
            super(typeDefStore, typeRegistry, new String[] { "UPDATE_ATTRIBUTE_METADATA" });
        }

        @Override
        public PatchStatus applyPatch(TypeDefPatch patch) throws AtlasBaseException {
            String           typeName       = patch.getTypeName();
            AtlasBaseTypeDef typeDef        = typeRegistry.getTypeDefByName(typeName);
            PatchStatus      ret;

            if (typeDef == null) {
                throw new AtlasBaseException(AtlasErrorCode.PATCH_FOR_UNKNOWN_TYPE, patch.getAction(), typeName);
            }

            if (isPatchApplicable(patch, typeDef)) {
                String attributeNameFromPatch = patch.getAttributeName();
                if (typeDef.getClass().equals(AtlasEntityDef.class)) {
                    AtlasEntityDef entityDef = new AtlasEntityDef((AtlasEntityDef)typeDef);

                    updateAttributeMetadata(patch, entityDef.getAttributeDefs());

                    entityDef.setTypeVersion(patch.getUpdateToVersion());

                    typeDefStore.updateEntityDefByName(typeName, entityDef);

                    ret = APPLIED;
                } else if (typeDef.getClass().equals(AtlasStructDef.class)) {
                    AtlasStructDef updatedDef = new AtlasStructDef((AtlasStructDef)typeDef);

                    updateAttributeMetadata(patch, updatedDef.getAttributeDefs());

                    updatedDef.setTypeVersion(patch.getUpdateToVersion());

                    typeDefStore.updateStructDefByName(typeName, updatedDef);

                    ret = APPLIED;
                } else {
                    throw new AtlasBaseException(AtlasErrorCode.PATCH_NOT_APPLICABLE_FOR_TYPE, patch.getAction(), typeDef.getClass().getSimpleName());
                }
            } else {
                LOG.info("patch skipped: typeName={}; applyToVersion={}; updateToVersion={}",
                        patch.getTypeName(), patch.getApplyToVersion(), patch.getUpdateToVersion());
                ret = SKIPPED;
            }

            return ret;
        }

        private void updateAttributeMetadata(TypeDefPatch patch, List<AtlasAttributeDef> attributeDefsFromEntity) {
            for(AtlasAttributeDef attributeDef: attributeDefsFromEntity) {
                if(attributeDef.getName().equalsIgnoreCase(patch.getAttributeName())) {
                    updateAttribute(attributeDef, patch.getParams());
                }
            }
        }

        private void updateAttribute(AtlasAttributeDef atlasAttributeDef, Map<String, Object> params) {
            if(!params.isEmpty()) {
                for(Map.Entry<String, Object> entry: params.entrySet()) {
                    try {
                        if (AtlasAttributeDef.SEARCH_WEIGHT_ATTR_NAME.equalsIgnoreCase(entry.getKey())) {
                            Number number = (Number) entry.getValue();
                            int searchWeight = number.intValue();
                            if(!GraphBackedSearchIndexer.isValidSearchWeight(number.intValue())) {
                                String msg = String.format("Invalid search weight '%d' was provided for property %s.", searchWeight, atlasAttributeDef.getName());
                                LOG.error(msg);
                                throw new RuntimeException(msg);
                            }
                            atlasAttributeDef.setSearchWeight(searchWeight);
                            LOG.info("Updating Model attribute {}'s property{} to {}.", atlasAttributeDef.getName(), entry.getKey(), entry.getValue());
                        } else if (AtlasAttributeDef.INDEX_TYPE_ATTR_NAME.equalsIgnoreCase(entry.getKey())) {
                            String indexTypeString = (String) entry.getValue();
                            if(!StringUtils.isEmpty(indexTypeString)) {
                                try {
                                    AtlasAttributeDef.IndexType indexType = AtlasAttributeDef.IndexType.valueOf(indexTypeString);
                                    atlasAttributeDef.setIndexType(indexType);
                                } catch (IllegalArgumentException e) {
                                    String msg = String.format("Value %s provided for the attribute %s is not valid.", indexTypeString, AtlasAttributeDef.INDEX_TYPE_ATTR_NAME);
                                    LOG.error(msg);
                                    throw new RuntimeException(msg);
                                }
                            }
                        } else {
                            //sanity exception
                            //more attributes can be added as needed.
                            String msg = String.format("Received unknown property{} for attribute {}'s ", entry.getKey(), atlasAttributeDef.getName());
                            LOG.error(msg);
                            throw new RuntimeException(msg);
                        }
                    } catch (Exception e) {
                        String msg = String.format("Error encountered in updating Model attribute %s's property '%s' to %s.", atlasAttributeDef.getName(), entry.getKey(), entry.getValue().toString());
                        LOG.info(msg, e);
                        throw new RuntimeException(msg, e);
                    }
                }
            }
        }
    }

    class AddSuperTypePatchHandler extends PatchHandler {

        public AddSuperTypePatchHandler(AtlasTypeDefStore typeDefStore, AtlasTypeRegistry typeRegistry) {
            super(typeDefStore, typeRegistry, new String[] {"ADD_SUPER_TYPES"});
        }

        @Override
        public PatchStatus applyPatch(TypeDefPatch patch) throws AtlasBaseException {
            String           typeName = patch.getTypeName();
            AtlasBaseTypeDef typeDef  = typeRegistry.getTypeDefByName(typeName);
            PatchStatus      ret;

            if (typeDef == null) {
                throw new AtlasBaseException(AtlasErrorCode.PATCH_FOR_UNKNOWN_TYPE, patch.getAction(), typeName);
            }

            Set<String> superTypesToBeAdded = patch.getSuperTypes();

            if (CollectionUtils.isNotEmpty(superTypesToBeAdded) && isPatchApplicable(patch, typeDef)) {
                if (typeDef.getClass().equals(AtlasEntityDef.class)) {
                    AtlasEntityDef updatedDef = new AtlasEntityDef((AtlasEntityDef)typeDef);

                    for (String superType : superTypesToBeAdded) {
                        updatedDef.addSuperType(superType);
                    }

                    updatedDef.setTypeVersion(patch.getUpdateToVersion());

                    typeDefStore.updateEntityDefByName(typeName, updatedDef);

                    LOG.info("Update entities of {} with new supertypes", typeName);

                    // add to java patch handlers to update entity supertypes
                    patchManager.addPatchHandler(new SuperTypesUpdatePatch(patchManager.getContext(), patch.getId(), typeName));

                    ret = APPLIED;
                } else {
                    throw new AtlasBaseException(AtlasErrorCode.PATCH_NOT_APPLICABLE_FOR_TYPE, patch.getAction(), typeDef.getClass().getSimpleName());
                }
            } else {
                if (CollectionUtils.isEmpty(superTypesToBeAdded)) {
                    LOG.info("patch skipped: No superTypes provided to add for typeName={}", patch.getTypeName());
                } else {
                    LOG.info("patch skipped: typeName={}; applyToVersion={}; updateToVersion={}",
                            patch.getTypeName(), patch.getApplyToVersion(), patch.getUpdateToVersion());
                }

                ret = SKIPPED;
            }

            return ret;
        }
    }
}
