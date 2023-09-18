package com.datasophon.api.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.dao.mapper.ClusterAlertGroupMapMapper;
import com.datasophon.dao.entity.ClusterAlertGroupMap;
import com.datasophon.api.service.ClusterAlertGroupMapService;


@Service("clusterAlertGroupMapService")
public class ClusterAlertGroupMapServiceImpl extends ServiceImpl<ClusterAlertGroupMapMapper, ClusterAlertGroupMap> implements ClusterAlertGroupMapService {

}
