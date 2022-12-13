package cn.datax.service.data.quality.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 规则核查项信息表 查询实体
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RuleItemQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String ruleTypeId;
}
