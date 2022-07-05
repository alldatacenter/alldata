package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallAdmin;
import java.util.List;

public interface LitemallAdminService {
    List<LitemallAdmin> findAdmin(String username);

    LitemallAdmin findAdmin(Integer id);
    List<LitemallAdmin> querySelective(String username, Integer page, Integer limit, String sort, String order);

    int updateById(LitemallAdmin admin);

    void deleteById(Integer id);

    void add(LitemallAdmin admin);

    LitemallAdmin findById(Integer id);

    List<LitemallAdmin> all();
}
