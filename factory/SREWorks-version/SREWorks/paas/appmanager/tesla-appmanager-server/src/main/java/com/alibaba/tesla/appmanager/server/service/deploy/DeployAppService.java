package com.alibaba.tesla.appmanager.server.service.deploy;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.server.repository.condition.DeployAppQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDO;
import com.alibaba.tesla.appmanager.server.service.deploy.business.DeployAppBO;

import java.util.Map;

/**
 * 内部服务 - 部署 App 工单
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface DeployAppService {

    /**
     * 根据部署工单 ID 获取对应的部署工单对象
     *
     * @param deployAppId 部署工单 ID
     * @param withExt     是否包含扩展信息
     * @return 包含元信息及扩展信息
     */
    DeployAppBO get(Long deployAppId, boolean withExt);

    /**
     * 根据条件过滤部署工单列表
     *
     * @param condition 过滤条件
     * @param withExt   是否包含扩展信息
     * @return List
     */
    Pagination<DeployAppBO> list(DeployAppQueryCondition condition, boolean withExt);

    /**
     * 创建指定部署工单
     *
     * @param order   工单记录
     * @param attrMap 属性字典
     */
    void create(DeployAppDO order, Map<String, String> attrMap);

    /**
     * 获取指定部署工单的指定属性内容
     *
     * @param deployAppId 部署工单 ID
     * @param attrType    属性类型
     * @return 如果不存在，返回 null
     */
    String getAttr(Long deployAppId, String attrType);

    /**
     * 更新指定部署工单的指定属性内容
     *
     * @param deployAppId 部署工单 ID
     * @param attrType    属性类型
     * @param attrValue   属性内容
     */
    void updateAttr(Long deployAppId, String attrType, String attrValue);

    /**
     * 更新指定部署工单的元信息
     *
     * @param order 工单记录
     */
    void update(DeployAppDO order);
}
