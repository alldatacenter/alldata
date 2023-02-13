package cn.datax.service.data.market.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * api调用日志信息表 查询实体
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiLogQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String apiName;
}
