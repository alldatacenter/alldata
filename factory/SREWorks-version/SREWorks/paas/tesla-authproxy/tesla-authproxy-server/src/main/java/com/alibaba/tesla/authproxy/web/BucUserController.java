package com.alibaba.tesla.authproxy.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.BaseController;
import com.alibaba.tesla.authproxy.exceptions.UnAuthorizedException;
import com.alibaba.tesla.authproxy.model.TeslaServiceUserDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.model.example.TeslaServiceUserExample;
import com.alibaba.tesla.authproxy.model.example.TeslaServiceUserExample.Criteria;
import com.alibaba.tesla.authproxy.model.teslamapper.TeslaServiceUserMapper;
import com.alibaba.tesla.authproxy.model.vo.TeslaUserInfoVO;
import com.alibaba.tesla.authproxy.model.vo.UserInfoVO;
import com.alibaba.tesla.authproxy.service.BucUserService;
import com.alibaba.tesla.common.base.TeslaResult;
import com.alibaba.tesla.common.utils.TeslaResultBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 兼容老版tesla用户信息相关请求
 *
 * @Date 2019-09-03 14:00
 * @Author cdx
 **/
@Slf4j
@RestController
@RequestMapping("auth/bucUser")
public class BucUserController extends BaseController {

    @Autowired
    protected HttpServletRequest request;
    @Autowired
    private BucUserService bucUserService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuthProperties authProperties;
    @Autowired
    private TeslaServiceUserMapper teslaServiceUserMapper;

    /**
     * 获取当前登录用户的信息,兼容原auth_proxy代理的/getUserInfo和/myself请求
     *
     * @return
     */
    @GetMapping(value = {"/getUserInfo", "/myself"})
    public Map<String, Object> myself(HttpServletResponse response) throws IOException {
        return getUserInfo(response);
    }

    /**
     * 获取当前登录用户的BUC信息
     *
     * @return
     */
    @GetMapping("/bucUserInfo")
    public TeslaResult getBucUserInfo() {
        UserDO userDo = getLoginUser(request);
        UserInfoVO bucUser = bucUserService.getUserByEmpId(userDo.getEmpId());
        return TeslaResultBuilder.successResult(bucUser);
    }

    /**
     * 获取当前登录用户信息迁移老主站getUserInfo
     *
     * @return
     */
    @GetMapping("/userInfo")
    public Map<String, Object> getUserInfo(HttpServletResponse response) throws IOException {
        UserDO userDo = getLoginUser(request);
        UserInfoVO bucUser = bucUserService.getUserByEmpId(userDo.getEmpId());
        if (null == bucUser) {
            throw new UnAuthorizedException();
        }
        Map<String, Object> result = new HashMap<>();

        TeslaUserInfoVO userInfoVO = TeslaUserInfoVO.teslaUserDoToVo(userDo);
        String employeeId = userInfoVO.getEmployeeId();
        /**
         * 硬编码将稳石和缘现设为超级管理员
         */
        if (Objects.equals("110528", employeeId) || Objects.equals("136749", employeeId)) {
            userInfoVO.setIsSuperAdmin(1);
        }
        /**
         * 将原来users表中的userid返回提供给工单鉴权使用
         */
        TeslaServiceUserExample example = new TeslaServiceUserExample();
        Criteria criteria = example.createCriteria();
        criteria.andValidflagEqualTo((byte)1);
        criteria.andEmployeeIdEqualTo(employeeId);
        List<TeslaServiceUserDO> users = teslaServiceUserMapper.selectByExample(example);
        if (users.size() > 0) {
            userInfoVO.setUserid(users.get(0).getUserid());
        }

        result.put("teslaUserInfo", userInfoVO);
        return result;
    }

    /**
     * 原auth_proxy使用，改造后已废弃
     *
     * @param name
     * @return
     */
    @GetMapping("/users/name/{name:.+}")
    public Map<String, Object> getUser(@PathVariable String name, HttpServletResponse response) throws IOException {
        //不存在时返回当前用户
        if (name == null || name.length() == 0) {
            return getUserInfo(response);
        }
        //由于公共账号的原因存在同名,因此此请求的name变为了emplId,因此切换了查询接口
        return bucUserService.getUserInfoByEmpId(name);
    }

    @GetMapping("/users/name")
    public Map<String, Object> getCurrentTeslaUser(HttpServletResponse response) throws IOException {
        return getUserInfo(response);
    }

}
