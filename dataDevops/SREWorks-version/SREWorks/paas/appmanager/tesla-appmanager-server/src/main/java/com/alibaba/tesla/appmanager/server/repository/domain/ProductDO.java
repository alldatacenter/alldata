package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 产品表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDO {
    /**
     * ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

    /**
     * 产品 ID
     */
    private String productId;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 基线 Git 地址
     */
    private String baselineGitAddress;

    /**
     * 基线 Git User
     */
    private String baselineGitUser;

    /**
     * 基线 Git Token
     */
    private String baselineGitToken;
}