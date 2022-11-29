package cn.datax.service.file.config;

import cn.datax.common.utils.ThrowableUtil;
import cn.datax.service.file.api.entity.FileEntity;
import cn.datax.service.file.properties.FileServerProperties;
import cn.datax.service.file.service.impl.FileServiceImpl;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.qiniu.common.Zone;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 七牛云配置
 */
@Configuration
@ConditionalOnProperty(name = "data.file-server.type", havingValue = "qiniu")
public class QiniuOSSAutoConfig {

    private final Logger logger = LoggerFactory.getLogger(QiniuOSSAutoConfig.class);

    @Autowired
    private FileServerProperties fileProperties;

    /**
     * 华东机房
     */
    @Bean
    public com.qiniu.storage.Configuration qiniuConfig() {
        return new com.qiniu.storage.Configuration(Zone.zone2());
    }

    /**
     * 构建一个七牛上传工具实例
     */
    @Bean
    public UploadManager uploadManager() {
        return new UploadManager(qiniuConfig());
    }

    /**
     * 认证信息实例
     *
     * @return
     */
    @Bean
    public Auth auth() {
        return Auth.create(fileProperties.getOss().getAccessKey(), fileProperties.getOss().getAccessKeySecret());
    }

    /**
     * 构建七牛空间管理实例
     */
    @Bean
    public BucketManager bucketManager() {
        return new BucketManager(auth(), qiniuConfig());
    }

    @Service
    public class QiniuOssServiceImpl extends FileServiceImpl {
        @Autowired
        private UploadManager uploadManager;
        @Autowired
        private BucketManager bucketManager;
        @Autowired
        private Auth auth;

        @Override
        protected String fileType() {
            return fileProperties.getType();
        }

        @Override
        protected void uploadFile(MultipartFile file, FileEntity fileEntity) {
            try {
                Response response = uploadManager.put(file.getBytes(), fileEntity.getFileName(), auth.uploadToken(fileProperties.getOss().getBucketName()));
                if (response.statusCode == 200) {
                    fileEntity.setFilePath(fileProperties.getOss().getDomainName() + File.separator + fileEntity.getFileName());
                }
            } catch (IOException e) {
                logger.error("全局异常信息ex={}, StackTrace={}", e.getMessage(), ThrowableUtil.getStackTrace(e));
            }
        }

        @Override
        protected void deleteFile(FileEntity fileEntity) {
            try {
                Response response = bucketManager.delete(fileProperties.getOss().getBucketName(), fileEntity.getFileName());
            } catch (QiniuException e) {
                logger.error("全局异常信息ex={}, StackTrace={}", e.getMessage(), ThrowableUtil.getStackTrace(e));
            }
        }
    }
}
