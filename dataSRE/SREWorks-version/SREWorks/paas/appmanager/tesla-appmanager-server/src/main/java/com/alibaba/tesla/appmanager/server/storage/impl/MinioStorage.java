package com.alibaba.tesla.appmanager.server.storage.impl;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.server.storage.Storage;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MinioStorage extends BaseStorage implements Storage {

    private static final int CONNECT_TIMEOUT_SECS = 5;
    private static final int READ_TIMEOUT_SECS = 600;
    private static final int WRITE_TIMEOUT_SECS = 600;

    private final MinioClient minioClient;

    public MinioStorage(String endpoint, String accessKey, String secretKey) {
        super(endpoint, accessKey, secretKey);
        String[] array = endpoint.split(":");
        try {
            if (array.length == 2) {
                minioClient = new MinioClient(array[0], Integer.parseInt(array[1]), accessKey, secretKey, false);
            } else {
                minioClient = new MinioClient(endpoint, accessKey, secretKey, false);
            }
            minioClient.setTimeout(
                    TimeUnit.SECONDS.toMillis(CONNECT_TIMEOUT_SECS),
                    TimeUnit.SECONDS.toMillis(READ_TIMEOUT_SECS),
                    TimeUnit.SECONDS.toMillis(WRITE_TIMEOUT_SECS)
            );
        } catch (Exception e) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "Invalid minio access info", e);
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
            return minioClient.bucketExists(bucketName);
        } catch (Exception e) {
            throw new AppException(AppErrorCode.STORAGE_ERROR,
                    String.format("Check bucket %s existence failed", bucketName), e);
        }
    }

    /**
     * 设置文件的权限为公共读
     */
    @Override
    public void setObjectAclPublic(String bucketName, String remotePath){
        return;
    }

    /**
     * 检测 对象 是否存在
     *
     * @param bucketName Bucket 名称
     * @param objectPath 文件名称
     * @return true or false
     */
    @Override
    public boolean objectExists(String bucketName, String objectPath) {
        return false;
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
            minioClient.makeBucket(bucketName);
        } catch (Exception e) {
            if (ignoreOnExists
                    && e instanceof ErrorResponseException
                    && ((ErrorResponseException) e).errorResponse().code().equals("BucketAlreadyOwnedByYou")) {
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
            minioClient.putObject(bucketName, remotePath, localPath);
        } catch (Exception e) {
            throw new AppException(AppErrorCode.STORAGE_ERROR,
                    String.format("[%s] Put object from %s to %s failed", bucketName, localPath, remotePath), e);
        }
        log.info("action=storage.putObject|type=localFile|bucketName={}|remotePath={}|localPath={}",
                bucketName, remotePath, localPath);
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
            minioClient.putObject(bucketName, remotePath, stream, "application/octet-stream");
        } catch (Exception e) {
            throw new AppException(AppErrorCode.STORAGE_ERROR,
                    String.format("[%s] Put stream object to %s failed", bucketName, remotePath), e);
        }
        log.info("action=storage.putObject|type=stream|bucketName={}|remotePath={}", bucketName, remotePath);
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
            Iterable<Result<Item>> objects = minioClient.listObjects(bucketName, parentPath, false);
            for (Result<Item> obj : objects) {
                results.add(obj.get().objectName());
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
            minioClient.removeObject(bucketName, remotePath);
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
        String url;
        try {
            url = minioClient.presignedGetObject(bucketName, remotePath, expiration);
        } catch (Exception e) {
            throw new AppException(AppErrorCode.STORAGE_ERROR,
                    String.format("[%s] Get presigned object url failed for remote path %s", bucketName, remotePath), e);
        }
        log.info("action=storage.getObjectUrl|bucketName={}|remotePath={}|url={}", bucketName, remotePath, url);
        return url;
    }
}
