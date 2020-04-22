package com.platform.website.service;

import com.platform.website.common.base.IBaseService;
import com.platform.website.module.User;

import org.apache.ibatis.annotations.Param;

public interface IUserService extends IBaseService<User> {
  public User getOneUser(@Param("id") Integer id);
}
