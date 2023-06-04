package com.datasophon.api.service.impl;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.enums.Status;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.service.ClusterHostService;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterNodeLabelService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.utils.PackageUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterNodeLabelEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.mapper.ClusterNodeLabelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service("clusterNodeLabelService")
@Transactional
public class ClusterNodeLabelServiceImpl extends ServiceImpl<ClusterNodeLabelMapper, ClusterNodeLabelEntity> implements ClusterNodeLabelService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterNodeLabelServiceImpl.class);

    @Autowired
    private ClusterHostService hostService;

    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;

    @Autowired
    private ClusterInfoService clusterInfoService;

//    @Autowired
//    DataSourceTransactionManager dataSourceTransactionManager;
//    @Autowired
//    TransactionDefinition transactionDefinition;

    @Override
    public Result saveNodeLabel(Integer clusterId, String nodeLabel) {
//        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);

        if (repeatNodeLable(clusterId, nodeLabel)) {
            return Result.error(Status.REPEAT_NODE_LABEL.getMsg());
        }
        ClusterNodeLabelEntity nodeLabelEntity = new ClusterNodeLabelEntity();
        nodeLabelEntity.setClusterId(clusterId);
        nodeLabelEntity.setNodeLabel(nodeLabel);
        this.save(nodeLabelEntity);
        //refresh to yarn
        if(!refreshToYarn(clusterId,"-addToClusterNodeLabels",nodeLabel)){
//            dataSourceTransactionManager.rollback(transactionStatus);
            return Result.error(Status.ADD_YARN_NODE_LABEL_FAILED.getMsg());
        }
//        dataSourceTransactionManager.commit(transactionStatus);
        return Result.success();
    }

    private boolean refreshToYarn(Integer clusterId,String type, String nodeLabel) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        List<ClusterServiceRoleInstanceEntity> roleList = roleInstanceService.getServiceRoleInstanceListByClusterIdAndRoleName(clusterId, "ResourceManager");
        if (roleList.size() > 0) {
            String hostname = roleList.get(0).getHostname();
            ActorSelection execCmdActor = ActorUtils.actorSystem.actorSelection("akka.tcp://datasophon@" + hostname + ":2552/user/worker/executeCmdActor");
            ExecuteCmdCommand command = new ExecuteCmdCommand();
            Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
            ArrayList<String> commands = new ArrayList<>();
            commands.add(Constants.INSTALL_PATH +Constants.SLASH+ PackageUtils.getServiceDcPackageName(clusterInfo.getClusterFrame(),"YARN") +"/bin/yarn");
            commands.add("rmadmin");
            commands.add(type);
            commands.add("\"" + nodeLabel + "\"");
            command.setCommands(commands);
            Future<Object> execFuture = Patterns.ask(execCmdActor, command, timeout);
            try {
                ExecResult execResult = (ExecResult) Await.result(execFuture, timeout.duration());
                if (execResult.getExecResult()) {
                    logger.info("add yarn node label success at {}", hostname);
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            logger.info("add yarn node label failed");
            return false;
        }
        return true;
    }

    @Override
    public Result deleteNodeLabel(Integer nodeLabelId) {
//        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        ClusterNodeLabelEntity nodeLabelEntity = this.getById(nodeLabelId);

        if (nodeLabelInUse(nodeLabelEntity.getNodeLabel())) {
            return Result.error(Status.NODE_LABEL_IS_USING.getMsg());
        }
        this.removeById(nodeLabelId);
        if(!refreshToYarn(nodeLabelEntity.getClusterId(),"-removeFromClusterNodeLabels",nodeLabelEntity.getNodeLabel())){
//            dataSourceTransactionManager.rollback(transactionStatus);
            return Result.error(Status.ADD_YARN_NODE_LABEL_FAILED.getMsg());
        }
//        dataSourceTransactionManager.commit(transactionStatus);
        return Result.success();
    }

    @Override
    public Result assignNodeLabel(Integer nodeLabelId, String hostIds) {
//        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        ClusterNodeLabelEntity nodeLabelEntity = this.getById(nodeLabelId);
        List<String> ids = Arrays.asList(hostIds.split(","));
        hostService.updateBatchNodeLabel(ids, nodeLabelEntity.getNodeLabel());

        List<ClusterHostEntity> list = hostService.getHostListByIds(ids);
        String assignNodeLabel = list.stream().map(e -> e.getHostname() + "=" + nodeLabelEntity.getNodeLabel()).collect(Collectors.joining(" "));
        logger.info("assign node label {}",assignNodeLabel);
        //sync to yarn
        //refresh to yarn
        if(!refreshToYarn(nodeLabelEntity.getClusterId(),"-replaceLabelsOnNode",assignNodeLabel)){
//            dataSourceTransactionManager.rollback(transactionStatus);
            return Result.error(Status.ADD_YARN_NODE_LABEL_FAILED.getMsg());
        }
//        dataSourceTransactionManager.commit(transactionStatus);
        return Result.success();
    }

    @Override
    public List<ClusterNodeLabelEntity> queryClusterNodeLabel(Integer clusterId) {
        return this.list(new QueryWrapper<ClusterNodeLabelEntity>().eq(Constants.CLUSTER_ID,clusterId));
    }

    @Override
    public void createDefaultNodeLabel(Integer clusterId) {
        ClusterNodeLabelEntity nodeLabelEntity = new ClusterNodeLabelEntity();
        nodeLabelEntity.setNodeLabel("default");
        nodeLabelEntity.setClusterId(clusterId);
        this.save(nodeLabelEntity);
    }

    private boolean nodeLabelInUse(String nodeLabel) {
        List<ClusterHostEntity> list = hostService.list(new QueryWrapper<ClusterHostEntity>()
                .eq(Constants.NODE_LABEL, nodeLabel));
        if (list.size() > 0) {
            return true;
        }
        return false;
    }

    private boolean repeatNodeLable(Integer clusterId, String nodeLabel) {
        List<ClusterNodeLabelEntity> list = this.list(new QueryWrapper<ClusterNodeLabelEntity>()
                .eq(Constants.CLUSTER_ID, clusterId)
                .eq(Constants.NODE_LABEL, nodeLabel));
        if (list.size() > 0) {
            return true;
        }
        return false;
    }
}
