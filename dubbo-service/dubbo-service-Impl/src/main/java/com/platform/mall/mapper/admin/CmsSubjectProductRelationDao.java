package com.platform.mall.mapper.admin;


import com.platform.mall.entity.admin.CmsSubjectProductRelation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 自定义商品和专题关系操作
 * Created by wulinhao on 2019/9/26.
 */
public interface CmsSubjectProductRelationDao {
    int insertList(@Param("list") List<CmsSubjectProductRelation> subjectProductRelationList);
}
