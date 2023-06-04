package datart.server.service;

import datart.core.entity.Datachart;
import datart.core.entity.Folder;
import datart.core.mappers.ext.DatachartMapperExt;
import datart.server.base.dto.DatachartDetail;
import datart.server.base.params.BaseCreateParam;
import datart.server.base.transfer.model.DatachartResourceModel;
import datart.server.base.transfer.model.DatachartTemplateModel;

public interface DatachartService extends VizCRUDService<Datachart, DatachartMapperExt>, ResourceTransferService<Folder, DatachartResourceModel, DatachartTemplateModel, Folder> {

    DatachartDetail getDatachartDetail(String datachartId);

    Folder createWithFolder(BaseCreateParam createParam);
}
