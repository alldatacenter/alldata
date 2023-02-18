package com.datasophon.api.master;

import akka.actor.UntypedActor;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.common.Constants;
import com.datasophon.common.command.HdfsEcCommand;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 用于管理hdfs的扩容与缩容
 */
public class HdfsECActor extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger(HdfsECActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable, Throwable {
        if(msg instanceof HdfsEcCommand){

            HdfsEcCommand hdfsEcCommand = (HdfsEcCommand) msg;
            ClusterServiceRoleInstanceService roleInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceService.class);
            //list datanode
            List<ClusterServiceRoleInstanceEntity> datanodes = roleInstanceService.list(new QueryWrapper<ClusterServiceRoleInstanceEntity>().eq(Constants.SERVICE_ID, hdfsEcCommand.getServiceInstanceId()).eq(Constants.SERVICE_ROLE_NAME, "DataNode"));
            TreeSet<String> list = datanodes.stream().map(e -> e.getHostname()).collect(Collectors.toCollection(TreeSet::new));
            ProcessUtils.hdfsEcMethond(hdfsEcCommand.getServiceInstanceId(),roleInstanceService,list,"whitelist","NameNode");

        }else {
            unhandled(msg);
        }
    }


}
