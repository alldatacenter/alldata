package cn.datax.service.data.market.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 数据API脱敏信息表 查询实体
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiMaskQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String maskName;
}
