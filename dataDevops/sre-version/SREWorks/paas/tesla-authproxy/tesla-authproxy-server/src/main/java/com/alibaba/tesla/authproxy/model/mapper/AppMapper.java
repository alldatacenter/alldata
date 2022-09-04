package com.alibaba.tesla.authproxy.model.mapper;

import com.alibaba.tesla.authproxy.model.AppDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>Title: AppMapper.java<／p>
 * <p>Description: 应用信息数据访问接口 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Mapper
public interface AppMapper {


    /**
     * 根据主键删除应用信息
     *
     * @param id
     * @return
     */
    int deleteByPrimaryKey(Long id);

    /**
     * 添加应用信息
     *
     * @param record
     * @return
     */
    int insert(AppDO record);

    /**
     * 根据主键获取应用信息
     *
     * @param id
     * @return
     */
    AppDO selectByPrimaryKey(Long id);

    /**
     * 根据主键更新应用信息
     *
     * @param record
     * @return
     */
    int updateByPrimaryKey(AppDO record);

    /**
     * 根据appId查询app信息
     *
     * @param appId
     * @return
     */
    AppDO getByAppId(String appId);
}