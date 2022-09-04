package com.alibaba.tesla.gateway.server.service;


import com.alibaba.tesla.gateway.server.domain.SwitchViewMapping;
import com.alibaba.tesla.gateway.server.domain.SwitchViewUser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 切换视图服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface SwitchViewService {

    /**
     * 检查指定用户是否可以切换为目标用户
     *
     * @param empId       当前用户 EmpId
     * @param switchEmpId 目标用户 EmpId
     * @return view mapping
     */
    Mono<SwitchViewMapping> checkUser(String empId, String switchEmpId);

    /**
     * 获取当前全量的白名单用户列表
     * @return view user
     */
    Flux<SwitchViewUser> getWhiteListUsers();
}
