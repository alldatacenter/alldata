package com.datasophon.api.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.enums.Status;
import com.datasophon.api.security.UserPermission;
import com.datasophon.api.utils.SecurityUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.EncryptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.api.service.UserInfoService;
import com.datasophon.common.utils.Result;


/**
 * 用户信息表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
@RestController
@RequestMapping("api/user")
public class UserInfoController {
    @Autowired
    private UserInfoService userInfoService;

    /**
     * 列表带分页
     */
    @RequestMapping("/list")
    public Result list(String username,Integer page,Integer pageSize) {
        return userInfoService.getUserListByPage(username,page,pageSize);
    }

    /**
     * 查询所有用户
     */
    @RequestMapping("/all")
    public Result all() {
        List<UserInfoEntity> list = userInfoService.list(new QueryWrapper<UserInfoEntity>().ne(Constants.ID,1));
        return Result.success(list);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id) {
        UserInfoEntity userInfo = userInfoService.getById(id);

        return Result.success().put(Constants.DATA, userInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @UserPermission
    public Result save(@RequestBody UserInfoEntity userInfo) {

        return userInfoService.createUser(userInfo);
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @UserPermission
    public Result update(@RequestBody UserInfoEntity userInfo) {
        //用户名判重
        List<UserInfoEntity> list = userInfoService.list(new QueryWrapper<UserInfoEntity>().eq(Constants.USERNAME, userInfo.getUsername()));
        if (Objects.nonNull(list) && list.size() >= 1) {
            UserInfoEntity userInfoEntity = list.get(0);
            if(!userInfoEntity.getId().equals(userInfo.getId())){
                return Result.error(Status.USER_NAME_EXIST.getCode(), Status.USER_NAME_EXIST.getMsg());
            }
        }
        String password = userInfo.getPassword();
        userInfo.setPassword(EncryptionUtils.getMd5(password));
        userInfoService.updateById(userInfo);

        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @UserPermission
    public Result delete(@RequestBody Integer[] ids) {
//        UserInfoEntity authUser = SecurityUtils.getAuthUser();
//        if (!SecurityUtils.isAdmin(authUser)) {
//            return Result.error(Status.USER_NO_OPERATION_PERM.getCode(), Status.USER_NO_OPERATION_PERM.getMsg());
//        }
        if(SecurityUtils.getAuthUser().getId() != 1 ){
            return Result.error(Status.USER_NO_OPERATION_PERM.getMsg());
        }
        userInfoService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
