package com.alibaba.sreworks.dataset.domain.req.inter;

import com.alibaba.sreworks.dataset.common.constant.ValidConstant;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.StringUtils;

/**
 * 数据接口分组字段
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@ApiModel(value="数据接口分组字段")
public class InterfaceSortFieldCreateReq extends InterfaceSortFieldBaseReq {

    @Override
    public String getDim() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(dim), "索引字段不允许为空");
        return dim;
    }

    @Override
    public String getOrder() {
        return StringUtils.isEmpty(order) ? ValidConstant.DEFAULT_SORT_MODE : order;
    }
}
