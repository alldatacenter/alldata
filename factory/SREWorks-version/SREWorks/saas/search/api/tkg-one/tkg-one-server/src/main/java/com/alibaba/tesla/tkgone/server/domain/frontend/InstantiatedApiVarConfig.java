package com.alibaba.tesla.tkgone.server.domain.frontend;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;

/**
 * 实例化的API变量类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/05/18 20:02
 */
public class InstantiatedApiVarConfig extends ApiVarConfig {

    @Setter
    @Getter
    private JSONObject dependencyVar;
}
