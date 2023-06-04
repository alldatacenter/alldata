package com.datasophon.api.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.ClusterAlertRuleMapper;
import com.datasophon.dao.entity.ClusterAlertRule;
import com.datasophon.api.service.ClusterAlertRuleService;


@Service("clusterAlertRuleService")
public class ClusterAlertRuleServiceImpl extends ServiceImpl<ClusterAlertRuleMapper, ClusterAlertRule> implements ClusterAlertRuleService {

}
