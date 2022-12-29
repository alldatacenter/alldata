package datart.server.service.impl;

import datart.core.base.PageInfo;
import datart.core.base.consts.AttachmentType;
import datart.core.common.Application;
import datart.core.common.POIUtils;
import datart.core.data.provider.Dataframe;
import datart.core.entity.View;
import datart.core.entity.poi.POISettings;
import datart.server.base.params.DownloadCreateParam;
import datart.server.base.params.ViewExecuteParam;
import datart.server.common.PoiConvertUtils;
import datart.server.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;

import java.io.File;

@Service("excelAttachmentService")
@Slf4j
public class AttachmentExcelServiceImpl implements AttachmentService {

    protected final AttachmentType attachmentType = AttachmentType.EXCEL;

    private final VizService vizService;

    public AttachmentExcelServiceImpl(VizService vizService) {
        this.vizService = vizService;
    }

    @Override
    public File getFile(DownloadCreateParam downloadParams, String path, String fileName) throws Exception {
        DataProviderService dataProviderService = Application.getBean(DataProviderService.class);
        OrgSettingService orgSettingService = Application.getBean(OrgSettingService.class);
        ViewService viewService = Application.getBean(ViewService.class);

        Workbook workbook = POIUtils.createEmpty();
        for (int i = 0; i < downloadParams.getDownloadParams().size(); i++) {
            ViewExecuteParam viewExecuteParam = downloadParams.getDownloadParams().get(i);
            View view = viewService.retrieve(viewExecuteParam.getViewId(), false);
            viewExecuteParam.setPageInfo(PageInfo.builder().pageNo(1).pageSize(orgSettingService.getDownloadRecordLimit(view.getOrgId())).build());
            Dataframe dataframe = dataProviderService.execute(downloadParams.getDownloadParams().get(i));
            String chartConfigStr = vizService.getChartConfigByVizId(viewExecuteParam.getVizType(), viewExecuteParam.getVizId());
            POISettings poiSettings = PoiConvertUtils.covertToPoiSetting(chartConfigStr, dataframe);
            String sheetName = StringUtils.isNotBlank(viewExecuteParam.getVizName()) ? viewExecuteParam.getVizName() : "Sheet"+i;
            POIUtils.withSheet(workbook, sheetName, dataframe, poiSettings);
        }
        path = generateFileName(path,fileName,attachmentType);
        File file = new File(path);
        POIUtils.save(workbook, file.getPath(), true);
        log.info("create excel file complete.");
        return file;
    }
}
