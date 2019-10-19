package com.platform.mall.service;

import com.platform.mall.entity.TbPanelContent;
import com.platform.mall.dto.front.AllGoodsResult;
import com.platform.mall.entity.DataTablesResult;
import com.platform.mall.dto.front.ProductDet;
import com.platform.mall.entity.TbPanel;

import java.util.List;


/**
 * @author wulinhao
 */
public interface ContentService {

    /**
     * 添加板块内容
     * @param tbPanelContent
     * @return
     */
    int addPanelContent(TbPanelContent tbPanelContent);

    /**
     * 通过panelId获取板块具体内容
     * @param panelId
     * @return
     */
    DataTablesResult getPanelContentListByPanelId(int panelId);

    /**
     * 删除板块内容
     * @param id
     * @return
     */
    int deletePanelContent(int id);

    /**
     * 编辑板块内容
     * @param tbPanelContent
     * @return
     */
    int updateContent(TbPanelContent tbPanelContent);

    /**
     * 通过id获取板块内容
     * @param id
     * @return
     */
    TbPanelContent getTbPanelContentById(int id);

    /**
     * 获取首页数据
     * @return
     */
    List<TbPanel> getHome();

    /**
     * 获取商品推荐板块
     * @return
     */
    List<TbPanel> getRecommendGoods();

    /**
     * 获取我要捐赠板块
     * @return
     */
    List<TbPanel> getThankGoods();

    /**
     * 获取商品详情
     * @param id
     * @return
     */
    ProductDet getProductDet(Long id);

    /**
     * 分页多条件获取全部商品
     * @param page
     * @param size
     * @param sort
     * @param priceGt
     * @param priceLte
     * @return
     */
    AllGoodsResult getAllProduct(int page, int size, String sort, Long cid, int priceGt, int priceLte);

    /**
     * 获取首页缓存
     * @return
     */
    String getIndexRedis();

    /**
     * 同步首页缓存
     * @return
     */
    int updateIndexRedis();

    /**
     * 获取推荐板块缓存
     * @return
     */
    String getRecommendRedis();

    /**
     * 同步推荐板块缓存
     * @return
     */
    int updateRecommendRedis();

    /**
     * 获取推荐板块缓存
     * @return
     */
    String getThankRedis();

    /**
     * 同步推荐板块缓存
     * @return
     */
    int updateThankRedis();

    /**
     * 获取导航栏
     * @return
     */
    List<TbPanelContent> getNavList();
}
