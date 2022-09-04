package com.alibaba.tesla.appmanager.domain.res.imagebuilder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 镜像构建返回结果
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageBuilderCreateRes {

    /**
     * 镜像构建完成后的实际镜像名称
     */
    private String imageName;

    /**
     * sha256
     */
    private String sha256;

    /**
     * 镜像存储目录 (使用完毕后需要自行删除)
     */
    private String imageDir;

    /**
     * 镜像构建的本地存储地址 (绝对路径)，如果 request 中 imagePush == true 则该返回项为空
     */
    private String imagePath;

    /**
     * 日志内容
     */
    private String logContent;
}
