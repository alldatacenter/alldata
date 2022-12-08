package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallBrand;
import java.util.List;

public interface LitemallBrandService {
   List<LitemallBrand> query(Integer page, Integer limit, String sort, String order);

   List<LitemallBrand> query(Integer page, Integer limit);
   LitemallBrand findById(Integer id);

   List<LitemallBrand> querySelective(String id, String name, Integer page, Integer size, String sort, String order);

   int updateById(LitemallBrand brand);

   void deleteById(Integer id);

   void add(LitemallBrand brand);

   List<LitemallBrand> all();
}
