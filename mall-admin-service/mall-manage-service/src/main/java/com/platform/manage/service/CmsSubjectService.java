package com.platform.manage.service;

import com.platform.manage.model.CmsSubject;

import java.util.List;

/**
 * 商品专题Service
 * Created by wulinhao on 2019/6/1.
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
