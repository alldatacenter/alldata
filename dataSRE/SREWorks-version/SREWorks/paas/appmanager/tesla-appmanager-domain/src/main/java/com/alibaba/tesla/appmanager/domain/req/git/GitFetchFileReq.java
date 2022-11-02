package com.alibaba.tesla.appmanager.domain.req.git;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Git 抓取文件请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitFetchFileReq implements Serializable {

    /**
     * 镜像仓库地址
     */
    private String repo;

    /**
     * 分支地址
     */
    private String branch;

    /**
     * 需要获取文件内容的文件路径 (相对路径)
     */
    private String filePath;

    /**
     * CI Account
     */
    private String ciAccount;

    /**
     * CI Token
     */
    private String ciToken;
}
