package com.alibaba.tesla.appmanager.server.storage;

import java.io.InputStream;
import java.util.List;

/**
 * 存储接口层
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface Storage {

    /**
     * 检测 Bucket 是否存在
     *
     * @param bucketName Bucket 名称
     * @return true or false
     */
    boolean bucketExists(String bucketName);

    /**
     * 检测 对象 是否存在
     *
     * @param bucketName Bucket 名称
     * @param objectPath 文件名称
     * @return true or false
     */
    boolean objectExists(String bucketName, String objectPath);

    /**
     * 创建 Bucket
     *
     * @param bucketName     Bucket 名称
     * @param ignoreOnExists 当 Bucket 已经存在时是否忽略异常
     */
    void makeBucket(String bucketName, boolean ignoreOnExists);

    /**
     * 上传本地文件到远端存储
     *
     * @param bucketName Bucket 名称
     * @param remotePath 远端文件路径
     * @param localPath  本地文件路径
     */
    void putObject(String bucketName, String remotePath, String localPath);

    /**
     * 上传 Stream 到远端存储
     *
     * @param bucketName Bucket 名称
     * @param remotePath 远端文件路径
     * @param stream     文件流
     */
    void putObject(String bucketName, String remotePath, InputStream stream);

    /**
     * 列出当前指定目录下的所有 Objects 对象
     *
     * @param bucketName Bucket 名称
     * @param parentPath 父路径
     * @return Object 对象路径列表
     */
    List<String> listObjects(String bucketName, String parentPath);

    /**
     * 删除远端储存中的文件
     *
     * @param bucketName Bucket 名称
     * @param remotePath 远端文件路径
     */
    void removeObject(String bucketName, String remotePath);

    /**
     * 获取远端存储中指定文件的可下载 URL
     *
     * @param bucketName Bucket 名称
     * @param remotePath 远端文件路径
     * @param expiration 过期时间 (s)
     * @return URL 地址
     */
    String getObjectUrl(String bucketName, String remotePath, Integer expiration);

    /**
     * 设置文件的权限为公共读
     */
    void setObjectAclPublic(String bucketName, String remotePath);
}
