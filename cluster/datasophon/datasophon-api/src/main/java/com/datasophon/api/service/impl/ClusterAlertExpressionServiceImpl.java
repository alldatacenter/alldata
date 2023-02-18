package com.datasophon.api.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.ClusterAlertExpressionMapper;
import com.datasophon.dao.entity.ClusterAlertExpression;
import com.datasophon.api.service.ClusterAlertExpressionService;


@Service("clusterAlertExpressionService")
public class ClusterAlertExpressionServiceImpl extends ServiceImpl<ClusterAlertExpressionMapper, ClusterAlertExpression> implements ClusterAlertExpressionService {

}
