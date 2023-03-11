package com.hw.lineage.server.interfaces.controller;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.server.application.command.user.CreateUserCmd;
import com.hw.lineage.server.application.command.user.UpdateUserCmd;
import com.hw.lineage.server.application.dto.UserDTO;
import com.hw.lineage.server.application.service.UserService;
import com.hw.lineage.server.domain.query.user.UserCheck;
import com.hw.lineage.server.domain.query.user.UserQuery;
import com.hw.lineage.server.interfaces.result.Result;
import com.hw.lineage.server.interfaces.result.ResultMessage;
import io.swagger.annotations.Api;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * @description: UserController
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Validated
@RestController
@Api(tags = "Users API")
@RequestMapping("/users")
public class UserController {

    @Resource
    private UserService userService;

    @GetMapping("/{userId}")
    public Result<UserDTO> queryUser(@PathVariable("userId") Long userId) {
        UserDTO userDTO = userService.queryUser(userId);
        return Result.success(ResultMessage.DETAIL_SUCCESS, userDTO);
    }

    @GetMapping("")
    public Result<PageInfo<UserDTO>> queryUsers(UserQuery userQuery) {
        PageInfo<UserDTO> pageInfo = userService.queryUsers(userQuery);
        return Result.success(ResultMessage.QUERY_SUCCESS, pageInfo);
    }

    @PostMapping("")
    public Result<Long> createUser(@Valid @RequestBody CreateUserCmd command) {
        Long userId = userService.createUser(command);
        return Result.success(ResultMessage.CREATE_SUCCESS, userId);
    }

    @GetMapping("/exist")
    public Result<Boolean> checkUserExist(@Valid UserCheck userCheck) {
        return Result.success(ResultMessage.CHECK_SUCCESS, userService.checkUserExist(userCheck));
    }

    @PutMapping("/{userId}")
    public Result<Boolean> updateUser(@PathVariable("userId") Long userId,
                                        @Valid @RequestBody UpdateUserCmd command) {
        command.setUserId(userId);
        userService.updateUser(command);
        return Result.success(ResultMessage.UPDATE_SUCCESS);
    }

    @DeleteMapping("/{userId}")
    public Result<Boolean> deleteUser(@PathVariable("userId") Long userId) {
        userService.deleteUser(userId);
        return Result.success(ResultMessage.DELETE_SUCCESS);
    }

}
