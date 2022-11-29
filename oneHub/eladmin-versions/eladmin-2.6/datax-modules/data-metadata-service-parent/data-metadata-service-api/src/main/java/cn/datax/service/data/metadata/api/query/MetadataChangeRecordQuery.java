package cn.datax.service.data.metadata.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 元数据变更记录表 查询实体
 * </p>
 *
 * @author yuwei
 * @since 2020-07-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MetadataChangeRecordQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String objectId;
    private String fieldName;
}
