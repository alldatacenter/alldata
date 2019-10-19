package com.platform.mall.service.impl.admin;

import com.github.pagehelper.PageHelper;
import com.platform.mall.mapper.admin.SmsCouponHistoryMapper;
import com.platform.mall.entity.admin.SmsCouponHistory;
import com.platform.mall.entity.admin.SmsCouponHistoryExample;
import com.platform.mall.service.admin.SmsCouponHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 优惠券领取记录管理Service实现类
 * Created by wulinhao on 2019/9/6.
 */
@Service
public class SmsCouponHistoryServiceImpl implements SmsCouponHistoryService {
    @Autowired
    private SmsCouponHistoryMapper historyMapper;

    @Override
    public List<SmsCouponHistory> list(Long couponId, Integer useStatus, String orderSn, Integer pageSize, Integer pageNum) {
        PageHelper.startPage(pageNum, pageSize);
        SmsCouponHistoryExample example = new SmsCouponHistoryExample();
        SmsCouponHistoryExample.Criteria criteria = example.createCriteria();
        if (couponId != null) {
            criteria.andCouponIdEqualTo(couponId);
        }
        if (useStatus != null) {
            criteria.andUseStatusEqualTo(useStatus);
        }
        if (!StringUtils.isEmpty(orderSn)) {
            criteria.andOrderSnEqualTo(orderSn);
        }
        return historyMapper.selectByExample(example);
    }
}
