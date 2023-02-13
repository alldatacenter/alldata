package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.CustomAddonInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonInstanceDO;

import java.util.List;

/**
 * @author yangjie.dyj@alibaba-inc.com
 * @InterfaceName: CustomAddonInstanceRepository
 * @DATE: 2020-11-25
 * @Description:
 **/
public interface CustomAddonInstanceRepository {
    /**
     * 根据查询条件计数
     *
     * @param condition
     * @return
     */
    long countByCondition(CustomAddonInstanceQueryCondition condition);

    /**
     * 根据条件删除
     *
     * @param condition
     * @return
     */
    int deleteByCondition(CustomAddonInstanceQueryCondition condition);

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
    int insert(CustomAddonInstanceDO record);

    /**
     * 插入或更新
     *
     * @param record
     * @return
     */
    int insertOrUpdate(CustomAddonInstanceDO record);

    /**
     * 根据条件查询
     *
     * @param condition
     * @return
     */
    CustomAddonInstanceDO getByCondition(CustomAddonInstanceQueryCondition condition);

    /**
     * 根据条件查询所有
     *
     * @param condition
     * @return
     */
    List<CustomAddonInstanceDO> selectByCondition(CustomAddonInstanceQueryCondition condition);

    /**
     * 根据主键查询
     *
     * @param id
     * @return
     */
    CustomAddonInstanceDO selectByPrimaryKey(Long id);

    /**
     * 根据条件查询指定实体更新
     *
     * @param record
     * @param condition
     * @return
     */
    int updateByCondition(CustomAddonInstanceDO record, CustomAddonInstanceQueryCondition condition);

    /**
     * 根据主键更新
     *
     * @param record
     * @return
     */
    int updateByPrimaryKey(CustomAddonInstanceDO record);
}
