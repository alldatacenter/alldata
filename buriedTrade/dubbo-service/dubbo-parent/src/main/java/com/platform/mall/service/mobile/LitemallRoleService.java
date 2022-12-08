package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallRole;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public interface LitemallRoleService {
    Set<String> queryByIds(Integer[] roleIds);

    List<LitemallRole> querySelective(String name, Integer page, Integer limit,
                                      String sort, String order);

    LitemallRole findById(Integer id);

    void add(LitemallRole role);

    void deleteById(Integer id);

    void updateById(LitemallRole role);

    boolean checkExist(String name);

    List<LitemallRole> queryAll();
}
