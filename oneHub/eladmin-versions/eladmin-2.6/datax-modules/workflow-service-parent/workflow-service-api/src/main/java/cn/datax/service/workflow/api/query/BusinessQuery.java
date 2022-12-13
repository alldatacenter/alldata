package cn.datax.service.workflow.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 业务流程配置表 查询实体
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String businessCode;
    private String businessName;
}
