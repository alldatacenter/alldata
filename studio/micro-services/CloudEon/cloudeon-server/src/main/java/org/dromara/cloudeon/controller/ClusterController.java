package org.dromara.cloudeon.controller;

import cn.hutool.core.bean.BeanUtil;
import org.dromara.cloudeon.controller.request.ModifyClusterInfoRequest;
import org.dromara.cloudeon.controller.response.ClusterInfoVO;
import org.dromara.cloudeon.dao.*;
import org.dromara.cloudeon.dto.ResultDTO;
import org.dromara.cloudeon.entity.ClusterAlertRuleEntity;
import org.dromara.cloudeon.entity.ClusterInfoEntity;
import org.dromara.cloudeon.service.KubeService;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.dromara.cloudeon.utils.Constant.AdminUserName;

/**
 * Cluster Controller
 * vue axios 的POST请求必须使用 @RequestBody 接收
 *
 */
@RestController
@RequestMapping("/cluster")
public class ClusterController {


    @Resource
    private ClusterInfoRepository clusterInfoRepository;

    @Resource
    private StackInfoRepository stackInfoRepository;

    @Resource
    private ClusterNodeRepository clusterNodeRepository;

    @Resource
    private ServiceInstanceRepository serviceInstanceRepository;

    @Resource
    StackAlertRuleRepository stackAlertRuleRepository;

    @Resource
    private ClusterAlertRuleRepository clusterAlertRuleRepository;

    @Resource
    private KubeService kubeService;

    @PostMapping("/save")
    @Transactional(rollbackFor = Exception.class)
    public ResultDTO<Void> saveCluster(@RequestBody ModifyClusterInfoRequest req) {

        ClusterInfoEntity clusterInfoEntity;

        // 检查框架id是否存在
        Integer stackId = req.getStackId();
        stackInfoRepository.findById(stackId).orElseThrow(() -> new IllegalArgumentException("can't find stack info by stack Id:" + stackId));;

        Integer id = req.getId();
        // 判断更新还是新建
        if (id == null) {
            clusterInfoEntity = new ClusterInfoEntity();
            clusterInfoEntity.setCreateTime(new Date());
        }else {
            clusterInfoEntity = clusterInfoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("can't find cluster info by id:" + id));
        }
        // 检验kubeconfig是否正确能连接k8s集群
        KubernetesClient kubernetesClient = kubeService.getKubernetesClient(req.getKubeConfig());
        kubeService.testConnect(kubernetesClient);
        BeanUtils.copyProperties(req, clusterInfoEntity);
        clusterInfoEntity.setCreateTime(new Date());
        clusterInfoEntity.setCreateBy(AdminUserName);

        clusterInfoRepository.saveAndFlush(clusterInfoEntity);
        // 加载默认规则到集群中
        stackAlertRuleRepository.findByStackId(stackId).forEach(stackAlertRuleEntity -> {
            ClusterAlertRuleEntity clusterAlertRuleEntity = new ClusterAlertRuleEntity();
            BeanUtil.copyProperties(stackAlertRuleEntity, clusterAlertRuleEntity);
            clusterAlertRuleEntity.setClusterId(clusterInfoEntity.getId());
            clusterAlertRuleEntity.setCreateTime(new Date());
            clusterAlertRuleEntity.setUpdateTime(new Date());
            clusterAlertRuleRepository.save(clusterAlertRuleEntity);
        });

        return ResultDTO.success(null);
    }


    @PostMapping("/delete")
    @Transactional
    public ResultDTO<String> deleteCluster(Integer id) {
        // 判断服务是否删除
        Integer serviceCnt = serviceInstanceRepository.countByClusterId(id);
        if (serviceCnt > 0) {
            return ResultDTO.failed("请先删除集群下的服务");
        }

        clusterInfoRepository.deleteById(id);
        clusterNodeRepository.deleteByClusterId(id);
        return ResultDTO.success(null);
    }

    @GetMapping("/list")
    public ResultDTO<List<ClusterInfoVO>> listClusterInfo() {
        List<ClusterInfoVO> clusterInfoVOS = clusterInfoRepository.findAll().stream().map(new Function<ClusterInfoEntity, ClusterInfoVO>() {
            @Override
            public ClusterInfoVO apply(ClusterInfoEntity clusterInfoEntity) {
                ClusterInfoVO clusterInfoVO = new ClusterInfoVO();
                Integer clusterId = clusterInfoEntity.getId();
                BeanUtils.copyProperties(clusterInfoEntity,clusterInfoVO);
                // 查询节点数
                Integer nodeCnt = clusterNodeRepository.countByClusterId(clusterId);
                // 查询服务数
                Integer serviceCnt = serviceInstanceRepository.countByClusterId(clusterId);
                clusterInfoVO.setNodeCnt(nodeCnt);
                clusterInfoVO.setServiceCnt(serviceCnt);
                return clusterInfoVO;
            }
        }).collect(Collectors.toList());
        return ResultDTO.success(clusterInfoVOS);
    }




}
