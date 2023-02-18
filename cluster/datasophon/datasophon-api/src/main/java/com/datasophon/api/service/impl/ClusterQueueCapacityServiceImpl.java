package com.datasophon.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.enums.Status;
import com.datasophon.api.service.ClusterQueueCapacityService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.utils.HadoopUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterQueueCapacity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.mapper.ClusterQueueCapacityMapper;
import com.datasophon.dao.model.ClusterQueueCapacityList;
import com.datasophon.dao.model.Links;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


@Service("clusterQueueCapacityService")
public class ClusterQueueCapacityServiceImpl extends ServiceImpl<ClusterQueueCapacityMapper, ClusterQueueCapacity> implements ClusterQueueCapacityService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterQueueCapacityServiceImpl.class);

    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;

    @Override
    public Result refreshToYarn(Integer clusterId) throws Exception {
        List<ClusterQueueCapacity> list = this.list(new QueryWrapper<ClusterQueueCapacity>()
                .eq(Constants.CLUSTER_ID, clusterId));
        ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);
        List<ClusterServiceRoleInstanceEntity> roleList = roleInstanceService.getServiceRoleInstanceListByClusterIdAndRoleName(clusterId, "ResourceManager");

        //build configfilemap
        HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
        Generators generators = new Generators();
        generators.setFilename("capacity-scheduler.xml");
        generators.setOutputDirectory("etc/hadoop");
        generators.setConfigFormat("custom");
        generators.setTemplateName("capacity-scheduler.ftl");

        ArrayList<ServiceConfig> serviceConfigs = new ArrayList<>();
        ServiceConfig config = new ServiceConfig();
        ArrayList<JSONObject> queueList = new ArrayList<>();

        for (ClusterQueueCapacity clusterYarnQueue : list) {
            JSONObject queue = new JSONObject();
            BeanUtil.copyProperties(clusterYarnQueue, queue, false);
            queueList.add(queue);
        }

        config.setName("queueList");
        config.setValue(queueList);
        config.setConfigType("map");
        config.setRequired(true);

        serviceConfigs.add(config);

        configFileMap.put(generators, serviceConfigs);
        String hostname = "";
        for (ClusterServiceRoleInstanceEntity roleInstanceEntity : roleList) {
            ExecResult execResult = HadoopUtils.configQueueProp(clusterInfo, configFileMap, roleInstanceEntity);
            if (!execResult.getExecResult()) {
                return Result.error("config capacity-scheduler.xml failed");
            }
            if (StringUtils.isBlank(hostname)) {
                hostname = roleInstanceEntity.getHostname();
            }
        }
        ExecResult execResult = HadoopUtils.refreshQueuePropToYarn(clusterInfo, hostname);
        if (execResult.getExecResult()) {
            logger.info("yarn dfsadmin -refreshQueues success at {}", hostname);
        } else {
            logger.info(execResult.getExecOut());
            return Result.error(Status.FAILED_REFRESH_THE_QUEUE_TO_YARN.getMsg());
        }
        return Result.success();
    }

    @Override
    public void createDefaultQueue(Integer clusterId) {
        ClusterQueueCapacity queueCapacity = new ClusterQueueCapacity();
        queueCapacity.setCapacity("100");
        queueCapacity.setClusterId(clusterId);
        queueCapacity.setQueueName("default");
        queueCapacity.setNodeLabel("*");
        queueCapacity.setAclUsers("*");
        queueCapacity.setParent("root");
        this.save(queueCapacity);
    }

    @Override
    public Result listCapacityQueue(Integer clusterId) {
        List<ClusterQueueCapacity> list = this.list(new QueryWrapper<ClusterQueueCapacity>()
                .eq(Constants.CLUSTER_ID, clusterId));

        ClusterQueueCapacityList clusterQueueCapacityList = new ClusterQueueCapacityList();
        clusterQueueCapacityList.setRootId("root");
        clusterQueueCapacityList.setNodes(list);

        ArrayList<Links> linksList = new ArrayList<>();
        for (ClusterQueueCapacity clusterQueueCapacity : list) {
            Links links = new Links();
            links.setFrom(clusterQueueCapacity.getParent());
            links.setTo(clusterQueueCapacity.getQueueName());
            linksList.add(links);
        }
        clusterQueueCapacityList.setLinks(linksList);
        return Result.success(clusterQueueCapacityList);
    }


}
