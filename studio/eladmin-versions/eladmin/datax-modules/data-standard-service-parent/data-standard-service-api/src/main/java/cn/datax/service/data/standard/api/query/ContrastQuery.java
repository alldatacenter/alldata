package cn.datax.service.data.standard.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 对照表信息表 查询实体
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContrastQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String sourceName;
    private String tableName;
    private String columnName;
}
