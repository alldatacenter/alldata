package com.alibaba.tesla.tkgone.server.domain.frontend;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.Set;

/**
 * 前端页面变量
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/05/18 20:02
 */
@Data
public class FrontendPageVar {

    private String id;

    private String type;

    /**
     * 变量名列表
     */
    private Set<String> names;

    /**
     * 依赖变量列表
     */
    private Set<String> dependencies;

    private JSONObject config;
}
