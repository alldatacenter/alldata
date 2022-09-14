package com.alibaba.sreworks.dataset.domain.bo;

import lombok.Data;

/**
 * 数据接口排序字段
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/21 15:06
 */
@Data
public class InterfaceSortField {

    String dim;

    String order;

    String mode;

    String format;
}
