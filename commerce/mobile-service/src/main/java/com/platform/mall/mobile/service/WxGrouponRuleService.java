package com.platform.mall.mobile.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import com.platform.mall.config.vo.GrouponRuleVo;
import com.platform.mall.entity.mobile.LitemallGoods;
import com.platform.mall.entity.mobile.LitemallGrouponRules;
import com.platform.mall.service.mobile.LitemallGoodsService;
import com.platform.mall.service.mobile.LitemallGrouponRulesService;
import com.platform.mall.service.mobile.LitemallGrouponService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WxGrouponRuleService {
    private final Log logger = LogFactory.getLog(WxGrouponRuleService.class);

    @Autowired
    private LitemallGrouponRulesService grouponRulesService;
    @Autowired
    private LitemallGrouponService grouponService;
    @Autowired
    private LitemallGoodsService goodsService;


    public List<GrouponRuleVo> queryList(Integer page, Integer size) {
        return queryList(page, size, "add_time", "desc");
    }


    public List<GrouponRuleVo> queryList(Integer page, Integer size, String sort, String order) {
        List<LitemallGrouponRules> grouponRulesList = grouponRulesService.queryList(page, size, sort, order);
        PageInfo pageInfo = new PageInfo(grouponRulesList);
        Page<GrouponRuleVo> grouponList = new Page<GrouponRuleVo>();
        grouponList.setPages(pageInfo.getPages());
        grouponList.setPageNum(pageInfo.getPageNum());
        grouponList.setPageSize(pageInfo.getPageSize());
        grouponList.setTotal(pageInfo.getTotal());

        for (LitemallGrouponRules rule : (List<LitemallGrouponRules>)pageInfo.getList()) {
            Integer goodsId = rule.getGoodsId();
            LitemallGoods goods = goodsService.findById(goodsId);
            if (goods == null)
                continue;

            GrouponRuleVo grouponRuleVo = new GrouponRuleVo();
            grouponRuleVo.setId(goods.getId());
            grouponRuleVo.setName(goods.getName());
            grouponRuleVo.setBrief(goods.getBrief());
            grouponRuleVo.setPicUrl(goods.getPicUrl());
            grouponRuleVo.setCounterPrice(goods.getCounterPrice());
            grouponRuleVo.setRetailPrice(goods.getRetailPrice());
            grouponRuleVo.setGrouponPrice(goods.getRetailPrice().subtract(rule.getDiscount()));
            grouponRuleVo.setGrouponDiscount(rule.getDiscount());
            grouponRuleVo.setGrouponMember(rule.getDiscountMember());
            grouponList.add(grouponRuleVo);
        }

        return grouponList;
    }
}
