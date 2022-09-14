package com.alibaba.tesla.appmanager.domain.core;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Storage 文件容器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageFile {

    /**
     * Bucket 名称
     */
    private String bucketName;

    /**
     * Object 名称（含路径）
     */
    private String objectName;

    /**
     * 初始化，根据 DB 存储的 path 进行分离
     *
     * @param path 存储路径
     */
    public StorageFile(String path) {
        String[] pathArray = path.split("/", 2);
        if (pathArray.length != 2) {
            throw new AppException(AppErrorCode.STORAGE_ERROR, "invalid storage path " + path);
        }
        this.bucketName = pathArray[0];
        this.objectName = pathArray[1];
    }

    /**
     * 转换为数据库存储路径
     *
     * @return bucketName / objectName 形式
     */
    public String toPath() {
        return String.format("%s/%s", bucketName, objectName);
    }
}
