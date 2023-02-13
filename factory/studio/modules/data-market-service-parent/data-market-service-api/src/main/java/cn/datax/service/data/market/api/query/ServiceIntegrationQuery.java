package cn.datax.service.data.market.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 服务集成表 查询实体
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceIntegrationQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String serviceName;
    private String serviceType;
}
