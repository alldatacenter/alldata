package cn.datax.service.system.service;

import cn.datax.common.base.BaseService;
import cn.datax.service.system.api.dto.RoleDto;
import cn.datax.service.system.api.entity.RoleEntity;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author yuwei
 * @since 2019-09-04
 */
public interface RoleService extends BaseService<RoleEntity> {

    RoleEntity saveRole(RoleDto role);

    RoleEntity updateRole(RoleDto role);

    void deleteRoleById(String id);

    void deleteRoleBatch(List<String> ids);
}
