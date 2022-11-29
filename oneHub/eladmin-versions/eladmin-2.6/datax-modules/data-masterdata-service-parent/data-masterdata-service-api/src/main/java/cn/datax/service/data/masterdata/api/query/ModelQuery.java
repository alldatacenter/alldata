package cn.datax.service.data.masterdata.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 主数据模型表 查询实体
 * </p>
 *
 * @author yuwei
 * @since 2020-08-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String modelName;
}
