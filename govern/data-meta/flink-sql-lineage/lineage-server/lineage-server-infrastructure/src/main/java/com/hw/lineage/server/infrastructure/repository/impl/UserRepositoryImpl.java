package com.hw.lineage.server.infrastructure.repository.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import com.hw.lineage.common.exception.LineageException;
import com.hw.lineage.common.util.PageUtils;
import com.hw.lineage.server.domain.entity.Permission;
import com.hw.lineage.server.domain.entity.Role;
import com.hw.lineage.server.domain.entity.User;
import com.hw.lineage.server.domain.query.user.UserQuery;
import com.hw.lineage.server.domain.repository.UserRepository;
import com.hw.lineage.server.domain.vo.UserId;
import com.hw.lineage.server.infrastructure.persistence.converter.DataConverter;
import com.hw.lineage.server.infrastructure.persistence.dos.PermissionDO;
import com.hw.lineage.server.infrastructure.persistence.dos.RoleDO;
import com.hw.lineage.server.infrastructure.persistence.dos.UserDO;
import com.hw.lineage.server.infrastructure.persistence.mapper.PermissionMapper;
import com.hw.lineage.server.infrastructure.persistence.mapper.RoleMapper;
import com.hw.lineage.server.infrastructure.persistence.mapper.UserDynamicSqlSupport;
import com.hw.lineage.server.infrastructure.persistence.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

import static com.hw.lineage.server.infrastructure.persistence.mapper.PermissionDynamicSqlSupport.permission;
import static com.hw.lineage.server.infrastructure.persistence.mapper.RoleDynamicSqlSupport.role;
import static com.hw.lineage.server.infrastructure.persistence.mapper.RolePermissionDynamicSqlSupport.rolePermission;
import static com.hw.lineage.server.infrastructure.persistence.mapper.RoleUserDynamicSqlSupport.roleUser;
import static org.mybatis.dynamic.sql.SqlBuilder.*;


/**
 * @description: UserRepositoryImpl
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Repository
public class UserRepositoryImpl extends AbstractBasicRepository implements UserRepository {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private PermissionMapper permissionMapper;

    @Resource
    private DataConverter converter;

    @Override
    public User find(UserId userId) {
        UserDO userDO = userMapper.selectByPrimaryKey(userId.getValue())
                .orElseThrow(() ->
                        new LineageException(String.format("userId [%s] is not existed", userId.getValue()))
                );
        return converter.toUser(userDO);
    }

    @Override
    public boolean check(String name) {
        return !userMapper.select(completer -> completer.where(UserDynamicSqlSupport.username, isEqualTo(name))).isEmpty();
    }

    @Override
    public User save(User user) {
        UserDO userDO = converter.fromUser(user);
        if (userDO.getUserId() == null) {
            userMapper.insertSelective(userDO);
        } else {
            userMapper.updateByPrimaryKeySelective(userDO);
        }
        return converter.toUser(userDO);
    }

    @Override
    public void remove(UserId userId) {
        userMapper.deleteByPrimaryKey(userId.getValue());
    }

    @Override
    public User find(String username) {
        UserDO userDO = userMapper.selectOne(completer -> completer.where(UserDynamicSqlSupport.username, isEqualTo(username)))
                .orElseThrow(() ->
                        new LineageException("user account or password error")
                );
        return converter.toUser(userDO);
    }

    @Override
    public List<Role> findRoles(UserId userId) {
        List<RoleDO> roleDOList = roleMapper.select(completer ->
                completer.join(roleUser).on(role.roleId, equalTo(roleUser.roleId))
                        .where(roleUser.userId, isEqualTo(userId.getValue()))
        );
        return converter.toRoleList(roleDOList);
    }

    @Override
    public List<Permission> findPermissions(UserId userId) {
        List<PermissionDO> permissionDOList = permissionMapper.select(completer ->
                completer.join(rolePermission).on(permission.permissionId, equalTo(rolePermission.permissionId))
                        .join(roleUser).on(rolePermission.roleId, equalTo(roleUser.roleId))
                        .where(roleUser.userId, isEqualTo(userId.getValue()))
        );
        return converter.toPermissionList(permissionDOList);
    }

    @Override
    public PageInfo<User> findAll(UserQuery userQuery) {
        try (Page<UserDO> page = PageMethod.startPage(userQuery.getPageNum(), userQuery.getPageSize())) {
            PageInfo<UserDO> pageInfo = page.doSelectPageInfo(() ->
                    userMapper.select(completer ->
                            completer.where(UserDynamicSqlSupport.username, isLike(buildLikeValue(userQuery.getUsername())))
                                    .orderBy(buildSortSpecification(userQuery))
                    )
            );
            return PageUtils.convertPage(pageInfo, converter::toUser);
        }
    }
}
