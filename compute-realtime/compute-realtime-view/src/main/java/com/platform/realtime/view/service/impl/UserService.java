package com.platform.realtime.view.service.impl;

import com.platform.realtime.view.service.IUserService;
import com.platform.realtime.view.common.base.BaseService;
import com.platform.realtime.view.common.base.IBaseMapper;
import com.platform.realtime.view.dao.UserMapper;
import com.platform.realtime.view.module.User;
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

  public void  runAdClickRealTimeTask(){

    //userMapper.save()
  }

}
