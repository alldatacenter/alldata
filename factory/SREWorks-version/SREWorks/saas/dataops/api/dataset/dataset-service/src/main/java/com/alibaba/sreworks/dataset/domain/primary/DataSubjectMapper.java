package com.alibaba.sreworks.dataset.domain.primary;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface DataSubjectMapper {
    long countByExample(DataSubjectExample example);

    int deleteByExample(DataSubjectExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(DataSubject record);

    int insertSelective(DataSubject record);

    List<DataSubject> selectByExampleWithBLOBsWithRowbounds(DataSubjectExample example, RowBounds rowBounds);

    List<DataSubject> selectByExampleWithBLOBs(DataSubjectExample example);

    List<DataSubject> selectByExampleWithRowbounds(DataSubjectExample example, RowBounds rowBounds);

    List<DataSubject> selectByExample(DataSubjectExample example);

    DataSubject selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") DataSubject record, @Param("example") DataSubjectExample example);

    int updateByExampleWithBLOBs(@Param("record") DataSubject record, @Param("example") DataSubjectExample example);

    int updateByExample(@Param("record") DataSubject record, @Param("example") DataSubjectExample example);

    int updateByPrimaryKeySelective(DataSubject record);

    int updateByPrimaryKeyWithBLOBs(DataSubject record);

    int updateByPrimaryKey(DataSubject record);
}