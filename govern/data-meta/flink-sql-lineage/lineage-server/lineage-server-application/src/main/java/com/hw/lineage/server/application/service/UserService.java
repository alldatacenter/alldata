package com.hw.lineage.server.application.service;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.server.application.command.user.CreateUserCmd;
import com.hw.lineage.server.application.command.user.UpdateUserCmd;
import com.hw.lineage.server.application.dto.UserDTO;
import com.hw.lineage.server.domain.query.user.UserCheck;
import com.hw.lineage.server.domain.query.user.UserQuery;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * @description: UserService
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface UserService extends UserDetailsService {

    Long createUser(CreateUserCmd command);

    UserDTO queryUser(Long userId);

    Boolean checkUserExist(UserCheck userCheck);

    PageInfo<UserDTO> queryUsers(UserQuery userQuery);

    void deleteUser(Long userId);

    void updateUser(UpdateUserCmd command);
    
}
