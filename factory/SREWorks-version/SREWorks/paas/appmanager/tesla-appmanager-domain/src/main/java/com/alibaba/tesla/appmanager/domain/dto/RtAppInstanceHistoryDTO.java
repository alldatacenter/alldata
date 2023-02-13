package com.alibaba.tesla.appmanager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 实时应用实例历史记录 DTO
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RtAppInstanceHistoryDTO {

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
