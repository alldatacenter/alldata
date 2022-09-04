package com.alibaba.tesla.appmanager.server.service.deploy.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.server.repository.DeployAppAttrRepository;
import com.alibaba.tesla.appmanager.server.repository.DeployAppRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.DeployAppAttrQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.DeployAppQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppAttrDO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDO;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployAppService;
import com.alibaba.tesla.appmanager.server.service.deploy.business.DeployAppBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 内部服务 - 部署 App 工单
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class DeployAppServiceImpl implements DeployAppService {

    @Autowired
    private DeployAppRepository deployAppRepository;

    @Autowired
    private DeployAppAttrRepository deployAppAttrRepository;

    /**
     * 根据部署工单 ID 获取对应的部署工单对象
     *
     * @param deployAppId 部署工单 ID
     * @param withExt     是否包含扩展信息
     * @return 包含元信息及扩展信息
     */
    @Override
    public DeployAppBO get(Long deployAppId, boolean withExt) {
        DeployAppDO order = deployAppRepository.selectByPrimaryKey(deployAppId);
        if (order == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find deploy app order by id %d", deployAppId));
        }
        if (!withExt) {
            return DeployAppBO.builder().order(order).build();
        }

        // 获取扩展信息
        DeployAppAttrQueryCondition condition = DeployAppAttrQueryCondition.builder().deployAppId(deployAppId).build();
        List<DeployAppAttrDO> attrList = deployAppAttrRepository.selectByCondition(condition);
        return DeployAppBO.builder()
                .order(order)
                .attrMap(attrList.stream().
                        collect(Collectors.toMap(DeployAppAttrDO::getAttrType, DeployAppAttrDO::getAttrValue)))
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
    public Pagination<DeployAppBO> list(DeployAppQueryCondition condition, boolean withExt) {
        List<DeployAppDO> orderList = deployAppRepository.selectByCondition(condition);
        if (CollectionUtils.isEmpty(orderList)) {
            return new Pagination<>();
        }
        if (!withExt) {
            return Pagination.valueOf(orderList, order -> DeployAppBO.builder().order(order).build());
        }

        // 获取扩展信息
        List<DeployAppAttrDO> attrList = deployAppAttrRepository.selectByCondition(
                DeployAppAttrQueryCondition.builder()
                        .deployAppIdList(orderList.stream().map(DeployAppDO::getId).collect(Collectors.toList()))
                        .build());
        Map<Long, Map<String, String>> map = new HashMap<>();
        for (DeployAppAttrDO attr : attrList) {
            map.putIfAbsent(attr.getDeployAppId(), new HashMap<>());
            map.get(attr.getDeployAppId()).put(attr.getAttrType(), attr.getAttrValue());
        }
        return Pagination.valueOf(orderList, order -> DeployAppBO.builder()
                .order(order)
                .attrMap(map.get(order.getId()))
                .build());
    }

    /**
     * 创建指定部署工单
     *
     * @param order   工单记录
     * @param attrMap 属性字典
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void create(DeployAppDO order, Map<String, String> attrMap) {
        deployAppRepository.insert(order);
        log.info("action=createDeployApp|deployAppId={}|namespaceId={}|stageId={}",
                order.getId(), order.getNamespaceId(), order.getStageId());
        Long deployAppId = order.getId();
        for (Map.Entry<String, String> entry : attrMap.entrySet()) {
            deployAppAttrRepository.insert(DeployAppAttrDO.builder()
                    .deployAppId(deployAppId)
                    .attrType(entry.getKey())
                    .attrValue(entry.getValue())
                    .build());
        }
        log.info("deploy app has created|deployAppId={}|order={}", deployAppId, JSONObject.toJSONString(order));
    }

    /**
     * 获取指定部署工单的指定属性内容
     *
     * @param deployAppId 部署工单 ID
     * @param attrType    属性类型
     * @return 如果不存在，返回 null
     */
    @Override
    public String getAttr(Long deployAppId, String attrType) {
        DeployAppAttrQueryCondition condition = DeployAppAttrQueryCondition.builder()
                .deployAppId(deployAppId)
                .attrType(attrType)
                .build();
        List<DeployAppAttrDO> attrList = deployAppAttrRepository.selectByCondition(condition);
        assert attrList.size() <= 1;
        if (attrList.size() == 0) {
            return null;
        } else {
            return attrList.get(0).getAttrValue();
        }
    }

    /**
     * 更新指定部署工单的指定属性内容
     *
     * @param deployAppId 部署工单 ID
     * @param attrType    属性类型
     * @param attrValue   属性内容
     */
    @Override
    public void updateAttr(Long deployAppId, String attrType, String attrValue) {
        DeployAppAttrQueryCondition condition = DeployAppAttrQueryCondition.builder()
                .deployAppId(deployAppId)
                .attrType(attrType)
                .build();
        List<DeployAppAttrDO> attrList = deployAppAttrRepository.selectByCondition(condition);
        assert attrList.size() <= 1;

        if (attrList.size() == 0) {
            deployAppAttrRepository.insert(DeployAppAttrDO.builder()
                    .deployAppId(deployAppId)
                    .attrType(attrType)
                    .attrValue(attrValue)
                    .build());
        } else {
            DeployAppAttrDO attr = attrList.get(0);
            attr.setAttrValue(attrValue);
            deployAppAttrRepository.updateByPrimaryKey(attr);
        }
        log.info("deploy app attr has updated|deployAppId={}|attrType={}", deployAppId, attrType);
    }

    /**
     * 更新指定部署工单的元信息
     *
     * @param order 工单记录
     */
    @Override
    public void update(DeployAppDO order) {
        log.info("action=updateDeployApp|deployAppId={}|namespaceId={}|stageId={}",
                order.getId(), order.getNamespaceId(), order.getStageId());
        int count = deployAppRepository.updateByPrimaryKey(order);
        if (count == 0) {
            throw new AppException(AppErrorCode.LOCKER_VERSION_EXPIRED);
        }
    }
}
