package com.platform.website.service.impl;

import com.platform.website.common.base.BaseService;
import com.platform.website.common.base.IBaseMapper;
import com.platform.website.dao.UserMapper;
import com.platform.website.module.User;
import com.platform.website.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("userService")
public class UserService extends BaseService<User> implements IUserService {

  @Autowired
  UserMapper userMapper;

  @Override
  public User getOneUser(Integer id) {
    return userMapper.selectByPrimaryKey(id);
  }

  @Override
  public IBaseMapper<User> getBaseMapper() {
    return userMapper;
  }
}
