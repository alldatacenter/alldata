package cn.datax.service.data.standard.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 数据标准类别表 查询实体
 * </p>
 *
 * @author yuwei
 * @since 2020-08-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TypeQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String gbTypeCode;
    private String gbTypeName;
}
