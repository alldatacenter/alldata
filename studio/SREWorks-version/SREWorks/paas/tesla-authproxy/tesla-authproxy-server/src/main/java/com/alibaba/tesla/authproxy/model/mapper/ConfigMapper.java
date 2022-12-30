package com.alibaba.tesla.authproxy.model.mapper;

import com.alibaba.tesla.authproxy.model.ConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Config 系统配置项数据访问接口
 */
@Mapper
public interface ConfigMapper {

    /**
     * 根据 name 来获取配置项
     */
    ConfigDO getByName(String name);

    /**
     * 根据 name 来更新配置项
     */
    int save(ConfigDO record);

}