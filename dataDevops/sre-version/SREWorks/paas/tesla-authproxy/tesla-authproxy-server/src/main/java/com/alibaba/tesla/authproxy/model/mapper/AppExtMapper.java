package com.alibaba.tesla.authproxy.model.mapper;

import com.alibaba.tesla.authproxy.model.AppExtDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>Title: AppExtMapper.java<／p>
 * <p>Description: 外部应用信息数据访问接口 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Mapper
public interface AppExtMapper {

    /**
     * 根据appName
     *
     * @param extAppName
     * @return
     */
    AppExtDO getByName(String extAppName);

    /**
     * 添加应用信息
     *
     * @param record
     * @return
     */
    int insert(AppExtDO record);

    /**
     * 根据主键更新应用信息
     *
     * @param record
     * @return
     */
    int updateByPrimaryKey(AppExtDO record);

}