package cn.datax.service.file.config;

import cn.datax.common.utils.ThrowableUtil;
import cn.datax.service.file.api.entity.FileEntity;
import cn.datax.service.file.properties.FileServerProperties;
import cn.datax.service.file.service.impl.FileServiceImpl;
import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aliyun.oss.OSSClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 阿里云配置
 */
@Configuration
@ConditionalOnProperty(name = "data.file-server.type", havingValue = "aliyun")
public class AliyunOSSAutoConfig {

    private final Logger logger = LoggerFactory.getLogger(AliyunOSSAutoConfig.class);

    @Autowired
    private FileServerProperties fileProperties;

    /**
     * 阿里云文件存储client
     * 只有配置了aliyun.oss.access-key才可以使用
     */
    @Bean
    public OSSClient ossClient() {
        OSSClient ossClient = new OSSClient(fileProperties.getOss().getDomainName()
                , new DefaultCredentialProvider(fileProperties.getOss().getAccessKey(), fileProperties.getOss().getAccessKeySecret())
                , new ClientConfiguration());
        return ossClient;
    }

    @Service
    public class AliyunOssServiceImpl extends FileServiceImpl {
        @Autowired
        private OSSClient ossClient;

        @Override
        protected String fileType() {
            return fileProperties.getType();
        }

        @Override
        protected void uploadFile(MultipartFile file, FileEntity fileEntity) {
            try {
                PutObjectRequest putObjectRequest = new PutObjectRequest(fileProperties.getOss().getBucketName(), fileEntity.getFileName(), file.getInputStream());
                PutObjectResult result = ossClient.putObject(putObjectRequest);
                if(result.getResponse().getStatusCode() == 200){
                    fileEntity.setFilePath(fileProperties.getOss().getDomainName() + File.separator + fileEntity.getFileName());
                }
            } catch (IOException e) {
                logger.error("全局异常信息ex={}, StackTrace={}", e.getMessage(), ThrowableUtil.getStackTrace(e));
            } finally {
                // 关闭OSSClient
                ossClient.shutdown();
            }
        }

        @Override
        protected void deleteFile(FileEntity fileEntity) {
            ossClient.deleteObject(fileProperties.getOss().getBucketName(), fileEntity.getFileName());
        }
    }
}
