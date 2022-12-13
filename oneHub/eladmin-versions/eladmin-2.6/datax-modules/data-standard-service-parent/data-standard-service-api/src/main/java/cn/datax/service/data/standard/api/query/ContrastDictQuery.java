package cn.datax.service.data.standard.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 字典对照信息表 查询实体
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContrastDictQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String contrastId;
    private String colCode;
    private String colName;
}
