package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallGrouponRules;

import java.util.List;

public interface LitemallGrouponRulesService {

    int createRules(LitemallGrouponRules rules);

    /**
     * 根据ID查找对应团购项
     *
     * @param id
     * @return
     */
    LitemallGrouponRules queryById(Integer id);

    /**
     * 查询某个商品关联的团购规则
     *
     * @param goodsId
     * @return
     */
    List<LitemallGrouponRules> queryByGoodsId(Integer goodsId);

    /**
     * 获取首页团购活动列表
     *
     * @param page
     * @param limit
     * @return
     */
    List<LitemallGrouponRules> queryList(Integer page, Integer limit);

    List<LitemallGrouponRules> queryList(Integer page, Integer limit, String sort, String order);

    /**
     * 判断某个团购活动是否已经过期
     *
     * @return
     */
    boolean isExpired(LitemallGrouponRules rules);

    /**
     * 获取团购活动列表
     *
     * @param goodsId
     * @param page
     * @param size
     * @param sort
     * @param order
     * @return
     */
    List<LitemallGrouponRules> querySelective(String goodsId, Integer page, Integer size,
                                              String sort, String order);

    void delete(Integer id);

    int updateById(LitemallGrouponRules grouponRules);
}
