package cn.datax.service.data.quality.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 核查规则信息表 查询实体
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CheckRuleQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String ruleTypeId;
    private String ruleName;
    private String ruleSource;
    private String ruleTable;
    private String ruleColumn;
}
