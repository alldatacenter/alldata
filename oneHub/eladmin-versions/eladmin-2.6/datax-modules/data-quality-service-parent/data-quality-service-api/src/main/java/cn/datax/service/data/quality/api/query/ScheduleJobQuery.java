package cn.datax.service.data.quality.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 数据质量监控任务信息表 查询实体
 * </p>
 *
 * @author yuwei
 * @since 2020-09-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScheduleJobQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;
}
