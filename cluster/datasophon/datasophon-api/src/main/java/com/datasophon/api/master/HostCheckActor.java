package com.datasophon.api.master;

import akka.actor.UntypedActor;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.api.service.ClusterHostService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.common.command.HostCheckCommand;
import com.datasophon.common.utils.PromInfoUtils;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HostCheckActor extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(HostCheckActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if(msg instanceof HostCheckCommand){
            logger.info("start to check host info");
            ClusterHostService clusterHostService = SpringTool.getApplicationContext().getBean(ClusterHostService.class);
            ClusterServiceRoleInstanceService roleInstanceService = SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceService.class);
            ClusterInfoService clusterInfoService = SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
            Result result = clusterInfoService.runningClusterList();
            List<ClusterInfoEntity> clusterList = (List<ClusterInfoEntity>) result.getData();

            for (ClusterInfoEntity clusterInfoEntity : clusterList) {
                ClusterServiceRoleInstanceEntity prometheusInstance = roleInstanceService.getOneServiceRole("Prometheus", "", clusterInfoEntity.getId());
                List<ClusterHostEntity> list = clusterHostService.getHostListByClusterId(clusterInfoEntity.getId());
                String promUrl = "http://"+prometheusInstance.getHostname()+":9090/api/v1/query";

                for (ClusterHostEntity clusterHostEntity : list) {
                    try{
                        String hostname = clusterHostEntity.getHostname();
                        //查询内存总量
                        String totalMemPromQl = "node_memory_MemTotal_bytes{job=~\"node\",instance=\""+hostname+":9100\"}/1024/1024/1024";
                        String totalMemStr = PromInfoUtils.getSinglePrometheusMetric(promUrl, totalMemPromQl);
                        if(StringUtils.isNotBlank(totalMemStr)){
                            int totalMem = Double.valueOf(totalMemStr).intValue();
                            clusterHostEntity.setTotalMem(totalMem);
                        }
                        //查询内存使用量
                        String memAvailablePromQl = "node_memory_MemAvailable_bytes{job=~\"node\",instance=\""+hostname+":9100\"}/1024/1024/1024";
                        String memAvailableStr = PromInfoUtils.getSinglePrometheusMetric(promUrl, memAvailablePromQl);
                        if(StringUtils.isNotBlank(memAvailableStr)){
                            int memAvailable = Double.valueOf(memAvailableStr).intValue();
                            Integer memUsed = clusterHostEntity.getTotalMem() - memAvailable;
                            clusterHostEntity.setUsedMem(memUsed);
                        }
                        //总磁盘容量
                        String totalDistPromQl = "sum(node_filesystem_size_bytes{instance=\""+hostname+":9100\",fstype=~\"ext4|xfs\",mountpoint !~\".*pod.*\"})/1024/1024/1024";
                        String totalDiskStr = PromInfoUtils.getSinglePrometheusMetric(promUrl, totalDistPromQl);
                        if(StringUtils.isNotBlank(totalDiskStr)){
                            int totalDisk = Double.valueOf(totalDiskStr).intValue();
                            clusterHostEntity.setTotalDisk(totalDisk);
                        }
                        //查询磁盘使用量
                        String diskUsedPromQl ="sum(node_filesystem_size_bytes{instance=\""+hostname+":9100\",fstype=~\"ext.*|xfs\",mountpoint !~\".*pod.*\"}-node_filesystem_free_bytes{instance=\""+hostname+":9100\",fstype=~\"ext.*|xfs\",mountpoint !~\".*pod.*\"})/1024/1024/1024";
                        String diskUsed = PromInfoUtils.getSinglePrometheusMetric(promUrl, diskUsedPromQl);
                        if(StringUtils.isNotBlank(diskUsed)){
                            clusterHostEntity.setUsedDisk(Double.valueOf(diskUsed).intValue());
                        }
                        //查询cpu负载
                        String cpuLoadPromQl = "node_load5{job=~\"node\",instance=\""+hostname+":9100\"}";
                        String cpuLoad = PromInfoUtils.getSinglePrometheusMetric(promUrl, cpuLoadPromQl);
                        if(StringUtils.isNotBlank(cpuLoad)){
                            clusterHostEntity.setAverageLoad(cpuLoad);
                        }
                    }catch (Exception e){
                        logger.info(e.getMessage());
                    }
                }
                if(list.size() > 0){
                    clusterHostService.updateBatchById(list);
                }
            }


        }else {
            unhandled(msg);
        }
    }
}
