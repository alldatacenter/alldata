package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallCart;
import java.util.List;

public interface  LitemallCartService {
    LitemallCart queryExist(Integer goodsId, Integer productId, Integer userId);

    void add(LitemallCart cart);

    int updateById(LitemallCart cart);

    List<LitemallCart> queryByUid(int userId);


    List<LitemallCart> queryByUidAndChecked(Integer userId);

    int delete(List<Integer> productIdList, int userId);

    LitemallCart findById(Integer id);

    int updateCheck(Integer userId, List<Integer> idsList, Boolean checked);

    void clearGoods(Integer userId);

    List<LitemallCart> querySelective(Integer userId, Integer goodsId, Integer page, Integer limit, String sort, String order);

    void deleteById(Integer id);

    boolean checkExist(Integer goodsId);
}
