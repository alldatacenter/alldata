package cn.datax.service.file.service;

import cn.datax.service.file.api.entity.FileEntity;
import cn.datax.common.base.BaseService;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-17
 */
public interface FileService extends BaseService<FileEntity> {

    FileEntity uploadFile(MultipartFile file);

    void deleteFileById(String id);
}
