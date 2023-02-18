package com.datasophon.api.service.impl;

import akka.actor.ActorRef;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.PrometheusActor;
import com.datasophon.api.service.*;
import com.datasophon.dao.entity.*;
import com.datasophon.api.service.*;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.GeneratePrometheusConfigCommand;
import com.datasophon.common.model.alert.AlertLabels;
import com.datasophon.common.model.alert.AlertMessage;
import com.datasophon.common.model.alert.Alerts;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.enums.AlertLevel;
import com.datasophon.dao.enums.ServiceRoleState;
import com.datasophon.dao.enums.ServiceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.ClusterAlertHistoryMapper;

import java.util.Date;
import java.util.List;
import java.util.Objects;


@Service("clusterAlertHistoryService")
public class ClusterAlertHistoryServiceImpl extends ServiceImpl<ClusterAlertHistoryMapper, ClusterAlertHistory> implements ClusterAlertHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterAlertHistoryServiceImpl.class);

    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;

    @Autowired
    private ClusterServiceInstanceService serviceInstanceService;

    @Autowired
    private ClusterHostService hostService;

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Override
    public void saveAlertHistory(String alertMessage) {
        AlertMessage alertMes = JSONObject.parseObject(alertMessage, AlertMessage.class);
        List<Alerts> alerts = alertMes.getAlerts();
        for (Alerts alertInfo : alerts) {
            String status = alertInfo.getStatus();
            AlertLabels labels = alertInfo.getLabels();
            int clusterId = labels.getClusterId();
            String instance = labels.getInstance();
            String hostname = instance.split(":")[0];
            //查询告警历史
            ClusterAlertHistory clusterAlertHistory = this.getOne(new QueryWrapper<ClusterAlertHistory>()
                    .eq(Constants.ALERT_TARGET_NAME, labels.getAlertname())
                    .eq(Constants.CLUSTER_ID, labels.getClusterId())
                    .eq(Constants.HOSTNAME, hostname)
                    .eq(Constants.IS_ENABLED, 1));
            String serviceRoleName = labels.getServiceRoleName();
            if ("firing".equals(status)) {//生成告警历史
                //查询服务实例，服务角色实例
                if("node".equals(serviceRoleName)){
                    ClusterHostEntity clusterHost = hostService.getClusterHostByHostname(hostname);
                    if (Objects.isNull(clusterAlertHistory)) {
                        clusterAlertHistory = new ClusterAlertHistory();
                        clusterAlertHistory.setClusterId(clusterId);
                        clusterAlertHistory.setAlertGroupName(labels.getJob());
                        clusterAlertHistory.setAlertTargetName(labels.getAlertname());
                        clusterAlertHistory.setCreateTime(new Date());
                        clusterAlertHistory.setUpdateTime(new Date());
                        if ("warning".equals(labels.getSeverity())) {
                            clusterAlertHistory.setAlertLevel(AlertLevel.WARN);
                            clusterHost.setHostState(3);
                        }
                        clusterAlertHistory.setAlertInfo(alertInfo.getAnnotations().getDescription());
                        clusterAlertHistory.setAlertAdvice(alertInfo.getAnnotations().getSummary());
                        clusterAlertHistory.setHostname(hostname);
                        clusterAlertHistory.setIsEnabled(1);

                        if ("exception".equals(labels.getSeverity())) {
                            clusterAlertHistory.setAlertLevel(AlertLevel.EXCEPTION);
                            clusterHost.setHostState(2);
                        }
                        this.save(clusterAlertHistory);
                    } else {
                        clusterHost.setHostState(3);
                        if ("exception".equals(labels.getSeverity())) {
                            clusterHost.setHostState(2);
                        }
                    }
                    hostService.updateById(clusterHost);
                }else{
                    ClusterServiceRoleInstanceEntity roleInstance = roleInstanceService.getOneServiceRole(serviceRoleName, hostname, clusterId);
                    if (Objects.nonNull(roleInstance)) {
                        ClusterServiceInstanceEntity serviceInstance = serviceInstanceService.getById(roleInstance.getServiceId());
                        if (Objects.isNull(clusterAlertHistory)) {
                            clusterAlertHistory = new ClusterAlertHistory();
                            clusterAlertHistory.setClusterId(clusterId);
                            clusterAlertHistory.setAlertGroupName(labels.getJob());
                            clusterAlertHistory.setAlertTargetName(labels.getAlertname());
                            clusterAlertHistory.setCreateTime(new Date());
                            clusterAlertHistory.setUpdateTime(new Date());
                            clusterAlertHistory.setServiceRoleInstanceId(roleInstance.getId());
                            if ("warning".equals(labels.getSeverity())) {
                                clusterAlertHistory.setAlertLevel(AlertLevel.WARN);
                            }
                            clusterAlertHistory.setAlertInfo(alertInfo.getAnnotations().getDescription());
                            clusterAlertHistory.setAlertAdvice(alertInfo.getAnnotations().getSummary());

                            clusterAlertHistory.setHostname(hostname);
                            clusterAlertHistory.setIsEnabled(1);

                            serviceInstance.setServiceState(ServiceState.EXISTS_ALARM);
                            roleInstance.setServiceRoleState(ServiceRoleState.EXISTS_ALARM);
                            clusterAlertHistory.setServiceInstanceId(serviceInstance.getId());
                            if ("exception".equals(labels.getSeverity())) {
                                clusterAlertHistory.setAlertLevel(AlertLevel.EXCEPTION);
                                serviceInstance.setServiceState(ServiceState.EXISTS_EXCEPTION);
                                //查询服务角色实例
                                roleInstance.setServiceRoleState(ServiceRoleState.STOP);
                            }
                            this.save(clusterAlertHistory);
                        } else {
                            serviceInstance.setServiceState(ServiceState.EXISTS_ALARM);
                            roleInstance.setServiceRoleState(ServiceRoleState.EXISTS_ALARM);
                            if ("exception".equals(labels.getSeverity())) {
                                serviceInstance.setServiceState(ServiceState.EXISTS_EXCEPTION);
                                //查询服务角色实例
                                roleInstance.setServiceRoleState(ServiceRoleState.STOP);
                            }
                        }
                        serviceInstanceService.updateById(serviceInstance);
                        roleInstanceService.updateById(roleInstance);
                    }
                }

            }
            if ("resolved".equals(status)) {
                if (Objects.nonNull(clusterAlertHistory)) {
                    clusterAlertHistory.setIsEnabled(2);
                    List<ClusterAlertHistory> warnAlertList = this.list(new QueryWrapper<ClusterAlertHistory>()
                            .eq(Constants.HOSTNAME, hostname)
                            .eq(Constants.ALERT_GROUP_NAME, serviceRoleName.toLowerCase())
                            .eq(Constants.IS_ENABLED, 1)
                            .eq(Constants.ALERT_LEVEL, AlertLevel.WARN)
                            .ne(Constants.ID,clusterAlertHistory.getId()));
                    if("exception".equals(labels.getSeverity())){//异常告警处理
                        if("node".equals(serviceRoleName)){
                            //置为正常
                            ClusterHostEntity clusterHost = hostService.getClusterHostByHostname(hostname);
                            clusterHost.setHostState(1);
                            if(Objects.nonNull(warnAlertList) && warnAlertList.size() >0){
                                clusterHost.setHostState(3);
                            }
                            hostService.updateById(clusterHost);
                        } else {
                            //查询服务角色实例
                            ClusterServiceRoleInstanceEntity roleInstance = roleInstanceService.getOneServiceRole(labels.getServiceRoleName(), hostname, clusterId);
                            if (roleInstance.getServiceRoleState() != ServiceRoleState.RUNNING) {
                                roleInstance.setServiceRoleState(ServiceRoleState.RUNNING);
                                if(Objects.nonNull(warnAlertList) && warnAlertList.size() >0){
                                    roleInstance.setServiceRoleState(ServiceRoleState.EXISTS_ALARM);
                                }
                                roleInstanceService.updateById(roleInstance);
                            }
                        }
                    }else{
                        //警告告警处理
                        if("node".equals(serviceRoleName)){
                            //置为正常
                            ClusterHostEntity clusterHost = hostService.getClusterHostByHostname(hostname);
                            clusterHost.setHostState(1);
                            if(Objects.nonNull(warnAlertList) && warnAlertList.size() >0){
                                clusterHost.setHostState(3);
                            }
                            hostService.updateById(clusterHost);
                        } else {
                            //查询服务角色实例
                            ClusterServiceRoleInstanceEntity roleInstance = roleInstanceService.getOneServiceRole(labels.getServiceRoleName(), hostname, clusterId);
                            if (roleInstance.getServiceRoleState() != ServiceRoleState.RUNNING) {
                                roleInstance.setServiceRoleState(ServiceRoleState.RUNNING);
                                if(Objects.nonNull(warnAlertList) && warnAlertList.size() >0){
                                    roleInstance.setServiceRoleState(ServiceRoleState.EXISTS_ALARM);
                                }
                                roleInstanceService.updateById(roleInstance);
                            }
                        }
                    }

                    this.updateById(clusterAlertHistory);
                }
            }
        }
    }

    @Override
    public Result getAlertList(Integer serviceInstanceId) {
        List<ClusterAlertHistory> list = this.list(new QueryWrapper<ClusterAlertHistory>()
                .eq(serviceInstanceId != null, Constants.SERVICE_INSTANCE_ID, serviceInstanceId).eq(Constants.IS_ENABLED, 1));
        return Result.success(list);
    }

    @Override
    public Result getAllAlertList(Integer clusterId, Integer page, Integer pageSize) {
        Integer offset = (page - 1) * pageSize;
        List<ClusterAlertHistory> list = this.list(new QueryWrapper<ClusterAlertHistory>()
                .eq( Constants.CLUSTER_ID, clusterId)
                .eq(Constants.IS_ENABLED, 1)
                .last("limit " + offset + "," + pageSize));
        int count = this.count(new QueryWrapper<ClusterAlertHistory>()
                .eq(Constants.CLUSTER_ID, clusterId)
                .eq(Constants.IS_ENABLED, 1));
        return Result.success(list).put(Constants.TOTAL,count);
    }

    @Override
    public void removeAlertByRoleInstanceIds(List<Integer> ids) {
        ClusterServiceRoleInstanceEntity roleInstanceEntity = roleInstanceService.getById(ids.get(0));
        ClusterInfoEntity clusterInfoEntity = clusterInfoService.getById(roleInstanceEntity.getClusterId());
        this.remove(new QueryWrapper<ClusterAlertHistory>()
                .eq(Constants.IS_ENABLED,1)
                .in(Constants.SERVICE_ROLE_INSTANCE_ID,ids));
        //重新配置prometheus
        ActorRef prometheusActor = ActorUtils.getLocalActor(PrometheusActor.class,ActorUtils.getActorRefName(PrometheusActor.class));
        GeneratePrometheusConfigCommand prometheusConfigCommand = new GeneratePrometheusConfigCommand();
        prometheusConfigCommand.setServiceInstanceId(roleInstanceEntity.getServiceId());
        prometheusConfigCommand.setClusterFrame(clusterInfoEntity.getClusterFrame());
        prometheusConfigCommand.setClusterId(roleInstanceEntity.getClusterId());
        prometheusActor.tell(prometheusConfigCommand, ActorRef.noSender());
    }
}
