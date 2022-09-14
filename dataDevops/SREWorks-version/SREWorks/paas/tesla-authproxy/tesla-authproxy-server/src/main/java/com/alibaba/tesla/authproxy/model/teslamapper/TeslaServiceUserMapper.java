package com.alibaba.tesla.authproxy.model.teslamapper;

import com.alibaba.tesla.authproxy.model.TeslaServiceUserDO;
import com.alibaba.tesla.authproxy.model.example.TeslaServiceUserExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

/**
 * <p>Title: TeslaServiceUserMapper.java<／p>
 * <p>Description: 用户信息数据访问接口 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Mapper
public interface TeslaServiceUserMapper {

    long countByExample(TeslaServiceUserExample example);

    int deleteByExample(TeslaServiceUserExample example);

    int deleteByPrimaryKey(Integer userid);

    int insert(TeslaServiceUserDO record);

    int insertSelective(TeslaServiceUserDO record);

    List<TeslaServiceUserDO> selectByExampleWithRowbounds(TeslaServiceUserExample example, RowBounds rowBounds);

    List<TeslaServiceUserDO> selectByExample(TeslaServiceUserExample example);

    TeslaServiceUserDO selectByPrimaryKey(Integer userid);

    int updateByExampleSelective(@Param("record") TeslaServiceUserDO record, @Param("example") TeslaServiceUserExample example);

    int updateByExample(@Param("record") TeslaServiceUserDO record, @Param("example") TeslaServiceUserExample example);

    int updateByPrimaryKeySelective(TeslaServiceUserDO record);

    int updateByPrimaryKey(TeslaServiceUserDO record);
}