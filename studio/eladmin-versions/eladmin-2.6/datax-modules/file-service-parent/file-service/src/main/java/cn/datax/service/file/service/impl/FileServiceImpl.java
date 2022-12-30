package cn.datax.service.file.service.impl;

import cn.datax.service.file.api.entity.FileEntity;
import cn.datax.service.file.dao.FileDao;
import cn.datax.service.file.service.FileService;
import cn.datax.common.base.BaseServiceImpl;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-17
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public abstract class FileServiceImpl extends BaseServiceImpl<FileDao, FileEntity> implements FileService {

    @Autowired
    private FileDao fileDao;

    @Override
    public FileEntity uploadFile(MultipartFile file) {
        FileEntity fileEntity = new FileEntity();
        fileEntity.setContentType(file.getContentType())
                .setOriginalFilename(file.getOriginalFilename())
                .setFileSize(file.getSize());
        String nowDate = DateUtil.format(new Date(), "yyyyMMddHHmmss");
        String extName = FileUtil.extName(fileEntity.getOriginalFilename());
        String fileName = nowDate + "." + extName;
        fileEntity.setFileName(fileName);
        uploadFile(file, fileEntity);
        // 设置文件来源
        fileEntity.setFileType(fileType());
        // 将文件信息保存到数据库
        fileDao.insert(fileEntity);
        return fileEntity;
    }

    @Override
    public void deleteFileById(String id) {
        FileEntity fileEntity = fileDao.selectById(id);
        if (fileEntity != null) {
            fileDao.deleteById(fileEntity.getId());
            deleteFile(fileEntity);
        }
    }

    /**
     * 文件来源
     *
     * @return
     */
    protected abstract String fileType();

    /**
     * 上传文件
     *
     * @param file
     * @param fileEntity
     */
    protected abstract void uploadFile(MultipartFile file, FileEntity fileEntity);

    /**
     * 删除文件资源
     *
     * @param fileEntity
     * @return
     */
    protected abstract void deleteFile(FileEntity fileEntity);
}
