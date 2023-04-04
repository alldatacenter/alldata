/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.server.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import datart.core.base.consts.Const;
import datart.core.base.consts.FileOwner;
import datart.core.base.exception.Exceptions;
import datart.core.base.exception.NotFoundException;
import datart.core.base.exception.ParamException;
import datart.core.common.*;
import datart.core.data.provider.DataProviderConfigTemplate;
import datart.core.data.provider.DataProviderSource;
import datart.core.data.provider.SchemaInfo;
import datart.core.data.provider.SchemaItem;
import datart.core.entity.Role;
import datart.core.entity.Source;
import datart.core.entity.SourceSchemas;
import datart.core.entity.ext.SourceDetail;
import datart.core.mappers.ext.RelRoleResourceMapperExt;
import datart.core.mappers.ext.SourceMapperExt;
import datart.core.mappers.ext.SourceSchemasMapperExt;
import datart.security.base.PermissionInfo;
import datart.security.base.ResourceType;
import datart.security.base.SubjectType;
import datart.security.exception.PermissionDeniedException;
import datart.security.manager.shiro.ShiroSecurityManager;
import datart.security.util.AESUtil;
import datart.security.util.PermissionHelper;
import datart.server.base.params.*;
import datart.server.base.transfer.ImportStrategy;
import datart.server.base.transfer.TransferConfig;
import datart.server.base.transfer.model.SourceResourceModel;
import datart.server.job.SchemaSyncJob;
import datart.server.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.quartz.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SourceServiceImpl extends BaseService implements SourceService {

    private static final String ENABLE_SYNC_SCHEMAS = "enableSyncSchemas";

    private static final String SYNC_INTERVAL = "syncInterval";

    private final SourceMapperExt sourceMapper;

    private final RoleService roleService;

    private final FileService fileService;

    private final Scheduler scheduler;

    private final SourceSchemasMapperExt sourceSchemasMapper;

    private final RelRoleResourceMapperExt rrrMapper;

    public SourceServiceImpl(SourceMapperExt sourceMapper,
                             RoleService roleService,
                             FileService fileService,
                             Scheduler scheduler,
                             SourceSchemasMapperExt sourceSchemasMapper,
                             RelRoleResourceMapperExt rrrMapper) {
        this.sourceMapper = sourceMapper;
        this.roleService = roleService;
        this.fileService = fileService;
        this.scheduler = scheduler;
        this.sourceSchemasMapper = sourceSchemasMapper;
        this.rrrMapper = rrrMapper;
    }

    @Override
    public boolean checkUnique(String orgId, String parentId, String name) {
        if (!CollectionUtils.isEmpty(sourceMapper.checkName(orgId, parentId, name))) {
            Exceptions.tr(ParamException.class, "error.param.exists.name");
        }
        return true;
    }

    @Override
    public List<Source> listSources(String orgId, boolean active) throws PermissionDeniedException {

        List<Source> sources = sourceMapper.listByOrg(orgId, active);

        Map<String, Source> filtered = new HashMap<>();

        List<Source> permitted = sources.stream().filter(source -> {
            try {
                requirePermission(source, Const.READ);
                return true;
            } catch (Exception e) {
                filtered.put(source.getId(), source);
                return false;
            }
        }).collect(Collectors.toList());

        while (!filtered.isEmpty()) {
            boolean updated = false;
            for (Source source : permitted) {
                Source parent = filtered.remove(source.getParentId());
                if (parent != null) {
                    permitted.add(parent);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                break;
            }
        }
        return permitted;
    }

    @Override
    public SchemaInfo getSourceSchemaInfo(String sourceId) {
        Source retrieve = retrieve(sourceId);
        requirePermission(retrieve, Const.READ);
        SourceSchemas sourceSchemas = sourceSchemasMapper.selectBySource(sourceId);
        if (sourceSchemas == null || StringUtils.isBlank(sourceSchemas.getSchemas())) {
            return SchemaInfo.empty();
        }
        SchemaInfo schemaInfo = new SchemaInfo();
        try {
            schemaInfo.setUpdateTime(sourceSchemas.getUpdateTime());
            if (StringUtils.isEmpty(sourceSchemas.getSchemas())) {
                return schemaInfo;
            }
            schemaInfo.setSchemaItems(OBJECT_MAPPER.readerForListOf(SchemaItem.class).readValue(sourceSchemas.getSchemas()));
        } catch (Exception e) {
            log.error("source schema parse error ", e);
        }
        return schemaInfo;
    }

    @Override
    public SchemaInfo syncSourceSchema(String sourceId) throws Exception {
        SchemaSyncJob schemaSyncJob = new SchemaSyncJob();
        schemaSyncJob.execute(sourceId);
        return getSourceSchemaInfo(sourceId);
    }

    @Override
    public List<Source> getAllParents(String sourceId) {
        List<Source> parents = new LinkedList<>();
        Source source = sourceMapper.selectByPrimaryKey(sourceId);
        if (source != null) {
            getParent(parents, source.getParentId());
        }
        return parents;
    }

    private void getParent(List<Source> list, String parentId) {
        if (parentId == null) {
            return;
        }
        Source source = sourceMapper.selectByPrimaryKey(parentId);
        if (source != null) {
            if (source.getParentId() != null) {
                getParent(list, source.getParentId());
            }
            list.add(source);
        }
    }

    @Override
    public SourceResourceModel exportResource(TransferConfig transferConfig, Set<String> ids) {
        if (ids == null || ids.size() == 0) {
            return null;
        }
        SourceResourceModel sourceExportModel = new SourceResourceModel();
        sourceExportModel.setMainModels(new LinkedList<>());

        Map<String, Source> parentMap = new HashMap<>();

        for (String sourceId : ids) {
            SourceResourceModel.MainModel mainModel = new SourceResourceModel.MainModel();
            Source source = retrieve(sourceId);
            securityManager.requireOrgOwner(source.getOrgId());
            mainModel.setSource(source);
            // files
            mainModel.setFiles(FileUtils.walkDirAsStream(new File(fileService.getBasePath(FileOwner.DATA_SOURCE, sourceId)), null, false));
            sourceExportModel.getMainModels().add(mainModel);
            // parents
            if (transferConfig.isWithParents()) {
                List<Source> allParents = getAllParents(sourceId);
                if (!CollectionUtils.isEmpty(allParents)) {
                    for (Source parent : allParents) {
                        parentMap.put(parent.getId(), parent);
                    }
                }
            }
        }
        sourceExportModel.setParents(new LinkedList<>(parentMap.values()));
        return sourceExportModel;
    }

    @Override
    public boolean importResource(SourceResourceModel model, ImportStrategy strategy, String orgId) {
        if (model == null) {
            return true;
        }
        switch (strategy) {
            case OVERWRITE:
                importSource(model, orgId, true);
                break;
            case ROLLBACK:
                importSource(model, orgId, false);
                break;
            default:
                importSource(model, orgId, false);
        }
        return true;
    }

    @Override
    public void replaceId(SourceResourceModel model
            , final Map<String, String> sourceIdMapping
            , final Map<String, String> viewIdMapping
            , final Map<String, String> chartIdMapping, Map<String, String> boardIdMapping, Map<String, String> folderIdMapping) {

        if (model == null || model.getMainModels() == null) {
            return;
        }
        for (SourceResourceModel.MainModel mainModel : model.getMainModels()) {
            String newId = UUIDGenerator.generate();
            sourceIdMapping.put(mainModel.getSource().getId(), newId);
            mainModel.getSource().setId(newId);
        }
        Map<String, String> parentIdMapping = new HashMap<>();
        for (Source parent : model.getParents()) {
            String newId = UUIDGenerator.generate();
            parentIdMapping.put(parent.getId(), newId);
            parent.setId(newId);
        }
        for (Source parent : model.getParents()) {
            parent.setParentId(parentIdMapping.get(parent.getParentId()));
        }
    }

    @Override
    public void requirePermission(Source source, int permission) {
        if (securityManager.isOrgOwner(source.getOrgId())) {
            return;
        }
        List<Role> roles = roleService.listUserRoles(source.getOrgId(), getCurrentUser().getId());
        boolean hasPermission = roles.stream().anyMatch(role -> hasPermission(role, source, permission));
        if (!hasPermission) {
            Exceptions.tr(PermissionDeniedException.class, "message.security.permission-denied",
                    ResourceType.SOURCE + ":" + source.getName() + ":" + ShiroSecurityManager.expand2StringPermissions(permission));
        }
    }

    private boolean hasPermission(Role role, Source source, int permission) {
        if (source.getId() == null || rrrMapper.countRolePermission(source.getId(), role.getId()) == 0) {
            Source parent = sourceMapper.selectByPrimaryKey(source.getParentId());
            if (parent == null) {
                return securityManager.hasPermission(PermissionHelper.sourcePermission(source.getOrgId(), role.getId(), ResourceType.SOURCE.name(), permission));
            } else {
                return hasPermission(role, parent, permission);
            }
        } else {
            return securityManager.hasPermission(PermissionHelper.sourcePermission(source.getOrgId(), role.getId(), source.getId(), permission));
        }
    }

    @Override
    public Source createSource(SourceCreateParam createParam) {
        Source source = create(createParam);
        if (!source.getIsFolder()) {
            updateJdbcSourceSyncJob(source);
        }
        return source;
    }

    @Override
    @Transactional
    public Source create(BaseCreateParam createParam) {
        // encrypt property
        SourceCreateParam sourceCreateParam = (SourceCreateParam) createParam;
        if (Objects.equals(sourceCreateParam.getIsFolder(), Boolean.TRUE)) {
            sourceCreateParam.setType(ResourceType.FOLDER.name());
        } else {
            sourceCreateParam.setIsFolder(Boolean.FALSE);
        }
        if (!sourceCreateParam.getIsFolder()) {
            try {
                sourceCreateParam.setConfig(encryptConfig(sourceCreateParam.getType(), sourceCreateParam.getConfig()));
            } catch (Exception e) {
                Exceptions.e(e);
            }
        }
        Source source = SourceService.super.create(createParam);
        grantDefaultPermission(source);
        return source;

    }

    @Override
    public boolean updateSource(SourceUpdateParam updateParam) {
        boolean success = update(updateParam);
        if (success) {
            Source source = retrieve(updateParam.getId());
            getDataProviderService().updateSource(source);
            updateJdbcSourceSyncJob(source);
        }
        return false;
    }

    @Override
    public boolean updateBase(SourceBaseUpdateParam updateParam) {
        Source source = retrieve(updateParam.getId());
        requirePermission(source, Const.MANAGE);
        if (!source.getName().equals(updateParam.getName())) {
            //check name
            Source check = new Source();
            check.setParentId(updateParam.getParentId());
            check.setOrgId(source.getOrgId());
            check.setName(updateParam.getName());
            checkUnique(check);
        }
        source.setId(updateParam.getId());
        source.setUpdateBy(getCurrentUser().getId());
        source.setUpdateTime(new Date());
        source.setName(updateParam.getName());
        source.setParentId(updateParam.getParentId());
        source.setIndex(updateParam.getIndex());
        return 1 == sourceMapper.updateByPrimaryKey(source);
    }

    @Override
    public boolean unarchive(String id, String newName, String parentId, double index) {
        Source source = retrieve(id);
        requirePermission(source, Const.MANAGE);

        //check name
        if (!source.getName().equals(newName)) {
            checkUnique(source.getOrgId(), parentId, newName);
        }

        // update status
        source.setName(newName);
        source.setParentId(parentId);
        source.setStatus(Const.DATA_STATUS_ACTIVE);
        source.setIndex(index);
        return 1 == sourceMapper.updateByPrimaryKey(source);
    }

    @Override
    @Transactional
    public boolean update(BaseUpdateParam updateParam) {
        SourceUpdateParam sourceUpdateParam = (SourceUpdateParam) updateParam;
        try {
            String config = encryptConfig(sourceUpdateParam.getType(), sourceUpdateParam.getConfig());
            sourceUpdateParam.setConfig(config);
        } catch (Exception e) {
            Exceptions.e(e);
        }
        return SourceService.super.update(updateParam);
    }

    @Override
    @Transactional
    public void grantDefaultPermission(Source source) {
        if (securityManager.isOrgOwner(source.getOrgId())) {
            return;
        }
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.setOrgId(source.getOrgId());
        permissionInfo.setSubjectType(SubjectType.USER);
        permissionInfo.setSubjectId(getCurrentUser().getId());
        permissionInfo.setResourceType(ResourceType.SOURCE);
        permissionInfo.setResourceId(source.getId());
        permissionInfo.setPermission(Const.CREATE);
        roleService.grantPermission(Collections.singletonList(permissionInfo));
    }

    private String encryptConfig(String type, String config) throws Exception {
        if (StringUtils.isEmpty(config)) {
            return config;
        }
        DataProviderConfigTemplate configTemplate = getDataProviderService().getSourceConfigTemplate(type);
        if (!CollectionUtils.isEmpty(configTemplate.getAttributes())) {
            JSONObject jsonObject = JSON.parseObject(config);
            for (DataProviderConfigTemplate.Attribute attribute : configTemplate.getAttributes()) {
                if (attribute.isEncrypt()
                        && jsonObject.containsKey(attribute.getName())
                        && StringUtils.isNotBlank(jsonObject.get(attribute.getName()).toString())) {
                    String val = jsonObject.get(attribute.getName()).toString();
                    if (val.startsWith(Const.ENCRYPT_FLAG)) {
                        jsonObject.put(attribute.getName(), val);
                    } else {
                        jsonObject.put(attribute.getName(), Const.ENCRYPT_FLAG + AESUtil.encrypt(val));
                    }
                }
            }
            return jsonObject.toJSONString();
        }
        return config;
    }

    @Override
    public Source retrieve(String id) {
        SourceDetail sourceDetail = new SourceDetail(SourceService.super.retrieve(id));
        sourceDetail.setSchemaUpdateDate(sourceSchemasMapper.selectUpdateDateBySource(id));
        return sourceDetail;
    }

    @Override
    public void deleteReference(Source source) {
        deleteJdbcSourceSyncJob(source);
        sourceSchemasMapper.deleteBySource(source.getId());
    }

    @Override
    public boolean safeDelete(String id) {
        return sourceMapper.checkReference(id) == 0;
    }

    @Override
    public void deleteStaticFiles(Source source) {
        fileService.deleteFiles(FileOwner.DATA_SOURCE, source.getId());
    }

    private void deleteJdbcSourceSyncJob(Source source) {
        try {
            JobKey jobKey = new JobKey(source.getName(), source.getId());
            scheduler.deleteJob(jobKey);
        } catch (Exception e) {
            log.error("schema sync job delete error ", e);
        }
    }

    private void updateJdbcSourceSyncJob(Source source) {
        TaskExecutor.submit(() -> {
            try {
                new SchemaSyncJob().execute(source.getId());
            } catch (Exception e) {
                log.error("source schema sync error", e);
            }
        });
        try {
            DataProviderSource dataProviderSource = getDataProviderService().parseDataProviderConfig(source);

            JobKey jobKey = new JobKey(source.getName(), source.getId());

            Object enable = dataProviderSource.getProperties().get(ENABLE_SYNC_SCHEMAS);
            if (enable != null && "true".equals(enable.toString())) {
                Object interval = dataProviderSource.getProperties().get(SYNC_INTERVAL);
                if (interval == null || !NumberUtils.isDigits(interval.toString())) {
                    Exceptions.msg("sync interval must be a number");
                }
                int intervalMin = Math.max(Integer.parseInt(interval.toString()), Const.MINIMUM_SYNC_INTERVAL);

                scheduler.deleteJob(jobKey);
                Trigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity(source.getId())
                        .withSchedule(SimpleScheduleBuilder.repeatMinutelyForever(intervalMin))
                        .startNow()
                        .build();
                JobDetail jobDetail = JobBuilder.newJob()
                        .withIdentity(jobKey)
                        .ofType(SchemaSyncJob.class)
                        .build();
                jobDetail.getJobDataMap().put(SchemaSyncJob.SOURCE_ID, source.getId());
                scheduler.scheduleJob(jobDetail, trigger);
                log.info("jdbc source schema job has been created {} - {} - interval {} ", source.getId(), source.getName(), intervalMin);
            } else {
                scheduler.deleteJob(jobKey);
                log.info("jdbc source schema job has been deleted {} - {}", source.getId(), source.getName());
            }
        } catch (Exception e) {
            log.error("schema sync job update error ", e);
        }
    }

    private void importSource(SourceResourceModel model,
                              String orgId,
                              boolean deleteFirst) {

        if (model == null || CollectionUtils.isEmpty(model.getMainModels())) {
            return;
        }
        for (SourceResourceModel.MainModel mainModel : model.getMainModels()) {
            Source source = mainModel.getSource();
            if (source == null) {
                continue;
            }
            if (deleteFirst) {
                try {
                    Source retrieve = retrieve(source.getId(), false);
                    if (retrieve != null && !retrieve.getOrgId().equals(orgId)) {
                        Exceptions.msg("message.viz.import.database.conflict");
                    }
                } catch (NotFoundException ignored) {
                }
                try {
                    delete(source.getId(), false, false);
                } catch (Exception ignore) {
                }
            }
            // check name
            try {
                Source check = new Source();
                check.setOrgId(orgId);
                check.setParentId(source.getParentId());
                check.setName(source.getName());
                checkUnique(check);
            } catch (Exception e) {
                source.setName(DateUtils.withTimeString(source.getName()));
            }
            // insert source
            source.setOrgId(orgId);
            source.setUpdateBy(getCurrentUser().getId());
            source.setUpdateTime(new Date());
            sourceMapper.insert(source);

            // insert parents
            if (!CollectionUtils.isEmpty(model.getParents())) {
                for (Source parent : model.getParents()) {
                    try {
                        Source check = new Source();
                        check.setName(parent.getName());
                        check.setOrgId(parent.getOrgId());
                        check.setParentId(source.getParentId());
                        checkUnique(check);
                    } catch (Exception e) {
                        source.setName(DateUtils.withTimeString(source.getName()));
                    }
                    try {
                        parent.setOrgId(orgId);
                        sourceMapper.insert(parent);
                    } catch (Exception ignore) {
                    }
                }
            }

            // copy files
            if (!CollectionUtils.isEmpty(mainModel.getFiles())) {
                for (Map.Entry<String, byte[]> fileEntry : mainModel.getFiles().entrySet()) {
                    String name = fileEntry.getKey();
                    String basePath = fileService.getBasePath(FileOwner.DATA_SOURCE, source.getId());
                    String filePath = FileUtils.concatPath(basePath, name);
                    try {
                        FileUtils.save(filePath, fileEntry.getValue(), false);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private DataProviderService getDataProviderService() {
        return Application.getBean(DataProviderService.class);
    }

}
