package com.alibaba.sreworks.dataset.domain.bo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * 数据接口返回参数
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@Data
public class InterfaceResponseParam {

    @JSONField(name = "name")
    String name;

    @JSONField(name = "alias")
    String alias;

    @JSONField(name = "type")
    String type;
}
