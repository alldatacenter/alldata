package com.alibaba.tesla.appmanager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author qianmo.zm@alibaba-inc.com
 * @date 2020/09/28.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RtAppInstanceStatisticsDTO {

    /**
     * 部署数量
     */
    private Long deployCount;

    /**
     * 可更新数量
     */
    private Long upgradeCount;
}
