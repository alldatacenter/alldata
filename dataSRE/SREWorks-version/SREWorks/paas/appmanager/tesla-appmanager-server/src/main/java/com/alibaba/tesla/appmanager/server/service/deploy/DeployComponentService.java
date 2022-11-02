package com.alibaba.tesla.appmanager.server.service.deploy;

import com.alibaba.tesla.appmanager.common.enums.DeployComponentAttrTypeEnum;
import com.alibaba.tesla.appmanager.server.repository.condition.DeployComponentQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentDO;
import com.alibaba.tesla.appmanager.server.service.deploy.business.DeployComponentBO;

import java.util.List;
import java.util.Map;

/**
 * 内部服务 - 部署 Component 工单
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface DeployComponentService {

    /**
     * 根据部署工单 ID 获取对应的部署工单对象
     *
     * @param deployComponentId 部署 Component 工单 ID
     * @param withExt           是否包含扩展信息
     * @return 包含元信息及扩展信息
     */
    DeployComponentBO get(Long deployComponentId, boolean withExt);

    /**
     * 根据条件过滤部署工单列表
     *
     * @param condition 过滤条件
     * @param withExt   是否包含扩展信息
     * @return List
     */
    List<DeployComponentBO> list(DeployComponentQueryCondition condition, boolean withExt);

    /**
     * 创建指定部署工单
     *
     * @param subOrder 工单记录
     * @param attrMap  属性字典
     */
    void create(DeployComponentDO subOrder, Map<DeployComponentAttrTypeEnum, String> attrMap);

    /**
     * 更新指定部署工单的指定属性内容
     *
     * @param deployComponentId 部署 Component 工单 ID
     * @param attrType          属性类型
     * @param attrValue         属性内容
     */
    void updateAttr(Long deployComponentId, DeployComponentAttrTypeEnum attrType, String attrValue);

    /**
     * 更新指定部署工单的元信息
     *
     * @param subOrder 工单记录
     */
    void update(DeployComponentDO subOrder);
}
