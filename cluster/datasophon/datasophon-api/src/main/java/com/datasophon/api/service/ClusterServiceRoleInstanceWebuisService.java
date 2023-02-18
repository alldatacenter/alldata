package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceWebuis;

/**
 * 集群服务角色对应web ui表 
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-24 16:25:17
 */
public interface ClusterServiceRoleInstanceWebuisService extends IService<ClusterServiceRoleInstanceWebuis> {

    Result getWebUis(Integer serviceInstanceId);

    void removeByServiceInsId(Integer serviceInstanceId);
}

