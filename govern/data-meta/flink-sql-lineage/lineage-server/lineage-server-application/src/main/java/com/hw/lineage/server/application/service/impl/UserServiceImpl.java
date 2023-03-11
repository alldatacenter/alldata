package com.hw.lineage.server.application.service.impl;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.common.util.PageUtils;
import com.hw.lineage.server.application.assembler.DtoAssembler;
import com.hw.lineage.server.application.command.user.CreateUserCmd;
import com.hw.lineage.server.application.command.user.UpdateUserCmd;
import com.hw.lineage.server.application.dto.UserDTO;
import com.hw.lineage.server.application.service.UserService;
import com.hw.lineage.server.domain.entity.Permission;
import com.hw.lineage.server.domain.entity.Role;
import com.hw.lineage.server.domain.entity.User;
import com.hw.lineage.server.domain.query.user.UserCheck;
import com.hw.lineage.server.domain.query.user.UserQuery;
import com.hw.lineage.server.domain.repository.UserRepository;
import com.hw.lineage.server.domain.vo.UserId;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static com.hw.lineage.common.util.Preconditions.checkNotNull;


/**
 * @description: UserServiceImpl
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserRepository repository;

    @Resource
    private DtoAssembler assembler;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = repository.find(username);
        checkNotNull(user, new UsernameNotFoundException(username));

        List<Permission> permissionList = repository.findPermissions(user.getUserId());
        return assembler.fromUserPermissions(user, permissionList);
    }

    @Override
    public Long createUser(CreateUserCmd command) {
        User user = new User()
                .setUsername(command.getUsername())
                .setPassword(command.getPassword());

        user.setCreateTime(System.currentTimeMillis())
                .setModifyTime(System.currentTimeMillis())
                .setInvalid(false);

        user = repository.save(user);
        return user.getUserId().getValue();
    }

    @Override
    public UserDTO queryUser(Long userId) {
        User user = repository.find(new UserId(userId));
        List<Role> roleList = repository.findRoles(user.getUserId());
        return assembler.fromUserRoles(user, roleList);
    }

    @Override
    public Boolean checkUserExist(UserCheck userCheck) {
        return repository.check(userCheck.getUsername());
    }

    @Override
    public PageInfo<UserDTO> queryUsers(UserQuery userQuery) {
        PageInfo<User> pageInfo = repository.findAll(userQuery);
        return PageUtils.convertPage(pageInfo, assembler::fromUser);
    }

    @Override
    public void deleteUser(Long userId) {
        repository.remove(new UserId(userId));
    }

    @Override
    public void updateUser(UpdateUserCmd command) {
        User user = new User()
                .setUserId(new UserId(command.getUserId()))
                .setUsername(command.getUsername())
                .setPassword(command.getPassword())
                .setLocked(command.getLocked());

        user.setModifyTime(System.currentTimeMillis());
        repository.save(user);
    }
}
