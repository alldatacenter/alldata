package cn.datax.service.system.api.query;

import cn.datax.common.base.BaseQueryParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleQuery extends BaseQueryParams {

    private static final long serialVersionUID=1L;

    /**
     * 角色名称
     */
    private String roleName;
}
