package cn.datax.service.data.visual.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 数据集信息表 查询实体
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DataSetQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String setName;
}
