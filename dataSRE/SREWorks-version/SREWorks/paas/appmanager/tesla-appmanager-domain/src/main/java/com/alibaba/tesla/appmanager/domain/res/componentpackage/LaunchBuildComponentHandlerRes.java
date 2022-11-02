package com.alibaba.tesla.appmanager.domain.res.componentpackage;

import com.alibaba.tesla.appmanager.domain.core.StorageFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 一个实体 Component Package 的信息类对象
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaunchBuildComponentHandlerRes implements Serializable {

    private static final long serialVersionUID = -5939666573776406007L;

    /**
     * 日志内容
     */
    private String logContent;

    /**
     * 包文件对象（远端）
     */
    private StorageFile storageFile;

    /**
     * 包文件的 meta 信息内容
     */
    private String packageMetaYaml;

    /**
     * 包 Md5
     */
    private String packageMd5;
}
