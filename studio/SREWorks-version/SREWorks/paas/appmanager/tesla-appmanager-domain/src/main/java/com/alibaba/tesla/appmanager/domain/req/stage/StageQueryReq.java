package com.alibaba.tesla.appmanager.domain.req.stage;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * Stage 查询请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StageQueryReq extends BaseRequest {

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
     * 生产环境
     */
    private Boolean production;
}
