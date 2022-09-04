package com.alibaba.tesla.appmanager.domain.req.stage;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 创建 Stage 请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageCreateReq implements Serializable {

    private static final long serialVersionUID = 3817173347691347659L;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * 名称
     */
    private String stageName;

    /**
     * 环境扩展信息
     */
    private JSONObject stageExt;
}
