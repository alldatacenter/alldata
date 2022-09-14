package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 实时应用实例表_历史
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RtAppInstanceHistoryDO {
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 修改时间
     */
    private Date gmtModified;

    /**
     * 应用实例 ID
     */
    private String appInstanceId;

    /**
     * 状态
     */
    private String status;

    /**
     * 应用实例版本号
     */
    private String version;
}