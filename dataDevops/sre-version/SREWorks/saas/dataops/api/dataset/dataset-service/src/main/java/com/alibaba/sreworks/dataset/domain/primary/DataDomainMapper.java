package com.alibaba.sreworks.dataset.domain.primary;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface DataDomainMapper {
    long countByExample(DataDomainExample example);

    int deleteByExample(DataDomainExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(DataDomain record);

    int insertSelective(DataDomain record);

    List<DataDomain> selectByExampleWithBLOBsWithRowbounds(DataDomainExample example, RowBounds rowBounds);

    List<DataDomain> selectByExampleWithBLOBs(DataDomainExample example);

    List<DataDomain> selectByExampleWithRowbounds(DataDomainExample example, RowBounds rowBounds);

    List<DataDomain> selectByExample(DataDomainExample example);

    DataDomain selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") DataDomain record, @Param("example") DataDomainExample example);

    int updateByExampleWithBLOBs(@Param("record") DataDomain record, @Param("example") DataDomainExample example);

    int updateByExample(@Param("record") DataDomain record, @Param("example") DataDomainExample example);

    int updateByPrimaryKeySelective(DataDomain record);

    int updateByPrimaryKeyWithBLOBs(DataDomain record);

    int updateByPrimaryKey(DataDomain record);
}