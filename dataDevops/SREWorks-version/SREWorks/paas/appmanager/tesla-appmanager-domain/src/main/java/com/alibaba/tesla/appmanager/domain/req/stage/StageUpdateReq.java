package com.alibaba.tesla.appmanager.domain.req.stage;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 更新 Stage 请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageUpdateReq implements Serializable {

    private static final long serialVersionUID = 8397145986157974088L;

    /**
     * 名称
     */
    private String stageName;

    /**
     * 环境扩展信息
     */
    private JSONObject stageExt;
}
