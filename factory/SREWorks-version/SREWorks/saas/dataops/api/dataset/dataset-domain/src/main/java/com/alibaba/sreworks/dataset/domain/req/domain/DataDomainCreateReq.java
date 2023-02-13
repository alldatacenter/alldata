package com.alibaba.sreworks.dataset.domain.req.domain;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.StringUtils;

/**
 * 模型配置信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@ApiModel(value="数据域创建信息")
public class DataDomainCreateReq extends DataDomainBaseReq {
    @Override
    public String getName() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "domain name must not be empty");
        return name;
    }

    @Override
    public String getAbbreviation() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(abbreviation), "abbreviation must not be empty");
        return abbreviation;
    }

    @Override
    public Integer getSubjectId() {
        Preconditions.checkArgument(subjectId != null, "subject id must not be empty");
        return subjectId;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
