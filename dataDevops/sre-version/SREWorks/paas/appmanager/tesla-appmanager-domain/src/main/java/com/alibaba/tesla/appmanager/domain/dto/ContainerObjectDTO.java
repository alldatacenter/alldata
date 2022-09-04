package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.tesla.appmanager.common.enums.ContainerTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.RepoTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Container 对象 DTO (前端交互)
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerObjectDTO {

    /**
     * 分类
     */
    private ContainerTypeEnum containerType;

    /**
     * 标识
     */
    private String name;

    /**
     * 仓库类型
     */
    private RepoTypeEnum repoType;

    /**
     * 仓库地址
     */
    private String repo;

    /**
     * CI Token
     */
    private String ciToken;

    /**
     * CI Account
     */
    private String ciAccount;

    /**
     * 仓库域名
     */
    private String repoDomain;

    /**
     * 仓库分组
     */
    private String repoGroup;

    /**
     * 仓库路径
     */
    private String repoPath;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 开启 Repo 创建
     */
    private Boolean openCreateRepo = false;

    /**
     * 代码分支
     */
    private String branch;

    /**
     * Dockerfile 模板文件名称
     */
    private String dockerfileTemplate;

    /**
     * Dockerfile 模板文件变量
     */
    private List<ArgMetaDTO> dockerfileTemplateArgs = new ArrayList<>();

    /**
     * 构建变量
     */
    private List<ArgMetaDTO> buildArgs = new ArrayList<>();

    /**
     * 端口定义
     */
    private List<PortMetaDTO> ports = new ArrayList<>();

    /**
     * 命令行定义
     */
    private List<String> command;

    /**
     * 开发语言
     */
    private String language;

    /**
     * 服务类型
     */
    private String serviceType;

    public void initRepo() {
        int appNameIndex = repo.lastIndexOf("/");
        this.appName = repo.substring(appNameIndex + 1, repo.lastIndexOf("."));
        int repoGroupIndex = repo.lastIndexOf("/", appNameIndex - 1);
        this.repoGroup = repo.substring(repoGroupIndex + 1, appNameIndex);
        this.repoDomain = repo.substring(0, repoGroupIndex);
    }
}
