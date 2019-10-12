package com.platform.manage.dao;

import com.platform.manage.model.UmsAdminPermissionRelation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户权限自定义Dao
 * Created by wulinhao on 2019/9/8.
 */
public interface UmsAdminPermissionRelationDao {
    int insertList(@Param("list") List<UmsAdminPermissionRelation> list);
}
