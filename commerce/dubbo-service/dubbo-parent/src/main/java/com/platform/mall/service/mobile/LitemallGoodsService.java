package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallGoods;

import java.util.List;

public interface LitemallGoodsService {
    /**
     * 获取热卖商品
     *
     * @param offset
     * @param limit
     * @return
     */
    List<LitemallGoods> queryByHot(int offset, int limit);

    /**
     * 获取新品上市
     *
     * @param offset
     * @param limit
     * @return
     */
    List<LitemallGoods> queryByNew(int offset, int limit);

    /**
     * 获取分类下的商品
     *
     * @param catList
     * @param offset
     * @param limit
     * @return
     */
    List<LitemallGoods> queryByCategory(List<Integer> catList, int offset, int limit);

    /**
     * 获取分类下的商品
     *
     * @param catId
     * @param offset
     * @param limit
     * @return
     */
    List<LitemallGoods> queryByCategory(Integer catId, int offset, int limit);

    List<LitemallGoods> querySelective(Integer catId, Integer brandId, String keywords, Boolean isHot,
                                       Boolean isNew, Integer offset, Integer limit, String sort, String order);

    List<LitemallGoods> querySelective(String goodsSn, String name, Integer page,
                                       Integer size, String sort, String order);

    /**
     * 获取某个商品信息,包含完整信息
     *
     * @param id
     * @return
     */
    LitemallGoods findById(Integer id);

    /**
     * 获取某个商品信息，仅展示相关内容
     *
     * @param id
     * @return
     */
    LitemallGoods findByIdVO(Integer id);

    /**
     * 获取所有在售物品总数
     *
     * @return
     */
    Integer queryOnSale();

    int updateById(LitemallGoods goods);

    void deleteById(Integer id);

    void add(LitemallGoods goods);

    /**
     * 获取所有物品总数，包括在售的和下架的，但是不包括已删除的商品
     *
     * @return
     */
    int count();

    List<Integer> getCatIds(Integer brandId, String keywords, Boolean isHot, Boolean isNew);

    boolean checkExistByName(String name);

    List<LitemallGoods> queryByIds(Integer[] ids);
}
