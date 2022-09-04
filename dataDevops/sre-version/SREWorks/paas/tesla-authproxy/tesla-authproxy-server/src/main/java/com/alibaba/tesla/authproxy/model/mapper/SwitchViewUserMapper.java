package com.alibaba.tesla.authproxy.model.mapper;

import com.alibaba.tesla.authproxy.model.SwitchViewUserDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 切换视图用户列表 Mapper
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Mapper
public interface SwitchViewUserMapper {

    /**
     * 获取全量白名单用户
     */
    List<SwitchViewUserDO> select();

    /**
     * 根据工号获取指定用户
     *
     * @param empId 工号
     */
    SwitchViewUserDO getByEmpId(String empId);

    /**
     * 根据工号删除记录
     *
     * @param empId 工号
     */
    int delete(String empId);

    /**
     * 插入记录
     *
     * @param record 对应记录
     */
    int insert(SwitchViewUserDO record);
}
