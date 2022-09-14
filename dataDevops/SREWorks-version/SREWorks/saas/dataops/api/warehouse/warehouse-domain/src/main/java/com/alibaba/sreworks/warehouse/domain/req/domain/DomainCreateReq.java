package com.alibaba.sreworks.warehouse.domain.req.domain;

import com.alibaba.sreworks.warehouse.common.constant.DomainConstant;
import com.alibaba.sreworks.warehouse.common.utils.Regex;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;

/**
 * 创建数据域
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@ApiModel(value="创建数据域")
public class DomainCreateReq extends DomainBaseReq {
    @Override
    public String getName() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "域名不允许为空");
        return name;
    }

    @Override
    public String getAbbreviation() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(abbreviation), "域名简写不允许为空");
        Preconditions.checkArgument(Regex.checkDocumentByPattern(abbreviation, DomainConstant.DOAMIN_ABBREVIATION_PATTERN),
                "域名简写不符合规范:" + DomainConstant.DOAMIN_ABBREVIATION_PATTERN);
        return abbreviation.toLowerCase();
    }

    @Override
    public Boolean getBuildIn() {
        if (ObjectUtils.isEmpty(buildIn)) {
            return false;
        }
        return buildIn;
    }
}

