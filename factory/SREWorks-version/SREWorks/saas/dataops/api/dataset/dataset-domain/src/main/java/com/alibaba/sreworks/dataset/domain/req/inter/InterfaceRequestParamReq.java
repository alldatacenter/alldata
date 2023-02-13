package com.alibaba.sreworks.dataset.domain.req.inter;

import com.alibaba.sreworks.dataset.common.constant.Constant;
import com.alibaba.sreworks.dataset.common.utils.Regex;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 数据接口请求参数
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@Data
@ApiModel(value="数据接口请求参数")
public class InterfaceRequestParamReq {

    @ApiModelProperty(value = "参数名称", example = "appId")
    String name;

    @ApiModelProperty(value = "参数别名", example = "应用ID")
    String alias;

    @ApiModelProperty(value = "参数类型", example = "STRING")
    String type;

    @ApiModelProperty(value = "必填", example = "true")
    Boolean required;

    @ApiModelProperty(value = "默认值", example = "xx")
    String defaultValue;

    public String getAlias() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(alias), "参数别名不允许为空");
        return alias;
    }

    public String getName() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "参数名称不允许为空");
        Preconditions.checkArgument(Regex.checkDocumentByPattern(name, Constant.INTERFACE_NAME_PATTERN),
                "参数名称不符合规范:" + Constant.INTERFACE_NAME_REGEX);
        return name;
    }

    public String getType() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(type), "参数类型不允许为空");
        return type.toUpperCase();
    }

    public Boolean getRequired() {
        Preconditions.checkArgument(required != null, "参数是否必填不允许为空");
        return required;
    }

    public void validReq() {
        getAlias();
        getName();
        getType();
        getRequired();
    }
}
