package datart.server.service;

import datart.core.entity.Dashboard;
import datart.core.entity.Folder;
import datart.core.mappers.ext.DashboardMapperExt;
import datart.server.base.dto.DashboardBaseInfo;
import datart.server.base.dto.DashboardDetail;
import datart.server.base.params.DashboardCreateParam;
import datart.server.base.transfer.model.DashboardResourceModel;
import datart.server.base.transfer.model.DashboardTemplateModel;

import java.io.IOException;
import java.util.List;

public interface DashboardService extends VizCRUDService<Dashboard, DashboardMapperExt>, ResourceTransferService<Folder, DashboardResourceModel, DashboardTemplateModel,Folder> {

    List<DashboardBaseInfo> listDashboard(String orgId);

    Folder createWithFolder(DashboardCreateParam createParam);

    DashboardDetail getDashboardDetail(String dashboardId);

    Folder copyDashboard(DashboardCreateParam dashboard) throws IOException;

}