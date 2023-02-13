package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.tesla.appmanager.common.enums.RepoTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepoDTO {
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
     * 仓库项目名
     */
    private String repoProject;

    /**
     * 仓库模板
     */
    private String repoTemplate;

    /**
     * 仓库模板URL
     */
    private String repoTemplateUrl;

    /**
     * 仓库路径
     */
    private String repoPath;

    /**
     * dockerfile路径
     */
    private String dockerfilePath;
}
