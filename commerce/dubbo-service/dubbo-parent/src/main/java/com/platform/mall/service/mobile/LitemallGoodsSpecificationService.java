package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallGoodsSpecification;

import java.util.List;

public interface LitemallGoodsSpecificationService {

    List<LitemallGoodsSpecification> queryByGid(Integer id);

    LitemallGoodsSpecification findById(Integer id);

    void deleteByGid(Integer gid);

    void add(LitemallGoodsSpecification goodsSpecification);

    /**
     * [
     * {
     * name: '',
     * valueList: [ {}, {}]
     * },
     * {
     * name: '',
     * valueList: [ {}, {}]
     * }
     * ]
     *
     * @param id
     * @return
     */
    Object getSpecificationVoList(Integer id);

}
