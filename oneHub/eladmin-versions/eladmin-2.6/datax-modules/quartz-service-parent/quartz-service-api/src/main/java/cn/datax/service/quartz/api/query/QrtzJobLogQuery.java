package cn.datax.service.quartz.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 定时任务日志信息表 查询实体
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QrtzJobLogQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    private String jobId;
}
