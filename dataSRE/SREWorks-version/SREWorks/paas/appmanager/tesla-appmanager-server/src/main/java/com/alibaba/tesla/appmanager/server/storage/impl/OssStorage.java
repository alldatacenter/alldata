package com.alibaba.tesla.appmanager.server.storage.impl;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.server.storage.Storage;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.CannedAccessControlList;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
public class OssStorage extends BaseStorage implements Storage {

    private final OSS ossClient;

    public OssStorage(String endpoint, String accessKey, String secretKey) {
        super(endpoint, accessKey, secretKey);
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
        } catch (Exception e) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "Invalid oss access info", e);
        }
    }

    /**
     * 检测 Bucket 是否存在
     *
     * @param bucketName Bucket 名称
     * @return true or false
     */
    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return ossClient.doesBucketExist(bucketName);
        } catch (Exception e) {
            throw new AppException(AppErrorCode.STORAGE_ERROR,
                    String.format("Check bucket %s existence failed", bucketName), e);
        }
    }

    /**
     * 检测 文件 是否存在
     *
     * @param bucketName Bucket 名称
     * @param objectPath 文件名称
     * @return true or false
     */
    @Override
    public boolean objectExists(String bucketName, String objectPath) {
        try {
            return ossClient.doesObjectExist(bucketName, objectPath);
        } catch (Exception e) {
            throw new AppException(AppErrorCode.STORAGE_ERROR,
                    String.format("Check object %s %s existence failed", bucketName, objectPath), e);
        }
    }

    /**
     * 创建 Bucket
     *
     * @param bucketName     Bucket 名称
     * @param ignoreOnExists 当 Bucket 已经存在时是否忽略异常
     */
    @Override
    public void makeBucket(String bucketName, boolean ignoreOnExists) {
        try {
            ossClient.createBucket(bucketName);
        } catch (Exception e) {
            if (ignoreOnExists
                    && e instanceof OSSException
                    && ((OSSException) e).getErrorCode().equals("BucketAlreadyExists")) {
                log.info("Bucket {} exists now, ignore", bucketName);
                return;
            }
            throw new AppException(AppErrorCode.STORAGE_ERROR,
                    String.format("Create bucket %s failed", bucketName), e);
        }
    }



    /**
     * 上传本地文件到远端存储
     *
     * @param bucketName Bucket 名称
     * @param remotePath 远端文件路径
     * @param localPath  本地文件路径
     */
    @Override
    public void putObject(String bucketName, String remotePath, String localPath) {
        try {
            ossClient.putObject(bucketName, remotePath, new File(localPath));
        } catch (Exception e) {
            throw new AppException(AppErrorCode.STORAGE_ERROR,
                    String.format("[%s] Put object from %s to %s failed", bucketName, localPath, remotePath), e);
        }
        log.info("action=storage.putObject|type=localFile|bucketName={}|remotePath={}|localPath={}",
                bucketName, remotePath, localPath);
    }

    /**
     * 设置文件的权限为公共读
     */
    @Override
    public void setObjectAclPublic(String bucketName, String remotePath){
        ossClient.setObjectAcl(bucketName, remotePath, CannedAccessControlList.PublicRead);
    }


    /**
     * 上传 Stream 到远端存储
     *
     * @param bucketName Bucket 名称
     * @param remotePath 远端文件路径
     * @param stream     文件流
     */
    @Override
    public void putObject(String bucketName, String remotePath, InputStream stream) {
        try {
            int size = stream.available();
            ossClient.putObject(bucketName, remotePath, stream);
            log.info("action=storage.putObject|type=stream|bucketName={}|remotePath={}|size={}",
                    bucketName, remotePath, size);
        } catch (Exception e) {
            throw new AppException(AppErrorCode.STORAGE_ERROR,
                    String.format("[%s] Put stream object to %s failed", bucketName, remotePath), e);
        }
    }

    /**
     * 列出当前指定目录下的所有 Objects 对象
     *
     * @param bucketName Bucket 名称
     * @param parentPath 父路径
     * @return Object 对象路径列表
     */
    @Override
    public List<String> listObjects(String bucketName, String parentPath) {
        List<String> results = new ArrayList<>();
        try {
            ObjectListing objects = ossClient.listObjects(bucketName, parentPath);
            for (OSSObjectSummary obj : objects.getObjectSummaries()) {
                results.add(obj.getKey());
            }
        } catch (Exception e) {
            throw new AppException(AppErrorCode.STORAGE_ERROR,
                    String.format("[%s] List object %s failed", bucketName, parentPath), e);
        }
        return results;
    }

    /**
     * 删除远端储存中的文件
     *
     * @param bucketName Bucket 名称
     * @param remotePath 远端文件路径
     */
    @Override
    public void removeObject(String bucketName, String remotePath) {
        try {
            ossClient.deleteObject(bucketName, remotePath);
        } catch (Exception e) {
            throw new AppException(AppErrorCode.STORAGE_ERROR,
                    String.format("[%s] Remove object %s failed", bucketName, remotePath), e);
        }
        log.info("action=storage.removeObject|bucketName={}|remotePath={}", bucketName, remotePath);
    }

    /**
     * 获取远端存储中指定文件的可下载 URL
     *
     * @param bucketName Bucket 名称
     * @param remotePath 远端文件路径
     * @param expiration 过期时间
     * @return URL 地址
     */
    @Override
    public String getObjectUrl(String bucketName, String remotePath, Integer expiration) {
        Date targetDate = new Date(System.currentTimeMillis() + expiration * 1000);
        String url;
        try {
            url = ossClient.generatePresignedUrl(bucketName, remotePath, targetDate).toString();
        } catch (Exception e) {
            throw new AppException(AppErrorCode.STORAGE_ERROR,
                    String.format("[%s] Get presigned object url failed for remote path %s", bucketName, remotePath), e);
        }
        log.info("action=storage.getObjectUrl|bucketName={}|remotePath={}|url={}", bucketName, remotePath, url);
        return url;
    }
}
