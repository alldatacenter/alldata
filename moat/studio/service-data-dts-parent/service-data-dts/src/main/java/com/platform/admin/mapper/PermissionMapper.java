package com.platform.admin.mapper;

import com.platform.admin.entity.JobPermission;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
public interface PermissionMapper {

    List<JobPermission> findAll();

    List<JobPermission> findByAdminUserId(int userId);
}