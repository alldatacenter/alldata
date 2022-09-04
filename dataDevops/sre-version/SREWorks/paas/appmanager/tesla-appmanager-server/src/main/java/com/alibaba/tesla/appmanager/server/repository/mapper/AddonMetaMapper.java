package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.AddonMetaDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonMetaDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AddonMetaMapper {
    long countByExample(AddonMetaDOExample example);

    int deleteByExample(AddonMetaDOExample example);

    /**
     * delete by primary key
     *
     * @param id primaryKey
     * @return deleteCount
     */
    int deleteByPrimaryKey(Long id);

    /**
     * insert record to table
     *
     * @param record the record
     * @return insert count
     */
    int insert(AddonMetaDO record);

    /**
     * insert record to table selective
     *
     * @param record the record
     * @return insert count
     */
    int insertSelective(AddonMetaDO record);

    List<AddonMetaDO> selectByExample(AddonMetaDOExample example);

    /**
     * select by primary key
     *
     * @param id primary key
     * @return object by primary key
     */
    AddonMetaDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") AddonMetaDO record, @Param("example") AddonMetaDOExample example);

    int updateByExample(@Param("record") AddonMetaDO record, @Param("example") AddonMetaDOExample example);

    /**
     * update record selective
     *
     * @param record the updated record
     * @return update count
     */
    int updateByPrimaryKeySelective(AddonMetaDO record);

    /**
     * update record
     *
     * @param record the updated record
     * @return update count
     */
    int updateByPrimaryKey(AddonMetaDO record);
}