package cn.datax.service.system.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 系统参数配置信息表 查询实体
 * </p>
 *
 * @author yuwei
 * @date 2022-05-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;
}
