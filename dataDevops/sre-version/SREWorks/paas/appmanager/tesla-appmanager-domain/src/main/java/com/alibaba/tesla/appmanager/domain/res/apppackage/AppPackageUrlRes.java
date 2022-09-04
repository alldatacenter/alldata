package com.alibaba.tesla.appmanager.domain.res.apppackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 应用包 URL 获取返回结果
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageUrlRes implements Serializable {

    private static final long serialVersionUID = 7899594349944326142L;

    /**
     * 包下载 URL 地址
     */
    private String url;

    /**
     * 包名
     */
    private String filename;
}
