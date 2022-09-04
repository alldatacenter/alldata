package com.alibaba.sreworks.dataset.domain.req.inter;

import com.alibaba.sreworks.dataset.common.constant.ValidConstant;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 数据接口排序字段
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/21 15:06
 */
@Data
@ApiModel(value="数据接口排序字段")
public class InterfaceSortFieldReq {
    @ApiModelProperty(value ="索引字段", example = "time", required = true)
    String dim;

    @ApiModelProperty(value ="升序/降序", example = "asc", required = true)
    String order;

    @ApiModelProperty(value ="数组排序方式(针对ES)", example = "min")
    String mode;

    @ApiModelProperty(value ="时间格式化方式(针对ES)", example = "strict_date_optional_time_nanos")
    String format;

    public String getDim() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(dim), "索引字段不允许为空");
        return dim;
    }

    public String getOrder() {
        return StringUtils.isEmpty(order) ? ValidConstant.DEFAULT_SORT_MODE : order.toLowerCase();
    }

    public void validReq() {
        getDim();
    }
}
