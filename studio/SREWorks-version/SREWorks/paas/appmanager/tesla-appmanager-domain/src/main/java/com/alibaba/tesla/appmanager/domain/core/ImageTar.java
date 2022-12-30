package com.alibaba.tesla.appmanager.domain.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Image Tar
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageTar {

    /**
     * 架构 (x86/arm/sw6b)
     */
    private String arch = "";

    /**
     * 镜像名称 (在压缩包中的文件名)
     */
    private String name = "";

    /**
     * Schema 定义中的镜像地址 (reg.docker.alibaba-inc.com/abm-aone/paas-service:live
     */
    private String image = "";

    /**
     * Sha256
     */
    private String sha256 = "";

    /**
     * 实际目标环境中，应该被替换为什么镜像地址
     */
    private String actualImage = "";
}
