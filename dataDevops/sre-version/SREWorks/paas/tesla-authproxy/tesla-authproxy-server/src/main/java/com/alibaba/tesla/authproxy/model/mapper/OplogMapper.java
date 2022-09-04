package com.alibaba.tesla.authproxy.model.mapper;

import com.alibaba.tesla.authproxy.model.OplogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>Title: OplogMapper.java<／p>
 * <p>Description: 操作日志信息数据访问接口 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Mapper
public interface OplogMapper {

    /**
     * 根据主键删除
     *
     * @param id
     * @return
     */
    int deleteByPrimaryKey(Long id);

    /**
     * 添加
     *
     * @param record
     * @return
     */
    int insert(OplogDO record);

    /**
     * 根据主键获取
     *
     * @param id
     * @return
     */
    OplogDO selectByPrimaryKey(Long id);

    /**
     * 根据主键更新
     *
     * @param record
     * @return
     */
    int updateByPrimaryKey(OplogDO record);

    /**
     * 根据 User 和 Action 选择离当前时间最近的一条记录
     *
     * @param user   用户的 loginName
     * @param action 执行的 action 内容
     */
    OplogDO getLastByUserAndAction(@Param("user") String user, @Param("action") String action);
}