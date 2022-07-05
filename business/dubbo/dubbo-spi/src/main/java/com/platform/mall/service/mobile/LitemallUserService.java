package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallUser;
import com.platform.mall.entity.mobile.UserVo;
import java.util.List;

public interface LitemallUserService {

    LitemallUser findById(Integer userId);

    UserVo findUserVoById(Integer userId);

    LitemallUser queryByOid(String openId);

    void add(LitemallUser user);

    int updateById(LitemallUser user);

    List<LitemallUser> querySelective(String username, String mobile, Integer page, Integer size,
                                      String sort, String order);

    int count();

    List<LitemallUser> queryByUsername(String username);

    boolean checkByUsername(String username);

    List<LitemallUser> queryByMobile(String mobile);

    List<LitemallUser> queryByOpenid(String openid);

    void deleteById(Integer id);
}
