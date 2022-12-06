package com.platform.dts.admin.mapper;

import com.platform.dts.admin.entity.JobUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author AllDataDC 2022/11/04 16:44:59
 */
@Mapper
public interface JobUserMapper {

    List<JobUser> pageList(@Param("offset") int offset,
						   @Param("pagesize") int pagesize,
						   @Param("username") String username);

    List<JobUser> findAll(@Param("username") String username);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("username") String username);

    JobUser loadByUserName(@Param("username") String username);

    JobUser getUserById(@Param("id") int id);

    List<JobUser> getUsersByIds(@Param("ids") String[] ids);

    int save(JobUser jobUser);

    int update(JobUser jobUser);

    int delete(@Param("id") int id);



}
