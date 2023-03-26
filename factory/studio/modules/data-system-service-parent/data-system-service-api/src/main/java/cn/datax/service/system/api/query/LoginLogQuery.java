package cn.datax.service.system.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 登录日志信息表 查询实体
 * </p>
 *
 * @author yuwei
 * @date 2022-05-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginLogQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    /**
     * 登录用户名称
     */
    private String userName;
}
