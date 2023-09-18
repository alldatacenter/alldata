package com.datasophon.api.master;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import cn.hutool.core.util.ObjectUtil;
import com.datasophon.api.service.ClusterHostService;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceCommandService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.GenerateHostPrometheusConfig;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.InstallState;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.StartWorkerMessage;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.MANAGED;
import com.datasophon.dao.enums.ServiceRoleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class WorkerStartActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(WorkerStartActor.class);

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof StartWorkerMessage) {
            StartWorkerMessage msg = (StartWorkerMessage) message;
            logger.info("receive message when worker first start :{}", msg.getHostname());

            ClusterHostService clusterHostService = SpringTool.getApplicationContext().getBean(ClusterHostService.class);
            ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);

            //is managed?
            ClusterHostEntity hostEntity = clusterHostService.getClusterHostByHostname(msg.getHostname());
            ClusterInfoEntity cluster = clusterInfoService.getById(msg.getClusterId());
            logger.info("host install set to 100%");
            if (CacheUtils.constainsKey(cluster.getClusterCode() + Constants.HOST_MAP)) {
                HashMap<String, HostInfo> map = (HashMap<String, HostInfo>) CacheUtils.get(cluster.getClusterCode() + Constants.HOST_MAP);
                HostInfo hostInfo = map.get(msg.getHostname());
                if (Objects.nonNull(hostInfo)) {
                    hostInfo.setProgress(Constants.ONE_HUNDRRD);
                    hostInfo.setInstallState(InstallState.SUCCESS);
                    hostInfo.setInstallStateCode(InstallState.SUCCESS.getValue());
                    hostInfo.setManaged(true);
                }
            }
            if (ObjectUtil.isNull(hostEntity)) {
                //save to db
                ProcessUtils.saveHostInstallInfo(msg, cluster.getClusterCode(), clusterHostService);
                logger.info("host install save to database");
            } else {
                hostEntity.setCpuArchitecture(msg.getCpuArchitecture());
                hostEntity.setManaged(MANAGED.YES);
                clusterHostService.updateById(hostEntity);
            }
            //add to prometheus
            ActorRef prometheusActor = ActorUtils.getLocalActor(PrometheusActor.class,ActorUtils.getActorRefName(PrometheusActor.class));
            GenerateHostPrometheusConfig prometheusConfigCommand = new GenerateHostPrometheusConfig();
            prometheusConfigCommand.setClusterId(cluster.getId());
            prometheusActor.tell(prometheusConfigCommand, getSelf());
        }
    }
}
