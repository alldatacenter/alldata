package com.platform.mall.service.admin;

import com.platform.mall.entity.admin.SmsHomeRecommendProduct;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 首页人气推荐管理Service
 * Created by wulinhao on 2019/9/7.
 */
public interface SmsHomeRecommendProductService {
    /**
     * 添加首页推荐
     */
    @Transactional
    int create(List<SmsHomeRecommendProduct> homeRecommendProductList);

    /**
     * 修改推荐排序
     */
    int updateSort(Long id, Integer sort);

    /**
     * 批量删除推荐
     */
    int delete(List<Long> ids);

    /**
     * 更新推荐状态
     */
    int updateRecommendStatus(List<Long> ids, Integer recommendStatus);

    /**
     * 分页查询推荐
     */
    List<SmsHomeRecommendProduct> list(String productName, Integer recommendStatus, Integer pageSize, Integer pageNum);
}
