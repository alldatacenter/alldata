package com.platform.mall.service.admin;

import com.platform.mall.entity.admin.CmsSubject;

import java.util.List;

/**
 * 商品专题Service
 * Created by wulinhao on 2019/9/1.
 */
public interface CmsSubjectService {
    /**
     * 查询所有专题
     */
    List<CmsSubject> listAll();

    /**
     * 分页查询专题
     */
    List<CmsSubject> list(String keyword, Integer pageNum, Integer pageSize);
}
