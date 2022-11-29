package cn.datax.service.system.service;

import cn.datax.common.base.BaseService;
import cn.datax.common.base.DataScope;
import cn.datax.service.system.api.dto.UserDto;
import cn.datax.service.system.api.dto.UserPasswordDto;
import cn.datax.service.system.api.entity.UserEntity;
import cn.datax.service.system.api.vo.UserInfo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author yuwei
 * @since 2019-09-04
 */
public interface UserService extends BaseService<UserEntity> {

    UserEntity saveUser(UserDto user);

    UserEntity updateUser(UserDto user);

    void deleteUserById(String id);

    void deleteUserBatch(List<String> ids);

    UserInfo getUserByUsername(String username);

    IPage<UserEntity> pageDataScope(IPage<UserEntity> page, Wrapper<UserEntity> queryWrapper, DataScope dataScope);

    void updateUserPassword(UserPasswordDto user);

    void resetUserPassword(UserPasswordDto user);

    Map<String, Object> getRouteById();

    List<UserEntity> getAuditUsers();
}
