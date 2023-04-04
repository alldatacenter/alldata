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

import datart.core.base.consts.Const;
import datart.core.base.consts.FileOwner;
import datart.core.base.exception.Exceptions;
import datart.core.base.exception.NotFoundException;
import datart.core.base.exception.ParamException;
import datart.core.common.DateUtils;
import datart.core.common.UUIDGenerator;
import datart.core.entity.Datachart;
import datart.core.entity.Folder;
import datart.core.entity.Variable;
import datart.core.entity.View;
import datart.core.mappers.ext.DatachartMapperExt;
import datart.core.mappers.ext.FolderMapperExt;
import datart.core.mappers.ext.RelRoleResourceMapperExt;
import datart.security.base.ResourceType;
import datart.security.util.PermissionHelper;
import datart.server.base.dto.DatachartDetail;
import datart.server.base.params.BaseCreateParam;
import datart.server.base.params.DatachartCreateParam;
import datart.server.base.transfer.ImportStrategy;
import datart.server.base.transfer.TransferConfig;
import datart.server.base.transfer.model.DatachartResourceModel;
import datart.server.base.transfer.model.DatachartTemplateModel;
import datart.server.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
public class DatachartServiceImpl extends BaseService implements DatachartService {

    private final RoleService roleService;

    private final FileService fileService;

    private final FolderMapperExt folderMapper;

    private final RelRoleResourceMapperExt rrrMapper;

    private final FolderService folderService;

    private final DatachartMapperExt datachartMapper;

    private final ViewService viewService;

    private final VariableService variableService;

    public DatachartServiceImpl(RoleService roleService,
                                FileService fileService,
                                FolderMapperExt folderMapper,
                                RelRoleResourceMapperExt rrrMapper,
                                FolderService folderService,
                                DatachartMapperExt datachartMapper,
                                ViewService viewService,
                                VariableService variableService) {
        this.roleService = roleService;
        this.fileService = fileService;
        this.folderMapper = folderMapper;
        this.rrrMapper = rrrMapper;
        this.folderService = folderService;
        this.datachartMapper = datachartMapper;
        this.viewService = viewService;
        this.variableService = variableService;
    }

    @Override
    public RoleService getRoleService() {
        return roleService;
    }

    @Override
    @Transactional
    public boolean archive(String id) {
        DatachartService.super.archive(id);
        //remove from folder
        return 1 == folderMapper.deleteByRelTypeAndId(ResourceType.DATACHART.name(), id);
    }

    @Override
    @Transactional
    public boolean delete(String id) {
        DatachartService.super.delete(id);
        //remove from folder
        return 1 == folderMapper.deleteByRelTypeAndId(ResourceType.DATACHART.name(), id);
    }

    @Override
    public void deleteStaticFiles(Datachart datachart) {
        fileService.deleteFiles(FileOwner.DATACHART, datachart.getId());
    }

    @Override
    public void requirePermission(Datachart datachart, int permission) {
        Folder folder = folderMapper.selectByRelTypeAndId(ResourceType.DATACHART.name(), datachart.getId());
        if (folder == null) {
            // 在创建时，不进行权限校验
        } else {
            folderService.requirePermission(folder, permission);
        }
    }

    @Override
    public DatachartDetail getDatachartDetail(String datachartId) {
        DatachartDetail datachartDetail = new DatachartDetail();
        Datachart datachart = retrieve(datachartId);
        //folder index
        Folder folder = folderMapper.selectByRelTypeAndId(ResourceType.DATACHART.name(), datachartId);
        if (folder != null) {
            datachartDetail.setParentId(folder.getParentId());
            datachartDetail.setIndex(folder.getIndex());
        }
        BeanUtils.copyProperties(datachart, datachartDetail);
        try {
            datachartDetail.setView(retrieve(datachart.getViewId(), View.class));
        } catch (NotFoundException ignored) {
        }

        //variables
        LinkedList<Variable> variables = new LinkedList<>(variableService.listOrgQueryVariables(datachart.getOrgId()));
        variables.addAll(variableService.listViewQueryVariables(datachart.getViewId()));
        datachartDetail.setQueryVariables(variables);

        // download permission
        datachartDetail.setDownload(securityManager
                .hasPermission(PermissionHelper.vizPermission(datachart.getOrgId(), "*", datachartId, Const.DOWNLOAD)));

        return datachartDetail;
    }

    @Override
    public Folder createWithFolder(BaseCreateParam createParam) {
        // check unique
        DatachartCreateParam param = (DatachartCreateParam) createParam;
        if (!CollectionUtils.isEmpty(folderMapper.checkVizName(param.getOrgId(), param.getParentId(), param.getName()))) {
            Exceptions.tr(ParamException.class, "error.param.exists.name");
        }
        Datachart datachart = DatachartService.super.create(createParam);
        // create folder
        Folder folder = new Folder();
        folder.setId(UUIDGenerator.generate());
        BeanUtils.copyProperties(createParam, folder);
        folder.setRelType(ResourceType.DATACHART.name());
        folder.setRelId(datachart.getId());
        folder.setSubType(param.getSubType());
        folder.setAvatar(param.getAvatar());

        folderService.requirePermission(folder, Const.CREATE);
        folderMapper.insert(folder);

        return folder;
    }

    @Override
    public DatachartResourceModel exportResource(TransferConfig transferConfig, Set<String> ids) {
        if (ids == null || ids.size() == 0) {
            return null;
        }
        DatachartResourceModel datachartResourceModel = new DatachartResourceModel();
        datachartResourceModel.setMainModels(new LinkedList<>());

        Set<String> viewIds = new HashSet<>();
        Set<String> parents = new HashSet<>();

        for (String datachartId : ids) {
            DatachartResourceModel.MainModel mainModel = new DatachartResourceModel.MainModel();
            Datachart datachart = retrieve(datachartId);
            securityManager.requireOrgOwner(datachart.getOrgId());
            mainModel.setDatachart(datachart);
            Folder vizFolder = folderService.getVizFolder(datachartId, ResourceType.DATACHART.name());
            mainModel.setFolder(vizFolder);
            if (StringUtils.isNotBlank(datachart.getViewId())) {
                viewIds.add(datachart.getViewId());
            }
            datachartResourceModel.getMainModels().add(mainModel);

            if (transferConfig.isWithParents()) {
                List<Folder> allParents = folderService.getAllParents(vizFolder.getParentId());
                if (!CollectionUtils.isEmpty(allParents)) {
                    for (Folder folder : allParents) {
                        parents.add(folder.getId());
                    }
                }
            }
        }
        //folder
        datachartResourceModel.setParents(parents);
        // view
        datachartResourceModel.getViews().addAll(viewIds);
        return datachartResourceModel;
    }

    @Override
    public boolean importResource(DatachartResourceModel model, ImportStrategy strategy, String orgId) {
        if (model == null) {
            return true;
        }
        switch (strategy) {
            case OVERWRITE:
                importDatachart(model, orgId, true);
                break;
            case ROLLBACK:
                importDatachart(model, orgId, false);
                break;
            default:
                importDatachart(model, orgId, false);
        }
        return true;
    }

    @Override
    public void replaceId(DatachartResourceModel model
            , final Map<String, String> sourceIdMapping
            , final Map<String, String> viewIdMapping
            , final Map<String, String> chartIdMapping
            , final Map<String, String> boardIdMapping
            , final Map<String, String> folderIdMapping) {
        if (model == null || model.getMainModels() == null) {
            return;
        }
        for (DatachartResourceModel.MainModel mainModel : model.getMainModels()) {
            String newId = UUIDGenerator.generate();
            chartIdMapping.put(mainModel.getDatachart().getId(), newId);
            mainModel.getDatachart().setId(newId);
            mainModel.getDatachart().setViewId(viewIdMapping.get(mainModel.getDatachart().getViewId()));
            mainModel.getFolder().setId(UUIDGenerator.generate());
            mainModel.getFolder().setRelId(newId);
            mainModel.getFolder().setParentId(folderIdMapping.get(mainModel.getFolder().getParentId()));
        }
    }

    @Override
    public Folder importTemplate(DatachartTemplateModel model, String orgId, String name, Folder folder) {
        DatachartCreateParam createParam = new DatachartCreateParam();
        createParam.setOrgId(orgId);
        createParam.setConfig(model.getDatachart().getConfig());
        createParam.setName(name);
        if (folder != null) {
            createParam.setParentId(folder.getId());
        }
        return createWithFolder(createParam);
    }

    private void importDatachart(DatachartResourceModel model,
                                 String orgId,
                                 boolean deleteFirst) {
        if (model == null || CollectionUtils.isEmpty(model.getMainModels())) {
            return;
        }
        for (DatachartResourceModel.MainModel mainModel : model.getMainModels()) {
            Datachart datachart = mainModel.getDatachart();
            if (deleteFirst) {
                try {
                    Datachart retrieve = retrieve(datachart.getId(), false);
                    if (retrieve != null && !retrieve.getOrgId().equals(orgId)) {
                        Exceptions.msg("message.viz.import.database.conflict");
                    }
                } catch (NotFoundException ignored) {
                }
                try {
                    delete(datachart.getId(), false, false);
                } catch (Exception ignore) {
                }
            }
            // datachart folder
            Folder folder = mainModel.getFolder();
            folder.setOrgId(orgId);
            try {
                folderService.checkUnique(orgId, folder.getParentId(), folder.getName());
            } catch (Exception e) {
                folder.setName(DateUtils.withTimeString(folder.getName()));
                datachart.setName(folder.getName());
            }
            folderMapper.insert(folder);

            datachart.setOrgId(orgId);
            datachart.setUpdateBy(getCurrentUser().getId());
            datachart.setUpdateTime(new Date());
            datachartMapper.insert(datachart);
        }
    }

    @Override
    public boolean safeDelete(String id) {
        return datachartMapper.countWidgetRels(id) == 0;
    }

    @Override
    public List<Folder> getAllParents(String id) {
        return null;
    }

}
