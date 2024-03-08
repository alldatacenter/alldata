package com.platform.admin.mapper;

import com.platform.admin.entity.JobGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JobGroupMapper {

    List<JobGroup> findAll();

    List<JobGroup> find(@Param("appName") String appName,
                        @Param("title") String title,
                        @Param("addressList") String addressList);

    int save(JobGroup jobGroup);
    List<JobGroup> findByAddressType(@Param("addressType") int addressType);

    int update(JobGroup jobGroup);

    int remove(@Param("id") int id);

    JobGroup load(@Param("id") int id);
}
