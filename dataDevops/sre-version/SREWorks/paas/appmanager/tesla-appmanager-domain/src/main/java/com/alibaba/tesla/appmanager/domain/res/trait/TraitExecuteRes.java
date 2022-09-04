package com.alibaba.tesla.appmanager.domain.res.trait;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Trait 执行侧 Res 返回结果
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraitExecuteRes {

    private JSONObject spec;
}
