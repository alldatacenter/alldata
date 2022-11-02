package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.model.mapper.SwitchViewUserMapper;
import com.alibaba.tesla.authproxy.model.SwitchViewUserDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.service.AuthPolicy;
import com.alibaba.tesla.authproxy.service.SwitchViewUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SwitchViewUserServiceImpl implements SwitchViewUserService {

    @Autowired
    private SwitchViewUserMapper switchViewUserMapper;

    @Autowired
    private AuthPolicy authPolicy;

    /**
     * 增加新用户
     *
     * @param empId 工号
     */
    @Override
    public int addUser(String empId) throws Exception {
        UserDO user = authPolicy.getAuthServiceManager().getUserByEmpId(empId);
        return switchViewUserMapper.insert(SwitchViewUserDO.builder()
            .empId(empId)
            .loginName(user.getLoginName())
            .bucId(String.valueOf(user.getBucId()))
            .build());
    }

    /**
     * 删除用户
     *
     * @param empId 工号
     */
    @Override
    public int deleteUser(String empId) {
        return switchViewUserMapper.delete(empId);
    }

    /**
     * 获取当前全量的白名单用户列表
     */
    @Override
    public List<SwitchViewUserDO> select() {
        return switchViewUserMapper.select();
    }

    /**
     * 检测指定用户是否存在于白名单中
     *
     * @param empId 工号
     */
    @Override
    public SwitchViewUserDO getByEmpId(String empId) {
        return switchViewUserMapper.getByEmpId(empId);
    }
}
