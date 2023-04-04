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
import datart.core.base.consts.Const;
import datart.core.base.consts.FileOwner;
import datart.core.base.consts.TransferFileType;
import datart.core.base.consts.VariableTypeEnum;
import datart.core.base.exception.Exceptions;
import datart.core.common.*;
import datart.core.entity.*;
import datart.core.mappers.ext.DownloadMapperExt;
import datart.security.base.ResourceType;
import datart.server.base.dto.*;
import datart.server.base.dto.chart.WidgetConfig;
import datart.server.base.params.*;
import datart.server.base.transfer.*;
import datart.server.base.transfer.model.*;
import datart.server.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Slf4j
@Service
public class VizServiceImpl extends BaseService implements VizService {

    private final DatachartService datachartService;

    private final DashboardService dashboardService;

    private final StoryboardService storyboardService;

    private final StorypageService storypageService;

    private final FolderService folderService;

    private final ViewService viewService;

    private final SourceService sourceService;

    private final VariableService variableService;

    private final FileService fileService;

    private final DownloadMapperExt downloadMapper;

    public VizServiceImpl(DatachartService datachartService,
                          DashboardService dashboardService,
                          StoryboardService storyboardService,
                          StorypageService storypageService,
                          FolderService folderService,
                          ViewService viewService,
                          SourceService sourceService, VariableService variableService,
                          FileService fileService, DownloadMapperExt downloadMapper) {
        this.datachartService = datachartService;
        this.dashboardService = dashboardService;
        this.storyboardService = storyboardService;
        this.storypageService = storypageService;
        this.folderService = folderService;
        this.viewService = viewService;
        this.sourceService = sourceService;
        this.variableService = variableService;
        this.fileService = fileService;
        this.downloadMapper = downloadMapper;
    }

    @Override
    public boolean checkName(String orgId, String name, String parentId, ResourceType vizType) {
        if (ResourceType.STORYBOARD == vizType) {
            Storyboard storyboard = new Storyboard();
            storyboard.setName(name);
            storyboard.setOrgId(orgId);
            return storyboardService.checkUnique(storyboard);
        } else {
            return folderService.checkUnique(orgId, parentId, name);
        }
    }

    @Override
    public List<Folder> listViz(String orgId) {
        return folderService.listOrgFolders(orgId);
    }

    @Override
    @Transactional
    public Folder createFolder(FolderCreateParam createParam) {
        return folderService.create(createParam);
    }

    @Override
    @Transactional
    public boolean updateFolder(FolderUpdateParam updateParam) {
        return folderService.update(updateParam);
    }

    @Override
    @Transactional
    public boolean deleteFolder(String folderId) {
        return folderService.delete(folderId);
    }

    @Override
    public List<Storyboard> listStoryboards(String orgId) {
        return storyboardService.listStoryBoards(orgId);
    }

    @Override
    @Transactional
    public boolean publish(ResourceType resourceType, String vizId) {
        switch (resourceType) {
            case DATACHART:
                return datachartService.updateStatus(vizId, Const.VIZ_PUBLISH);
            case DASHBOARD:
                return dashboardService.updateStatus(vizId, Const.VIZ_PUBLISH);
            case STORYBOARD:
                return storyboardService.updateStatus(vizId, Const.VIZ_PUBLISH);
            default:
                Exceptions.msg("unknown viz type " + resourceType);
                return false;
        }
    }

    @Override
    @Transactional
    public boolean unpublish(ResourceType resourceType, String vizId) {
        switch (resourceType) {
            case DATACHART:
                return datachartService.updateStatus(vizId, Const.DATA_STATUS_ACTIVE);
            case DASHBOARD:
                return dashboardService.updateStatus(vizId, Const.DATA_STATUS_ACTIVE);
            case STORYBOARD:
                return storyboardService.updateStatus(vizId, Const.DATA_STATUS_ACTIVE);
            default:
                Exceptions.msg("unknown viz type " + resourceType);
                return false;
        }
    }

    @Override
    @Transactional
    public Folder createDatachart(DatachartCreateParam createParam) {
        return datachartService.createWithFolder(createParam);
    }

    @Override
    @Transactional
    public Folder createDashboard(DashboardCreateParam createParam) {
        return dashboardService.createWithFolder(createParam);
    }

    @Override
    @Transactional
    public Storypage createStorypage(StorypageCreateParam createParam) {
        return storypageService.create(createParam);
    }

    @Override
    @Transactional
    public Storyboard createStoryboard(StoryboardCreateParam createParam) {
        return storyboardService.create(createParam);
    }

    @Override
    public DatachartDetail getDatachart(String datachartId) {
        return datachartService.getDatachartDetail(datachartId);
    }

    @Override
    public DatachartDetailList getDatacharts(Set<String> datachartIds) {
        DatachartDetailList datachartDetailList = new DatachartDetailList();
        datachartDetailList.setDatacharts(new LinkedList<>());
        datachartDetailList.setViews(new LinkedList<>());
        datachartDetailList.setViewVariables(new HashMap<>());
        if (CollectionUtils.isEmpty(datachartIds)) {
            return datachartDetailList;
        }
        for (String datachartId : datachartIds) {
            try {
                datachartDetailList.getDatacharts().add(datachartService.retrieve(datachartId));
            } catch (Exception ignored) {
            }
        }
        for (Datachart datachart : datachartDetailList.getDatacharts()) {
            try {
                datachartDetailList.getViews().add(viewService.retrieve(datachart.getViewId()));
                if (!datachartDetailList.getViewVariables().containsKey(datachart.getViewId())) {
                    List<Variable> variables = variableService.listByView(datachart.getViewId());
                    datachartDetailList.getViewVariables().put(datachart.getViewId(), variables);
                }
            } catch (Exception ignored) {
            }
        }
        List<Variable> orgVariables = variableService.listOrgVariables(datachartDetailList.getDatacharts().get(0).getOrgId());
        orgVariables = orgVariables.stream().filter(v -> v.getType().equals(VariableTypeEnum.QUERY.name()))
                .collect(Collectors.toList());
        datachartDetailList.setOrgVariables(orgVariables);
        return datachartDetailList;
    }

    @Override
    public DashboardDetail getDashboard(String dashboardId) {
        return dashboardService.getDashboardDetail(dashboardId);
    }

    @Override
    public StorypageDetail getStorypage(String storypageId) {
        StorypageDetail storypageDetail = new StorypageDetail();
        Storypage storypage = storypageService.retrieve(storypageId);
        BeanUtils.copyProperties(storypage, storypageDetail);
        switch (ResourceType.valueOf(storypage.getRelType())) {
            case DASHBOARD:
                storypageDetail.setVizDetail(dashboardService.getDashboardDetail(storypage.getRelId()));
                break;
            case DATACHART:
                storypageDetail.setVizDetail(datachartService.getDatachartDetail(storypage.getRelId()));
            default:
                Exceptions.msg("unknown viz type " + storypage.getRelType());
        }
        return storypageDetail;
    }

    @Override
    public List<Storypage> listStorypages(String storyboardId) {
        return storypageService.listByStoryboard(storyboardId);
    }

    @Override
    public StoryboardDetail getStoryboard(String storyboardId) {
        return storyboardService.getStoryboard(storyboardId);
    }

    @Override
    public String getChartConfigByVizId(ResourceType resourceType, String vizId) {
        String result = "";
        try {
            switch (resourceType) {
                case DATACHART:
                    return retrieve(vizId, Datachart.class).getConfig();
                case WIDGET:
                    String config = retrieve(vizId, Widget.class).getConfig();
                    WidgetConfig widgetConfig = JSON.parseObject(config, WidgetConfig.class);
                    return widgetConfig.getChartConfig();
                default:
                    return result;
            }
        } catch (Exception e) {
            log.warn("query chart(" + vizId + ") config fail, download with none style.");
        }
        return result;
    }

    @Override
    public Download exportResource(ResourceTransferParam param) throws IOException {
        final String user = securityManager.getCurrentUser().getUsername();

        return newExportDownloadTask(transferParam -> {
            securityManager.runAs(user);
            TransferConfig transferConfig = TransferConfig.builder()
                    .withParents(true)
                    .build();
            List<ResourceTransferParam.VizItem> resources = transferParam.getResources();
            ResourceModel resourceModel = new ResourceModel();
            final Set<String> dashboards = new HashSet<>();
            final Set<String> datacharts = new HashSet<>();
            final Set<String> views = new HashSet<>();
            final Set<String> sources = new HashSet<>();
            final Set<String> folders = new HashSet<>();
            dashboards.addAll(resources
                    .parallelStream()
                    .filter(vizItem -> ResourceType.DASHBOARD.equals(vizItem.getResourceType()))
                    .map(ResourceTransferParam.VizItem::getResourceId)
                    .collect(Collectors.toSet()));
            datacharts.addAll(resources
                    .parallelStream()
                    .filter(vizItem -> ResourceType.DATACHART.equals(vizItem.getResourceType()))
                    .map(ResourceTransferParam.VizItem::getResourceId)
                    .collect(Collectors.toSet()));
            if (dashboards.size() > 0) {
                DashboardResourceModel dashboardResourceModel = dashboardService.exportResource(transferConfig, dashboards);
                resourceModel.setDashboardResourceModel(dashboardService.exportResource(transferConfig, dashboards));
                datacharts.addAll(dashboardResourceModel.getDatacharts());
                views.addAll(dashboardResourceModel.getViews());
                if (dashboardResourceModel.getParents() != null) {
                    folders.addAll(dashboardResourceModel.getParents());
                }
            }
            if (datacharts.size() > 0) {
                DatachartResourceModel datachartResourceModel = datachartService.exportResource(transferConfig, datacharts);
                resourceModel.setDatachartResourceModel(datachartResourceModel);
                views.addAll(datachartResourceModel.getViews());
                if (datachartResourceModel.getParents() != null) {
                    folders.addAll(datachartResourceModel.getParents());
                }
            }
            if (views.size() > 0) {
                ViewResourceModel viewResourceModel = viewService.exportResource(transferConfig, views);
                resourceModel.setViewResourceModel(viewResourceModel);
                sources.addAll(viewResourceModel.getSources());
            }
            if (sources.size() > 0) {
                resourceModel.setSourceResourceModel(sourceService.exportResource(transferConfig, sources));
            }
            if (folders.size() > 0) {
                resourceModel.setFolderTransferModel(folderService.exportResource(transferConfig, folders));
            }
            return resourceModel;
        }, param, TransferFileType.DATART_RESOURCE_FILE);
    }

    @Override
    @Transactional
    public boolean importResource(MultipartFile file, ImportStrategy importStrategy, String orgId) throws IOException {
        ResourceModel model = null;
        try {
            model = (ResourceModel) extractModel(file);
        } catch (Exception e) {
            log.error("viz model extract error ", e);
            Exceptions.e(e);
        }
        if (model == null) {
            Exceptions.msg("message.viz.import.invalid");
            return false;
        }
        Organization organization = retrieve(orgId, Organization.class);
        if (organization == null) {
            Exceptions.msg("The target organization does not exist");
        }
        securityManager.requireOrgOwner(orgId);
        if (ImportStrategy.NEW.equals(importStrategy)) {
            final Map<String, String> sourceIdMapping = new HashMap<>();
            final Map<String, String> viewIdMapping = new HashMap<>();
            final Map<String, String> datachartIdMapping = new HashMap<>();
            final Map<String, String> boardIdMapping = new HashMap<>();
            final Map<String, String> folderIdMapping = new HashMap<>();
            // replace folder Id
            folderService.replaceId(model.getFolderTransferModel(), sourceIdMapping, viewIdMapping, datachartIdMapping, boardIdMapping, folderIdMapping);
            // replace source Id
            sourceService.replaceId(model.getSourceResourceModel(), sourceIdMapping, viewIdMapping, datachartIdMapping, boardIdMapping, folderIdMapping);
            // replace view id
            viewService.replaceId(model.getViewResourceModel(), sourceIdMapping, viewIdMapping, datachartIdMapping, boardIdMapping, folderIdMapping);
            // replace datachart id
            datachartService.replaceId(model.getDatachartResourceModel(), sourceIdMapping, viewIdMapping, datachartIdMapping, boardIdMapping, folderIdMapping);
            //replace dashboard id
            dashboardService.replaceId(model.getDashboardResourceModel(), sourceIdMapping, viewIdMapping, datachartIdMapping, boardIdMapping, folderIdMapping);
        }
        dashboardService.importResource(model.getDashboardResourceModel(), importStrategy, orgId);
        datachartService.importResource(model.getDatachartResourceModel(), importStrategy, orgId);
        viewService.importResource(model.getViewResourceModel(), importStrategy, orgId);
        sourceService.importResource(model.getSourceResourceModel(), importStrategy, orgId);
        folderService.importResource(model.getFolderTransferModel(), importStrategy, orgId);
        return true;
    }

    @Override
    public Download exportDatachartTemplate(DatachartTemplateParam param) {
        return newExportDownloadTask(templateParam -> {
            DatachartTemplateModel templateModel = new DatachartTemplateModel();
            templateModel.setDatachart(param.getDatachart());
            return templateModel;
        }, param, TransferFileType.DATART_TEMPLATE_FILE);
    }

    @Override
    public Download exportDashboardTemplate(DashboardTemplateParam param) {
        return newExportDownloadTask(templateParam -> {
            DashboardTemplateModel templateModel = new DashboardTemplateModel();
            templateModel.setDashboard(param.getDashboard());
            templateModel.setWidgets(param.getWidgets());
            templateModel.setFiles(FileUtils.walkDirAsStream(new File(fileService.getBasePath(FileOwner.DASHBOARD, param.getDashboard().getId()))
                    , null,
                    false));
            return templateModel;
        }, param, TransferFileType.DATART_TEMPLATE_FILE);
    }


    private <P extends TransferParam, R extends TransferModel> Download newExportDownloadTask(Function<P, R> function, P param, TransferFileType fileType) {
        final Download download = new Download();
        download.setCreateTime(new Date());
        download.setId(UUIDGenerator.generate());
        download.setStatus((byte) 0);
        download.setCreateBy(securityManager.getCurrentUser().getId());
        String path = getExportFile(getMessage("message.viz.export.name"), fileType);
        download.setName(new File(path).getName());
        download.setPath(path);
        downloadMapper.insert(download);
        TaskExecutor.submit(() -> {
            try {
                TransferModel model = function.apply(param);
                SerializerUtils.serializeObjectToFile(model, true, download.getPath());
                download.setStatus((byte) 1);
            } catch (Exception e) {
                download.setStatus((byte) -1);
                log.error("object serialize error", e);
            } finally {
                try {
                    download.setUpdateTime(new Date());
                    downloadMapper.updateByPrimaryKey(download);
                } catch (Exception e) {
                    log.error("download task update error", e);
                }
                try {
                    securityManager.releaseRunAs();
                } catch (Exception ignored) {
                }
            }
        });
        return download;
    }

    @Override
    @Transactional
    public Folder importVizTemplate(MultipartFile file, String orgId, String parentId, String name) throws Exception {
        Folder parent = null;
        if (StringUtils.isNotBlank(parentId)) {
            parent = folderService.retrieve(parentId);
        }
        TransferModel transferModel = null;
        transferModel = extractModel(file);
        datart.core.common.BeanUtils.validate(transferModel);
        if (transferModel instanceof DatachartTemplateModel) {
            return datachartService.importTemplate((DatachartTemplateModel) transferModel, orgId, name, parent);
        } else if (transferModel instanceof DashboardTemplateModel) {
            return dashboardService.importTemplate((DashboardTemplateModel) transferModel, orgId, name, parent);
        }
        return null;
    }


    @Override
    public boolean updateStoryboardBase(StoryboardBaseUpdateParam updateParam) {
        return storyboardService.updateBase(updateParam);
    }

    @Override
    @Transactional
    public boolean updateDatachart(DatachartUpdateParam updateParam) {
        // update folder
        Folder vizFolder = folderService.getVizFolder(updateParam.getId(), ResourceType.DATACHART.name());
        vizFolder.setAvatar(updateParam.getAvatar());
        vizFolder.setSubType(updateParam.getSubType());
        folderService.getDefaultMapper().updateByPrimaryKey(vizFolder);

        return datachartService.update(updateParam);
    }

    @Override
    @Transactional
    public boolean updateDashboard(DashboardUpdateParam updateParam) {
        // update folder
        Folder vizFolder = folderService.getVizFolder(updateParam.getId(), ResourceType.DASHBOARD.name());
        vizFolder.setAvatar(updateParam.getAvatar());
        vizFolder.setSubType(updateParam.getSubType());
        folderService.getDefaultMapper().updateByPrimaryKey(vizFolder);
        return dashboardService.update(updateParam);
    }

    @Override
    @Transactional
    public boolean updateStorypage(StorypageUpdateParam updateParam) {
        return storypageService.update(updateParam);
    }

    @Override
    @Transactional
    public boolean updateStoryboard(StoryboardUpdateParam updateParam) {
        return storyboardService.update(updateParam);
    }

    @Override
    @Transactional
    public boolean deleteDatachart(String datachartId, boolean archive) {
        return datachartService.delete(datachartId, archive);
    }

    @Override
    @Transactional
    public boolean deleteDashboard(String dashboardId, boolean archive) {
        return dashboardService.delete(dashboardId, archive);
    }

    @Override
    @Transactional
    public Folder copyDashboard(DashboardCreateParam createParam) throws IOException {
        return dashboardService.copyDashboard(createParam);
    }

    @Override
    @Transactional
    public boolean deleteStorypage(String storypageId) {
        return storypageService.delete(storypageId);
    }

    @Override
    @Transactional
    public boolean deleteStoryboard(String storyboardId, boolean archive) {
        return storyboardService.delete(storyboardId, archive);
    }

    @Override
    public List<Datachart> listArchivedDatachart(String orgId) {
        return datachartService.listArchived(orgId);
    }

    @Override
    public List<Dashboard> listArchivedDashboard(String orgId) {
        return dashboardService.listArchived(orgId);
    }

    @Override
    public List<Storyboard> listArchivedStoryboard(String orgId) {
        return storyboardService.listArchived(orgId);
    }

    @Override
    @Transactional
    public boolean unarchiveViz(String vizId, ResourceType vizType, String newName, String parentId, double index) {
        switch (vizType) {
            case DASHBOARD:
                Dashboard dashboard = dashboardService.retrieve(vizId);
                dashboardService.requirePermission(dashboard, Const.MANAGE);
                //check name
                folderService.checkUnique(dashboard.getOrgId(), parentId, newName);
                // add to folder
                createFolder(vizType, vizId, newName, dashboard.getOrgId(), parentId, index);
                dashboard.setName(newName);
                dashboard.setStatus(Const.DATA_STATUS_ACTIVE);
                //update status
                return 1 == dashboardService.getDefaultMapper().updateByPrimaryKey(dashboard);
            case DATACHART:
                Datachart datachart = datachartService.retrieve(vizId);
                datachartService.requirePermission(datachart, Const.MANAGE);
                //check name
                folderService.checkUnique(datachart.getOrgId(), parentId, newName);
                //update status
                datachart.setName(newName);
                datachart.setStatus(Const.DATA_STATUS_ACTIVE);
                // add to folder
                createFolder(vizType, vizId, newName, datachart.getOrgId(), parentId, index);
                return 1 == datachartService.getDefaultMapper().updateByPrimaryKey(datachart);
            case STORYBOARD:
                return storyboardService.unarchive(vizId, newName, parentId, 0D);
            default:
                Exceptions.msg("unknown viz type");
                return false;
        }
    }

    private void createFolder(ResourceType type, String id, String name, String orgId, String parentId, double index) {
        Folder folder = new Folder();
        folder.setId(UUIDGenerator.generate());
        folder.setRelType(type.name());
        folder.setRelId(id);
        folder.setParentId(parentId);
        folder.setOrgId(orgId);
        folder.setName(name);
        folder.setIndex(index);
        folderService.getDefaultMapper().insert(folder);
    }

    private String getExportFile(String name, TransferFileType fileType) {
        return fileService.getBasePath(FileOwner.EXPORT, null) + "/" + name + "-" + System.currentTimeMillis() + fileType.getSuffix();
    }

    public TransferModel extractModel(MultipartFile file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream inputStream = new ObjectInputStream(new GZIPInputStream(file.getInputStream()));) {
            return (TransferModel) inputStream.readObject();
        }
    }
}