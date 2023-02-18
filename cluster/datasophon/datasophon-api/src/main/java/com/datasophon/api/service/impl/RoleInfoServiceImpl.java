package com.datasophon.api.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.RoleInfoMapper;
import com.datasophon.dao.entity.RoleInfoEntity;
import com.datasophon.api.service.RoleInfoService;


@Service("roleInfoService")
public class RoleInfoServiceImpl extends ServiceImpl<RoleInfoMapper, RoleInfoEntity> implements RoleInfoService {


}
