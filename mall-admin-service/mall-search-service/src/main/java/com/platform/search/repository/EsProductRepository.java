package com.platform.search.repository;

import com.platform.search.domain.EsProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 商品ES操作类
 * Created by wulinhao on 2019/9/19.
 */
public interface EsProductRepository extends ElasticsearchRepository<EsProduct, Long> {
    /**
     * 搜索查询
     *
     * @param name     商品名称
     * @param subTitle 商品标题
     * @param keywords 商品关键字
     * @param page     分页信息
     * @return
     */
    Page<EsProduct> findByNameOrSubTitleOrKeywords(String name, String subTitle, String keywords, Pageable page);

}
