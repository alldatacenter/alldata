package com.alibaba.tesla.appmanager.server.service.deploy.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.enums.DeployComponentAttrTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.server.repository.DeployComponentAttrRepository;
import com.alibaba.tesla.appmanager.server.repository.DeployComponentRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.DeployComponentAttrQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.DeployComponentQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentAttrDO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentDO;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployComponentService;
import com.alibaba.tesla.appmanager.server.service.deploy.business.DeployComponentBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DeployComponentServiceImpl implements DeployComponentService {

    @Autowired
    private DeployComponentRepository deployComponentRepository;

    @Autowired
    private DeployComponentAttrRepository deployComponentAttrRepository;

    /**
     * 根据部署工单 ID 获取对应的部署工单对象
     *
     * @param deployComponentId 部署 Component 工单 ID
     * @param withExt           是否包含扩展信息
     * @return 包含元信息及扩展信息
     */
    @Override
    public DeployComponentBO get(Long deployComponentId, boolean withExt) {
        DeployComponentDO order = deployComponentRepository.selectByPrimaryKey(deployComponentId);
        if (order == null) {
            return null;
        }
        if (!withExt) {
            return DeployComponentBO.builder().subOrder(order).build();
        }

        // 获取扩展信息
        DeployComponentAttrQueryCondition condition = DeployComponentAttrQueryCondition.builder()
                .deployComponentId(deployComponentId).build();
        List<DeployComponentAttrDO> attrList = deployComponentAttrRepository.selectByCondition(condition);
        return DeployComponentBO.builder()
                .subOrder(order)
                .attrMap(attrList.stream().
                        collect(Collectors.toMap(DeployComponentAttrDO::getAttrType, DeployComponentAttrDO::getAttrValue)))
                .build();
    }

    /**
     * 根据条件过滤部署工单列表
     *
     * @param condition 过滤条件
     * @param withExt   是否包含扩展信息
     * @return List
     */
    @Override
    public List<DeployComponentBO> list(DeployComponentQueryCondition condition, boolean withExt) {
        List<DeployComponentDO> orderList = deployComponentRepository.selectByCondition(condition);
        if (CollectionUtils.isEmpty(orderList)) {
            return new ArrayList<>();
        }
        if (!withExt) {
            return orderList.stream()
                    .map(order -> DeployComponentBO.builder().subOrder(order).build())
                    .collect(Collectors.toList());
        }

        // 获取扩展信息
        List<DeployComponentAttrDO> attrList = deployComponentAttrRepository.selectByCondition(
                DeployComponentAttrQueryCondition.builder()
                        .deployComponentIdList(orderList.stream().map(DeployComponentDO::getId).collect(Collectors.toList()))
                        .build());
        Map<Long, Map<String, String>> map = new HashMap<>();
        for (DeployComponentAttrDO attr : attrList) {
            map.putIfAbsent(attr.getDeployComponentId(), new HashMap<>());
            map.get(attr.getDeployComponentId()).put(attr.getAttrType(), attr.getAttrValue());
        }
        return orderList.stream()
                .map(order -> DeployComponentBO.builder().subOrder(order).attrMap(map.get(order.getId())).build())
                .collect(Collectors.toList());
    }

    /**
     * 创建指定部署工单
     *
     * @param subOrder 工单记录
     * @param attrMap  属性字典
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void create(DeployComponentDO subOrder, Map<DeployComponentAttrTypeEnum, String> attrMap) {
        log.info("prepare to insert deploy component|subOrder={}", JSONObject.toJSONString(subOrder));
        deployComponentRepository.insert(subOrder);
        Long deployAppId = subOrder.getDeployId();
        Long deployComponentId = subOrder.getId();
//        appInstanceService.updateVisit(subOrder);
        for (Map.Entry<DeployComponentAttrTypeEnum, String> entry : attrMap.entrySet()) {
            deployComponentAttrRepository.insert(DeployComponentAttrDO.builder()
                    .deployComponentId(deployComponentId)
                    .attrType(entry.getKey().toString())
                    .attrValue(entry.getValue())
                    .build());
        }
        log.info("deploy component has created|deployAppId={}|deployComponentId={}|subOrder={}",
                deployAppId, deployComponentId, JSONObject.toJSONString(subOrder));
    }

    /**
     * 更新指定部署工单的指定属性内容
     *
     * @param deployComponentId 部署 Component 工单 ID
     * @param attrType          属性类型
     * @param attrValue         属性内容
     */
    @Override
    public void updateAttr(Long deployComponentId, DeployComponentAttrTypeEnum attrType, String attrValue) {
        DeployComponentAttrQueryCondition condition = DeployComponentAttrQueryCondition.builder()
                .deployComponentId(deployComponentId)
                .attrType(attrType.toString())
                .build();
        List<DeployComponentAttrDO> attrList = deployComponentAttrRepository.selectByCondition(condition);
        assert attrList.size() <= 1;

        if (attrList.size() == 0) {
            deployComponentAttrRepository.insert(DeployComponentAttrDO.builder()
                    .deployComponentId(deployComponentId)
                    .attrType(attrType.toString())
                    .attrValue(attrValue)
                    .build());
        } else {
            DeployComponentAttrDO attr = attrList.get(0);
            attr.setAttrValue(attrValue);
            deployComponentAttrRepository.updateByPrimaryKey(attr);
        }
        log.info("deploy component attr has updated|deployComponentId={}|attrType={}", deployComponentId, attrType);
    }

    /**
     * 更新指定部署工单的元信息
     *
     * @param subOrder 工单记录
     */
    @Override
    public void update(DeployComponentDO subOrder) {
        log.info("prepare to update deploy component|subOrder={}", JSONObject.toJSONString(subOrder));
        int count = deployComponentRepository.updateByPrimaryKey(subOrder);
        if (count == 0) {
            log.warn("update deploy component failed because of locker expired|subOrder={}",
                    JSONObject.toJSONString(subOrder));
            throw new AppException(AppErrorCode.LOCKER_VERSION_EXPIRED);
        }
    }
}
