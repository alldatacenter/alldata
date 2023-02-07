package cn.datax.service.workflow.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 流程分类表 查询实体
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CategoryQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String name;
}
