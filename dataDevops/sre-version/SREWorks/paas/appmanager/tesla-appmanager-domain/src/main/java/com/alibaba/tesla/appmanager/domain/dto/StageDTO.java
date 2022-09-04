package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * Stage DTO
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageDTO implements Serializable {

    private static final long serialVersionUID = 4831516000808613048L;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 修改时间
     */
    private Date gmtModified;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * 名称
     */
    private String stageName;

    /**
     * 创建者
     */
    private String stageCreator;

    /**
     * 修改者
     */
    private String stageModifier;

    /**
     * 环境扩展信息
     */
    private JSONObject stageExt;
}
