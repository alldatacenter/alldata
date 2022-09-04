package com.alibaba.sreworks.dataset.domain.bo;

import lombok.Data;

/**
 * 模型数值字段对象
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/08/12 19:08
 */

@Data
public class InterfaceQueryField {

    String field;

    String alias;

    String dim;

    String type;

    String operator;
}
