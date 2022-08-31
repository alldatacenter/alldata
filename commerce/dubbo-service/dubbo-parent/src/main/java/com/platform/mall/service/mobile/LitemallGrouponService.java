package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallGroupon;

import java.util.List;

public interface LitemallGrouponService {

    /**
     * 获取用户发起的团购记录
     *
     * @param userId
     * @return
     */
    List<LitemallGroupon> queryMyGroupon(Integer userId);

    /**
     * 获取用户参与的团购记录
     *
     * @param userId
     * @return
     */
    List<LitemallGroupon> queryMyJoinGroupon(Integer userId);

    /**
     * 根据OrderId查询团购记录
     *
     * @param orderId
     * @return
     */
    LitemallGroupon queryByOrderId(Integer orderId);

    /**
     * 获取某个团购活动参与的记录
     *
     * @param id
     * @return
     */
    List<LitemallGroupon> queryJoinRecord(Integer id);

    /**
     * 根据ID查询记录
     *
     * @param id
     * @return
     */
    LitemallGroupon queryById(Integer id);

    /**
     * 返回某个发起的团购参与人数
     *
     * @param grouponId
     * @return
     */
    int countGroupon(Integer grouponId);

    int updateById(LitemallGroupon groupon);

    /**
     * 创建或参与一个团购
     *
     * @param groupon
     * @return
     */
    int createGroupon(LitemallGroupon groupon);


    /**
     * 查询所有发起的团购记录
     *
     * @param rulesId
     * @param page
     * @param size
     * @param sort
     * @param order
     * @return
     */
    List<LitemallGroupon> querySelective(String rulesId, Integer page, Integer size, String sort, String order);
}
