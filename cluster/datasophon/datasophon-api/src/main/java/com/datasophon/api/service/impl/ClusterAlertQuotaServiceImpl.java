package com.datasophon.api.service.impl;

import akka.actor.ActorRef;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.PrometheusActor;
import com.datasophon.api.service.AlertGroupService;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.GenerateAlertConfigCommand;
import com.datasophon.common.model.AlertItem;
import com.datasophon.common.model.Generators;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.AlertGroupEntity;
import com.datasophon.dao.enums.QuotaState;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.ClusterAlertQuotaMapper;
import com.datasophon.dao.entity.ClusterAlertQuota;
import com.datasophon.api.service.ClusterAlertQuotaService;


@Service("clusterAlertQuotaService")
public class ClusterAlertQuotaServiceImpl extends ServiceImpl<ClusterAlertQuotaMapper, ClusterAlertQuota> implements ClusterAlertQuotaService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterAlertQuotaServiceImpl.class);
    @Autowired
    AlertGroupService alertGroupService;

    @Override
    public Result getAlertQuotaList(Integer clusterId, Integer alertGroupId, String quotaName, Integer page, Integer pageSize) {
        Integer offset = (page - 1) * pageSize;
        List<ClusterAlertQuota> list = this.list(new QueryWrapper<ClusterAlertQuota>()
                .eq(alertGroupId != null, Constants.ALERT_GROUP_ID, alertGroupId)
                .like(StringUtils.isNotBlank(quotaName), Constants.ALERT_QUOTA_NAME, quotaName)
                .last("limit " + offset + "," + pageSize));
        int count = this.count(new QueryWrapper<ClusterAlertQuota>()
                .eq(alertGroupId != null, Constants.ALERT_GROUP_ID, alertGroupId)
                .like(StringUtils.isNotBlank(quotaName), Constants.ALERT_QUOTA_NAME, quotaName));
        //查询通知组
        for (ClusterAlertQuota clusterAlertQuota : list) {
            AlertGroupEntity alertGroupEntity = alertGroupService.getById(clusterAlertQuota.getAlertGroupId());
            clusterAlertQuota.setAlertGroupName(alertGroupEntity.getAlertGroupName());
            clusterAlertQuota.setQuotaStateCode(clusterAlertQuota.getQuotaState().getValue());
        }
        return Result.success(list).put(Constants.TOTAL,count);
    }

    @Override
    public Result start(Integer clusterId, String alertQuotaIds) {
        HashMap<String, List<ClusterAlertQuota>> map = new HashMap<>();
        List<String> ids = Arrays.asList(alertQuotaIds.split(","));
        List<ClusterAlertQuota> alertQuotaList = this.list(new QueryWrapper<ClusterAlertQuota>()
                .in(Constants.ID, ids));
        for (ClusterAlertQuota alertQuota : alertQuotaList) {
            if(!map.containsKey(alertQuota.getServiceCategory())){
                ArrayList<ClusterAlertQuota> quotaList = new ArrayList<>();
                quotaList.add(alertQuota);
                map.put(alertQuota.getServiceCategory(),quotaList);
            }else{
                List<ClusterAlertQuota> quotaList = map.get(alertQuota.getServiceCategory());

                quotaList.add(alertQuota);
            }
            alertQuota.setQuotaState(QuotaState.RUNNING);
        }
        if(alertQuotaList.size() > 0){
            logger.info("start alert size is {}",alertQuotaList.size());
            this.updateBatchById(alertQuotaList);
        }
        HashMap<Generators, List<AlertItem>> configFileMap = new HashMap<>();
        for (Map.Entry<String, List<ClusterAlertQuota>> entry : map.entrySet()) {
            String category = entry.getKey();
            List<ClusterAlertQuota> alerts = entry.getValue();
            Generators generators = new Generators();
            generators.setFilename(category.toLowerCase()+".yml");
            generators.setConfigFormat("prometheus");
            generators.setOutputDirectory("alert_rules");
            ArrayList<AlertItem> alertItems = new ArrayList<>();
            for (ClusterAlertQuota clusterAlertQuota : alerts) {
                AlertItem alertItem = new AlertItem();
                alertItem.setAlertName(clusterAlertQuota.getAlertQuotaName());
                alertItem.setAlertExpr(clusterAlertQuota.getAlertExpr()+" "+ clusterAlertQuota.getCompareMethod()+" "+clusterAlertQuota.getAlertThreshold());
                alertItem.setClusterId(clusterId);
                alertItem.setServiceRoleName(clusterAlertQuota.getServiceRoleName());
                alertItem.setAlertLevel(clusterAlertQuota.getAlertLevel().getDesc());
                alertItem.setAlertAdvice(clusterAlertQuota.getAlertAdvice());
                alertItem.setTriggerDuration(clusterAlertQuota.getTriggerDuration());
                alertItems.add(alertItem);
            }
            configFileMap.put(generators,alertItems);
        }
        ActorRef prometheusActor = ActorUtils.getLocalActor(PrometheusActor.class,ActorUtils.getActorRefName(PrometheusActor.class));
        GenerateAlertConfigCommand alertConfigCommand = new GenerateAlertConfigCommand();
        alertConfigCommand.setClusterId(clusterId);
        alertConfigCommand.setConfigFileMap(configFileMap);
        prometheusActor.tell(alertConfigCommand, ActorRef.noSender());
        return Result.success();
    }

    @Override
    public Result stop(Integer clusterId, String alertQuotaIds) {
        return null;
    }

    @Override
    public void saveAlertQuota(ClusterAlertQuota clusterAlertQuota) {
        clusterAlertQuota.setQuotaState(QuotaState.STOPPED);
        clusterAlertQuota.setCreateTime(new Date());
        AlertGroupEntity alertGroupEntity = alertGroupService.getById(clusterAlertQuota.getAlertGroupId());
        clusterAlertQuota.setServiceCategory(alertGroupEntity.getAlertGroupCategory());
        this.save(clusterAlertQuota);
    }

    @Override
    public List<ClusterAlertQuota> listAlertQuotaByServiceName(String serviceName) {
        return this.list(new QueryWrapper<ClusterAlertQuota>().eq(Constants.SERVICE_CATEGORY,serviceName));
    }
}
