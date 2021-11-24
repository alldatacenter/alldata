package com.platform.realtime.view.service;

import com.platform.realtime.view.common.base.IBaseService;
import com.platform.realtime.view.module.User;
import org.apache.ibatis.annotations.Param;

public interface IUserService extends IBaseService<User> {
  public User getOneUser(@Param("id") Integer id);
}
