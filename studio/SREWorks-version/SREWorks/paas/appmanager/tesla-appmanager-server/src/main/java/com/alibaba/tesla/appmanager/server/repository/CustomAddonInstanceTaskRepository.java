package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.AddonInstanceTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonInstanceTaskDO;

import java.util.List;

/**
 * @author yangjie.dyj@alibaba-inc.com
 * @InterfaceName: CustomAddonInstanceTaskRepository
 * @DATE: 2020-11-25
 * @Description:
 **/
public interface CustomAddonInstanceTaskRepository {
    /**
     * 根据条件计数
     *
     * @param condition
     * @return
     */
    long countByCondition(AddonInstanceTaskQueryCondition condition);

    /**
     * 根据条件删除
     *
     * @param condition
     * @return
     */
    int deleteByCondition(AddonInstanceTaskQueryCondition condition);

    /**
     * 根据主键删除
     *
     * @param id
     * @return
     */
    int deleteByPrimaryKey(Long id);

    /**
     * 插入
     *
     * @param record
     * @return
     */
    int insert(CustomAddonInstanceTaskDO record);

    /**
     * 插入或更新
     *
     * @param record
     * @return
     */
    int insertOrUpdate(CustomAddonInstanceTaskDO record);

    /**
     * 根据条件查询所有
     *
     * @param condition
     * @return
     */
    List<CustomAddonInstanceTaskDO> selectByCondition(AddonInstanceTaskQueryCondition condition);

    /**
     * 根据主键查询
     *
     * @param id
     * @return
     */
    CustomAddonInstanceTaskDO selectByPrimaryKey(Long id);

    /**
     * 更新
     *
     * @param record
     * @param condition
     * @return
     */
    int updateByCondition(CustomAddonInstanceTaskDO record, AddonInstanceTaskQueryCondition condition);

    /**
     * 根据主键更新
     *
     * @param record
     * @return
     */
    int updateByPrimaryKey(CustomAddonInstanceTaskDO record);
}
