package com.datasophon.api.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.ClusterServiceRoleInstanceConfigMapper;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceConfigEntity;
import com.datasophon.api.service.ClusterServiceRoleInstanceConfigService;


@Service("clusterServiceRoleInstanceConfigService")
public class ClusterServiceRoleInstanceConfigServiceImpl extends ServiceImpl<ClusterServiceRoleInstanceConfigMapper, ClusterServiceRoleInstanceConfigEntity> implements ClusterServiceRoleInstanceConfigService {


}
