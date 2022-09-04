package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.fastjson.JSONArray;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 实时组件实例历史 DTO
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RtComponentInstanceHistoryDTO {

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 修改时间
     */
    private Date gmtModified;

    /**
     * 组件实例 ID
     */
    private String componentInstanceId;

    /**
     * 当前组件归属的应用实例 ID
     */
    private String appInstanceId;

    /**
     * 状态
     */
    private String status;

    /**
     * 当前状态详情 (JSON Array)
     */
    private JSONArray conditions;

    /**
     * 组件实例版本号
     */
    private String version;
}
