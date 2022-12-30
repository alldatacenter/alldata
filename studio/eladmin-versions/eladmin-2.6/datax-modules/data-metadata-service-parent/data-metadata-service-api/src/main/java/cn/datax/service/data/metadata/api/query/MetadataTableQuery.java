package cn.datax.service.data.metadata.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 数据库表信息表 查询实体
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MetadataTableQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String sourceId;

    private String tableName;
}
