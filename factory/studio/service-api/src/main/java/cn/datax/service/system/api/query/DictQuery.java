package cn.datax.service.system.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 字典编码信息表 查询实体
 * </p>
 *
 * @author AllDataDC
 * @since 2023-01-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DictQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String dictName;
    private String dictCode;
}
