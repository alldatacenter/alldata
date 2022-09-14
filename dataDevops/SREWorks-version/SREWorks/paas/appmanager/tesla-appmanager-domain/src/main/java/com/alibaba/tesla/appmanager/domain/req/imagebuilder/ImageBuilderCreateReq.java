package com.alibaba.tesla.appmanager.domain.req.imagebuilder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 镜像构建请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageBuilderCreateReq implements Serializable {

    private static final long serialVersionUID = 2859230935260022584L;

    /**
     * Arch (x86/arm/sw6b)
     */
    private String arch;

    /**
     * 使用已有镜像
     */
    private String useExistImage;

    /**
     * 是否上传镜像到 registry 中，如果为 true，需要额外提供 imagePushRegistry 参数。默认 false
     */
    private boolean imagePush;

    /**
     * 上传镜像源地址
     */
    private String imagePushRegistry;

    /**
     * 上传镜像指定 Tag (使用 branch)
     */
    private Boolean imagePushUseBranchAsTag;

    /**
     * 构建好的镜像名称 (不提供则自动生成)
     */
    private String imageName;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 镜像 basename
     */
    private String basename;

    /**
     * 镜像仓库地址
     */
    private String repo;

    /**
     * 分支地址
     */
    private String branch;

    /**
     * Commit Sha256
     */
    private String commit;

    /**
     * repo 前缀目录（相对路径），示例 test
     */
    private String repoPath;

    /**
     * CI Account
     */
    private String ciAccount;

    /**
     * CI Token
     */
    private String ciToken;

    /**
     * Dockerfile 模板文件名称
     */
    private String dockerfileTemplate;

    /**
     * Dockerfile 模板文件参数字典
     */
    private Map<String, String> dockerfileTemplateArgs = new HashMap<>();

    /**
     * Dockerfile 构建参数字典
     */
    private Map<String, String> args = new HashMap<>();
}
