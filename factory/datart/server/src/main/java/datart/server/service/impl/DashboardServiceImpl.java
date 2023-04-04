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
import datart.core.common.FileUtils;
import datart.core.common.UUIDGenerator;
import datart.core.entity.*;
import datart.core.mappers.ext.*;
import datart.security.base.ResourceType;
import datart.security.util.PermissionHelper;
import datart.server.base.dto.DashboardBaseInfo;
import datart.server.base.dto.DashboardDetail;
import datart.server.base.dto.WidgetDetail;
import datart.server.base.params.*;
import datart.server.base.transfer.ImportStrategy;
import datart.server.base.transfer.TransferConfig;
import datart.server.base.transfer.model.DashboardResourceModel;
import datart.server.base.transfer.model.DashboardTemplateModel;
import datart.server.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DashboardServiceImpl extends BaseService implements DashboardService {

    private final DashboardMapperExt dashboardMapper;

    private final WidgetMapperExt widgetMapper;

    private final RelWidgetElementMapperExt rweMapper;

    private final RelWidgetWidgetMapperExt rwwMapper;

    private final RelRoleResourceMapperExt rrrMapper;

    private final RoleService roleService;

    private final FileService fileService;

    private final FolderMapperExt folderMapper;

    private final ViewMapperExt viewMapper;

    private final DatachartMapperExt datachartMapper;

    private final WidgetService widgetService;

    private final FolderService folderService;

    private final VariableService variableService;

    private final ViewService viewService;

    private final DatachartService datachartService;


    public DashboardServiceImpl(DashboardMapperExt dashboardMapper,
                                WidgetMapperExt widgetMapper,
                                RelWidgetElementMapperExt rweMapper,
                                RelWidgetWidgetMapperExt rwwMapper,
                                RelRoleResourceMapperExt rrrMapper, RoleService roleService,
                                FileService fileService,
                                FolderMapperExt folderMapper,
                                ViewMapperExt viewMapper,
                                DatachartMapperExt datachartMapper,
                                WidgetService widgetService,
                                FolderService folderService,
                                VariableService variableService,
                                ViewService viewService,
                                DatachartService datachartService) {
        this.dashboardMapper = dashboardMapper;
        this.widgetMapper = widgetMapper;
        this.rweMapper = rweMapper;
        this.rwwMapper = rwwMapper;
        this.rrrMapper = rrrMapper;
        this.roleService = roleService;
        this.fileService = fileService;
        this.folderMapper = folderMapper;
        this.viewMapper = viewMapper;
        this.datachartMapper = datachartMapper;
        this.widgetService = widgetService;
        this.folderService = folderService;
        this.variableService = variableService;
        this.viewService = viewService;
        this.datachartService = datachartService;
    }


    @Override
    public List<DashboardBaseInfo> listDashboard(String orgId) {
        List<Dashboard> dashboards = dashboardMapper.listByOrgId(orgId);
        return dashboards.stream().filter(dashboard -> securityManager
                .hasPermission(PermissionHelper.vizPermission(dashboard.getOrgId(), "*", dashboard.getId(), Const.READ)))
                .map(DashboardBaseInfo::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean delete(String dashboardId) {
        //remove from folder
        DashboardService.super.delete(dashboardId);
        return 1 == folderMapper.deleteByRelTypeAndId(ResourceType.DASHBOARD.name(), dashboardId);
    }

    @Override
    public void deleteReference(Dashboard dashboard) {
        dashboardMapper.deleteDashboard(dashboard.getId());
    }

    @Override
    @Transactional
    public boolean archive(String id) {
        DashboardService.super.archive(id);
        //remove from folder
        return folderMapper.deleteByRelTypeAndId(ResourceType.DASHBOARD.name(), id) == 1;
    }

    @Override
    public DashboardDetail getDashboardDetail(String dashboardId) {

        Dashboard dashboard = retrieve(dashboardId);
        DashboardDetail dashboardDetail = new DashboardDetail();
        BeanUtils.copyProperties(dashboard, dashboardDetail);

        //folder index
        Folder folder = folderMapper.selectByRelTypeAndId(ResourceType.DASHBOARD.name(), dashboardId);
        if (folder != null) {
            dashboardDetail.setParentId(folder.getParentId());
            dashboardDetail.setIndex(folder.getIndex());
        }

        Set<String> viewIds = new HashSet<>();
        Set<String> datachartIds = new HashSet<>();
        // get all widgets details
        List<WidgetDetail> widgetDetails = getWidgets(dashboardId, datachartIds, viewIds);
        dashboardDetail.setWidgets(widgetDetails);

        // charts
        if (!CollectionUtils.isEmpty(datachartIds)) {
            dashboardDetail.setDatacharts(datachartMapper.listByIds(datachartIds));
        } else {
            dashboardDetail.setDatacharts(Collections.emptyList());
        }
        //views
        List<String> chartViews = dashboardDetail.getDatacharts().stream().map(Datachart::getViewId).collect(Collectors.toList());
        viewIds.addAll(chartViews);
        if (!CollectionUtils.isEmpty(viewIds)) {
            dashboardDetail.setViews(viewMapper.listByIds(viewIds));
        } else {
            dashboardDetail.setViews(Collections.emptyList());
        }

        //variables
        LinkedList<Variable> variables = new LinkedList<>(variableService.listOrgQueryVariables(dashboard.getOrgId()));
        if (!CollectionUtils.isEmpty(viewIds)) {
            for (String viewId : viewIds) {
                variables.addAll(variableService.listViewQueryVariables(viewId));
            }
        }
        dashboardDetail.setQueryVariables(variables);
        // download permission
        dashboardDetail.setDownload(securityManager.hasPermission(PermissionHelper.vizPermission(dashboard.getOrgId(), "*", dashboardId, Const.DOWNLOAD)));

        return dashboardDetail;
    }

    @Override
    public Folder copyDashboard(DashboardCreateParam dashboard) throws IOException {

        Folder folder = createWithFolder(dashboard);

        DashboardDetail copy = getDashboardDetail(dashboard.getId());

        BeanUtils.copyProperties(dashboard, copy);

        // copy database records
        if (CollectionUtils.isNotEmpty(copy.getWidgets())) {
            HashMap<String, String> widgetIdMapping = new HashMap<>();

            List<WidgetCreateParam> widgetCreateParams = copy.getWidgets().stream()
                    .map(widget -> {
                        String uuid = UUIDGenerator.generate();
                        widgetIdMapping.put(widget.getId(), uuid);
                        WidgetCreateParam createParam = new WidgetCreateParam();
                        BeanUtils.copyProperties(widget, createParam);
                        createParam.setId(uuid);
                        createParam.setDashboardId(folder.getRelId());

                        List<WidgetRelParam> widgetRelParams = widget.getRelations().stream().map(relWidgetWidget -> {
                            WidgetRelParam widgetRelParam = new WidgetRelParam();
                            BeanUtils.copyProperties(relWidgetWidget, widgetRelParam);
                            return widgetRelParam;
                        }).collect(Collectors.toList());

                        createParam.setRelations(widgetRelParams);
                        return createParam;
                    }).collect(Collectors.toList());

            // replace ids
            for (WidgetCreateParam createParam : widgetCreateParams) {
                if (widgetIdMapping.containsKey(createParam.getParentId())) {
                    createParam.setParentId(widgetIdMapping.get(createParam.getParentId()));
                }
                if (CollectionUtils.isNotEmpty(createParam.getRelations())) {
                    for (WidgetRelParam relation : createParam.getRelations()) {
                        relation.setSourceId(createParam.getId());
                        if (widgetIdMapping.containsKey(relation.getTargetId())) {
                            relation.setTargetId(widgetIdMapping.get(relation.getTargetId()));
                        }
                    }
                }
            }
            widgetService.createWidgets(widgetCreateParams);
        }

        // copy static files
        String basePath = fileService.getBasePath(FileOwner.DASHBOARD, dashboard.getId());
        String distPath = fileService.getBasePath(FileOwner.DASHBOARD, folder.getRelId());
        File src = new File(basePath);
        if (src.exists()) {
            FileSystemUtils.copyRecursively(src, new File(distPath));
        }
        return folder;
    }

    @Override
    public DashboardResourceModel exportResource(TransferConfig transferConfig, Set<String> dashboardIds) {

        DashboardResourceModel dashboardExportModel = new DashboardResourceModel();
        dashboardExportModel.setMainModels(new ArrayList<>());
        Set<String> parents = new HashSet<>();
        Set<String> viewIds = new HashSet<>();
        Set<String> datachartIds = new HashSet<>();

        for (String dashboardId : dashboardIds) {
            DashboardResourceModel.MainModel mainModel = new DashboardResourceModel.MainModel();
            Dashboard dashboard = retrieve(dashboardId);
            securityManager.requireOrgOwner(dashboard.getOrgId());
            mainModel.setDashboard(dashboard);
            Folder folder = folderService.getVizFolder(dashboardId, ResourceType.DASHBOARD.name());
            mainModel.setFolder(folder);
            //folders
            if (transferConfig.isWithParents()) {
                List<Folder> allParents = getAllParents(folder.getParentId());
                if (CollectionUtils.isNotEmpty(allParents)) {
                    for (Folder parent : allParents) {
                        parents.add(parent.getId());
                    }
                }
            }
            // widgets
            List<WidgetDetail> widgetDetails = getWidgets(dashboardId, datachartIds, viewIds);
            mainModel.setWidgets(widgetDetails);
            // files
            mainModel.setFiles(FileUtils.walkDirAsStream(new File(fileService.getBasePath(FileOwner.DASHBOARD, dashboardId)), null, false));
            dashboardExportModel.getMainModels().add(mainModel);
        }
        dashboardExportModel.setParents(parents);
        // datacharts
        dashboardExportModel.setDatacharts(datachartIds);
        // views
        dashboardExportModel.setViews(viewIds);
        dashboardExportModel.setOrgId(dashboardExportModel.getMainModels().get(0).getDashboard().getOrgId());
        return dashboardExportModel;
    }


    @Override
    public Folder importTemplate(DashboardTemplateModel model, String orgId, String name, Folder parent) {
        securityManager.requireOrgOwner(orgId);
        DashboardCreateParam createParam = new DashboardCreateParam();
        createParam.setOrgId(orgId);
        createParam.setConfig(model.getDashboard().getConfig());
        createParam.setName(name);
        if (parent != null) {
            createParam.setParentId(parent.getId());
        }
        createParam.setStatus((short) Const.DATA_STATUS_ACTIVE);
        Folder folder = createWithFolder(createParam);
        if (CollectionUtils.isNotEmpty(model.getWidgets())) {
            DashboardUpdateParam updateParam = new DashboardUpdateParam();
            BeanUtils.copyProperties(createParam, updateParam);
            List<WidgetCreateParam> widgetCreateParams = model.getWidgets()
                    .stream()
                    .map(widget -> {
                        WidgetCreateParam widgetCreateParam = new WidgetCreateParam();
                        BeanUtils.copyProperties(widget, widgetCreateParam);
                        widgetCreateParam.setDashboardId(folder.getRelId());
                        return widgetCreateParam;
                    }).collect(Collectors.toList());
            updateParam.setWidgetToCreate(widgetCreateParams);
            updateParam.setId(folder.getRelId());
            update(updateParam);
        }

        Map<String, byte[]> files = model.getFiles();
        if (files != null && files.size() > 0) {
            for (Map.Entry<String, byte[]> fileEntry : files.entrySet()) {
                String fileName = fileEntry.getKey();
                String basePath = fileService.getBasePath(FileOwner.DASHBOARD, folder.getRelId());
                String filePath = FileUtils.concatPath(basePath, fileName);
                try {
                    FileUtils.save(filePath, fileEntry.getValue(), false);
                } catch (Exception ignored) {
                }
            }
        }
        return folder;
    }

    @Override
    public RoleService getRoleService() {
        return roleService;
    }

    @Override
    public void deleteStaticFiles(Dashboard dashboard) {
        fileService.deleteFiles(FileOwner.DASHBOARD, dashboard.getId());
    }

    @Override
    public void requirePermission(Dashboard dashboard, int permission) {
        Folder folder = folderMapper.selectByRelTypeAndId(ResourceType.DASHBOARD.name(), dashboard.getId());
        if (folder == null) {
            //创建时，不进行权限校验
        } else {
            folderService.requirePermission(folder, permission);
        }
    }

    @Override
    public Folder createWithFolder(DashboardCreateParam createParam) {
        if (!CollectionUtils.isEmpty(folderMapper.checkVizName(createParam.getOrgId(), createParam.getParentId(), createParam.getName()))) {
            Exceptions.tr(ParamException.class, "error.param.exists.name");
        }
        Dashboard dashboard = DashboardService.super.create(createParam);

        // create folder
        Folder folder = new Folder();
        BeanUtils.copyProperties(createParam, folder);
        folder.setId(UUIDGenerator.generate());
        folder.setRelType(ResourceType.DASHBOARD.name());
        folder.setRelId(dashboard.getId());

        folderService.requirePermission(folder, Const.CREATE);

        folderMapper.insert(folder);
        return folder;
    }

    @Override
    @Transactional
    public boolean update(BaseUpdateParam updateParam) {

        DashboardUpdateParam param = (DashboardUpdateParam) updateParam;

        HashMap<String, String> widgetIdMapping = new HashMap<>();

        // generate widget id
        if (CollectionUtils.isNotEmpty(param.getWidgetToCreate())) {
            for (WidgetCreateParam widgetCreateParam : param.getWidgetToCreate()) {
                String uuid = UUIDGenerator.generate();
                widgetIdMapping.put(widgetCreateParam.getId(), uuid);
                widgetCreateParam.setId(uuid);
            }
            //replace widget relation id and parent id
            for (WidgetCreateParam widgetCreateParam : param.getWidgetToCreate()) {
                if (widgetIdMapping.containsKey(widgetCreateParam.getParentId())) {
                    widgetCreateParam.setParentId(widgetIdMapping.get(widgetCreateParam.getParentId()));
                }
                if (CollectionUtils.isNotEmpty(widgetCreateParam.getRelations())) {
                    for (WidgetRelParam relation : widgetCreateParam.getRelations()) {
                        relation.setSourceId(widgetCreateParam.getId());
                        if (widgetIdMapping.containsKey(relation.getTargetId())) {
                            relation.setTargetId(widgetIdMapping.get(relation.getTargetId()));
                        }
                    }
                }
            }
        }

        if (CollectionUtils.isNotEmpty(param.getWidgetToUpdate())) {
            for (WidgetUpdateParam widgetUpdateParam : param.getWidgetToUpdate()) {
                if (widgetIdMapping.containsKey(widgetUpdateParam.getParentId())) {
                    widgetUpdateParam.setParentId(widgetIdMapping.get(widgetUpdateParam.getParentId()));
                }
                if (CollectionUtils.isNotEmpty(widgetUpdateParam.getRelations())) {
                    for (WidgetRelParam relation : widgetUpdateParam.getRelations()) {
                        relation.setSourceId(widgetUpdateParam.getId());
                        if (widgetIdMapping.containsKey(relation.getTargetId())) {
                            relation.setTargetId(widgetIdMapping.get(relation.getTargetId()));
                        }
                    }
                }
            }
        }

        widgetService.createWidgets(param.getWidgetToCreate());

        widgetService.updateWidgets(param.getWidgetToUpdate());

        widgetService.deleteWidgets(param.getWidgetToDelete());

        return DashboardService.super.update(updateParam);
    }

    private List<WidgetDetail> getWidgets(String dashboardId, Set<String> datachartIds, Set<String> viewIds) {
        List<String> widgetIds = new ArrayList<>();

        // get all widgets details
        List<Widget> widgets = widgetMapper.listByDashboard(dashboardId);
        List<WidgetDetail> widgetDetails = widgets.stream().map(widget -> {
            WidgetDetail widgetDetail = new WidgetDetail();
            BeanUtils.copyProperties(widget, widgetDetail);
            widgetDetail.setViewIds(new LinkedList<>());
            widgetDetail.setRelations(new LinkedList<>());
            widgetIds.add(widgetDetail.getId());
            return widgetDetail;
        }).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(widgetIds)) {
            return new ArrayList<>(0);
        }
        Map<String, List<RelWidgetWidget>> relWidgetWidgetsMap = rwwMapper.listTargetWidgetsByIds(widgetIds).stream().collect(Collectors.groupingBy(RelWidgetWidget::getSourceId));
        Map<String, List<RelWidgetElement>> elementsMap = rweMapper.listWidgetElementsByIds(widgetIds).stream().collect(Collectors.groupingBy(RelWidgetElement::getWidgetId));


        for (WidgetDetail widgetDetail : widgetDetails) {
            widgetDetail.setRelations(Optional.ofNullable(relWidgetWidgetsMap.get(widgetDetail.getId())).orElse(new ArrayList<>(0)));
            List<RelWidgetElement> elements = Optional.ofNullable(elementsMap.get(widgetDetail.getId())).orElse(new ArrayList<>(0));
            for (RelWidgetElement element : elements) {
                if (ResourceType.DATACHART.name().equals(element.getRelType())) {
                    if (datachartIds != null) {
                        datachartIds.add(element.getRelId());
                    }
                    widgetDetail.setDatachartId(element.getRelId());
                } else if (ResourceType.VIEW.name().equals(element.getRelType())) {
                    if (viewIds != null) {
                        viewIds.add(element.getRelId());
                    }
                    widgetDetail.getViewIds().add(element.getRelId());
                }
            }
        }
        return widgetDetails;
    }

    @Override
    public boolean importResource(DashboardResourceModel model, ImportStrategy strategy, String orgId) {
        if (model == null) {
            return true;
        }
        switch (strategy) {
            case OVERWRITE:
                importDashboard(model, orgId, true);
                break;
            case ROLLBACK:
                importDashboard(model, orgId, false);
                break;
            default:
                importDashboard(model, orgId, false);
        }
        return true;
    }

    @Override
    public boolean safeDelete(String id) {
        Storypage storypage = new Storypage();
        storypage.setRelId(id);
        return folderMapper.checkUnique(storypage);
    }

    @Override
    public List<Folder> getAllParents(String id) {
        return folderService.getAllParents(id);
    }

    @Override
    public void replaceId(DashboardResourceModel model
            , final Map<String, String> sourceIdMapping
            , final Map<String, String> viewIdMapping
            , final Map<String, String> chartIdMapping
            , final Map<String, String> boardIdMapping
            , final Map<String, String> folderIdMapping) {
        if (model == null || model.getMainModels() == null) {
            return;
        }

        for (DashboardResourceModel.MainModel mainModel : model.getMainModels()) {
            String newId = UUIDGenerator.generate();
            boardIdMapping.put(mainModel.getDashboard().getId(), newId);
            mainModel.getDashboard().setId(newId);
            mainModel.getFolder().setRelId(newId);
            mainModel.getFolder().setId(UUIDGenerator.generate());
            mainModel.getFolder().setParentId(folderIdMapping.get(mainModel.getFolder().getParentId()));

            final Map<String, String> widgetIdMapping = new HashMap<>();
            for (WidgetDetail widget : mainModel.getWidgets()) {
                String widgetId = UUIDGenerator.generate();
                widgetIdMapping.put(widget.getId(), widgetId);
                widget.setId(widgetId);
            }
            for (WidgetDetail widget : mainModel.getWidgets()) {
                widget.setDashboardId(newId);
                widget.setParentId(widgetIdMapping.get(widget.getParentId()));
                widget.setDatachartId(chartIdMapping.get(widget.getDatachartId()));
                if (StringUtils.isNotBlank(widget.getConfig())) {
                    for (Map.Entry<String, String> entry : viewIdMapping.entrySet()) {
                        widget.setConfig(widget.getConfig().replaceAll(entry.getKey(), entry.getValue()));
                    }
                }
                if (CollectionUtils.isNotEmpty(widget.getViewIds())) {
                    widget.setViewIds(widget.getViewIds().stream()
                            .map(viewIdMapping::get)
                            .collect(Collectors.toList()));
                }
                if (widget.getRelations() != null) {
                    for (RelWidgetWidget relation : widget.getRelations()) {
                        relation.setId(UUIDGenerator.generate());
                        relation.setSourceId(widgetIdMapping.get(relation.getSourceId()));
                        relation.setTargetId(widgetIdMapping.get(relation.getTargetId()));
                    }
                }
            }

        }


    }

    private void importDashboard(DashboardResourceModel model,
                                 String orgId,
                                 boolean deleteOld) {
        List<DashboardResourceModel.MainModel> mainModels = model.getMainModels();

        if (CollectionUtils.isNotEmpty(mainModels)) {
            for (DashboardResourceModel.MainModel mainModel : mainModels) {
                Dashboard dashboard = mainModel.getDashboard();
                if (deleteOld) {
                    try {
                        Dashboard retrieve = retrieve(dashboard.getId(), false);
                        if (retrieve != null && !retrieve.getOrgId().equals(orgId)) {
                            Exceptions.msg("message.viz.import.database.conflict");
                        }
                    } catch (NotFoundException ignored) {
                    }
                    try {
                        delete(dashboard.getId(), false, false);
                    } catch (Exception ignore) {
                    }
                }

                // dashboard folder
                Folder folder = mainModel.getFolder();
                folder.setOrgId(orgId);
                try {
                    folderService.checkUnique(orgId, folder.getParentId(), folder.getName());
                } catch (Exception e) {
                    folder.setName(DateUtils.withTimeString(folder.getName()));
                    dashboard.setName(folder.getName());
                }
                folderMapper.insert(folder);
                dashboard.setOrgId(orgId);
                dashboard.setUpdateBy(getCurrentUser().getId());
                dashboard.setUpdateTime(new Date());
                dashboardMapper.insert(dashboard);

                // insert widgets
                List<WidgetDetail> widgets = mainModel.getWidgets();
                if (CollectionUtils.isNotEmpty(widgets)) {
                    for (WidgetDetail widget : widgets) {
                        widgetMapper.insert(widget);
                        List<RelWidgetElement> elements = new LinkedList<>();
                        if (StringUtils.isNotBlank(widget.getDatachartId())) {
                            RelWidgetElement element = new RelWidgetElement();
                            element.setWidgetId(widget.getId());
                            element.setRelId(widget.getDatachartId());
                            element.setRelType(ResourceType.DATACHART.name());
                            element.setId(UUIDGenerator.generate());
                            elements.add(element);
                        }
                        if (CollectionUtils.isNotEmpty(widget.getViewIds())) {
                            for (String viewId : widget.getViewIds()) {
                                RelWidgetElement element = new RelWidgetElement();
                                element.setWidgetId(widget.getId());
                                element.setRelId(viewId);
                                element.setRelType(ResourceType.VIEW.name());
                                element.setId(UUIDGenerator.generate());
                                elements.add(element);
                            }
                        }
                        if (CollectionUtils.isNotEmpty(elements)) {
                            rweMapper.batchInsert(elements);
                        }
                        if (CollectionUtils.isNotEmpty(widget.getRelations())) {
                            rwwMapper.batchInsert(widget.getRelations());
                        }
                    }
                }
                // copy files
                Map<String, byte[]> files = mainModel.getFiles();
                if (files != null && files.size() > 0) {
                    for (Map.Entry<String, byte[]> fileEntry : files.entrySet()) {
                        String name = fileEntry.getKey();
                        String basePath = fileService.getBasePath(FileOwner.DASHBOARD, dashboard.getId());
                        String filePath = FileUtils.concatPath(basePath, name);
                        try {
                            FileUtils.save(filePath, fileEntry.getValue(), false);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

        }
    }

}
