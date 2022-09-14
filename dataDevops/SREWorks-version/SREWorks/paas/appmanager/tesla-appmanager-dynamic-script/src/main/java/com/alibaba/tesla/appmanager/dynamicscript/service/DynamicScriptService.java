package com.alibaba.tesla.appmanager.dynamicscript.service;

import com.alibaba.tesla.appmanager.dynamicscript.repository.condition.DynamicScriptQueryCondition;
import com.alibaba.tesla.appmanager.dynamicscript.repository.domain.DynamicScriptDO;

import java.util.List;

/**
 * 动态脚本服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface DynamicScriptService {

    /**
     * 获取动态脚本内容
     *
     * @param condition 查询条件
     * @return DynamicScriptDO
     */
    DynamicScriptDO get(DynamicScriptQueryCondition condition);

    /**
     * 获取动态脚本列表
     *
     * @param condition 查询条件
     * @return List of DynamicScriptDO
     */
    List<DynamicScriptDO> list(DynamicScriptQueryCondition condition);

    /**
     * 初始化脚本
     * <p>
     * * 如果当前记录不存在，则新增
     * * 如果 version 大于当前版本，则覆盖数据库中的已有脚本数据
     * * 如果 version 小于等于当前版本，不进行操作
     *
     * @param condition 查询条件
     * @param revision  提供的脚本版本
     * @param code      Groovy 代码
     */
    void initScript(DynamicScriptQueryCondition condition, Integer revision, String code);
}
