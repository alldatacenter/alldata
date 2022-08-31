package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallAddress;
import java.util.List;

public interface LitemallAddressService {

    List<LitemallAddress> queryByUid(Integer uid);

    LitemallAddress query(Integer userId, Integer id);

    int add(LitemallAddress address);
    int update(LitemallAddress address);

    void delete(Integer id);
    LitemallAddress findDefault(Integer userId);

    void resetDefault(Integer userId);
    List<LitemallAddress> querySelective(Integer userId, String name, Integer page, Integer limit, String sort, String order);
}
