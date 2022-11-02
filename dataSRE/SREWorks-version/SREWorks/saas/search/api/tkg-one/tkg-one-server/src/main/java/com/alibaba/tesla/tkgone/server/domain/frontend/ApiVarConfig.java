package com.alibaba.tesla.tkgone.server.domain.frontend;

import com.alibaba.fastjson.JSONObject;
import lombok.*;

import java.util.List;

/**
 * API类型变量
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/05/18 20:02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiVarConfig {

    private String url;

    private String method;

    private String contentType;

    private JSONObject headers;

    private JSONObject params;

    private String body;

    private List<ApiField> fields;
}
