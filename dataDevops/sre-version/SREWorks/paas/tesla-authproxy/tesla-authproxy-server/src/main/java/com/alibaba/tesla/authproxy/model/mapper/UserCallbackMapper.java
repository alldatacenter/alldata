package com.alibaba.tesla.authproxy.model.mapper;

import com.alibaba.tesla.authproxy.model.UserCallbackDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户回调接口
 */
@Mapper
public interface UserCallbackMapper {

    /**
     * 根据 trigger type 来获取对应的所有回调地址
     */
    List<UserCallbackDO> selectByTriggerType(String triggerType);

}
