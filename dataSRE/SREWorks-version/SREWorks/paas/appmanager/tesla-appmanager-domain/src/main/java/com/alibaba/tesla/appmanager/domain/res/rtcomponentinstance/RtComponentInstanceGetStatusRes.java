package com.alibaba.tesla.appmanager.domain.res.rtcomponentinstance;

import com.alibaba.fastjson.JSONArray;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RtComponentInstanceGetStatusRes {

    private String status;

    /**
     * 当前状态详情 (Yaml Array)
     */
    private JSONArray conditions;
}
