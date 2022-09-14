package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 应用元信息 DTO
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
public class AppMetaDTO {

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

    /**
     * 应用唯一标识
     */
    private String appId;

    /**
     * 应用 Options
     */
    private JSONObject options;

    /**
     * 应用部署环境列表
     */
    private List<AppDeployEnvironmentDTO> environments = new ArrayList<>();
}
