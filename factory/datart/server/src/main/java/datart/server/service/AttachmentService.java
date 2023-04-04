package datart.server.service;

import datart.core.base.consts.AttachmentType;
import datart.core.base.consts.Const;
import datart.core.base.exception.Exceptions;
import datart.core.common.Application;
import datart.core.common.FileUtils;
import datart.server.base.params.DownloadCreateParam;
import datart.server.service.impl.AttachmentExcelServiceImpl;
import datart.server.service.impl.AttachmentImageServiceImpl;
import datart.server.service.impl.AttachmentPdfServiceImpl;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.File;
import java.util.Calendar;

public interface AttachmentService {

    String SHARE_USER = "SCHEDULER_";

    File getFile(DownloadCreateParam downloadCreateParam, String path, String fileName) throws Exception;

    default String generateFileName(String path, String fileName, AttachmentType attachmentType) {
        path = FileUtils.withBasePath(path);
        String timeStr = DateFormatUtils.format(Calendar.getInstance(), Const.FILE_SUFFIX_DATE_FORMAT);
        String randomStr = RandomStringUtils.randomNumeric(3);
        fileName = fileName + "_" + timeStr + "_" + randomStr + attachmentType.getSuffix();
        return FileUtils.concatPath(path, fileName);
    }

    static AttachmentService matchAttachmentService(AttachmentType type) {
        switch (type) {
            case EXCEL:
                return Application.getBean(AttachmentExcelServiceImpl.class);
            case IMAGE:
                return Application.getBean(AttachmentImageServiceImpl.class);
            case PDF:
                return Application.getBean(AttachmentPdfServiceImpl.class);
            default:
                Exceptions.msg("unsupported download type." + type);
                return null;
        }
    }

}
