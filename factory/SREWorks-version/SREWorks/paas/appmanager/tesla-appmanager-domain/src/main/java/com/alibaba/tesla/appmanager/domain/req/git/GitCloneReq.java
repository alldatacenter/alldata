package com.alibaba.tesla.appmanager.domain.req.git;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Git Clone 请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitCloneReq implements Serializable {

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
     * 保留 .git 文件
     */
    private boolean keepGitFiles = false;
}
