package com.alibaba.tesla.productops.repository;

import java.util.List;

import javax.transaction.Transactional;

import com.alibaba.tesla.productops.DO.ProductopsElement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;

/**
 * @author jinghua.yjh
 */
public interface ProductopsElementRepository
    extends JpaRepository<ProductopsElement, Long>, JpaSpecificationExecutor<ProductopsElement> {

    ProductopsElement findFirstByElementIdAndStageId(String elementId, String stageId);

    List<ProductopsElement> findAllByElementIdInAndStageId(List<String> elementIdList, String stageId);

    List<ProductopsElement> findAllByAppIdAndStageId(String appId, String stageId);


    @Modifying
    @Transactional(rollbackOn = Exception.class)
    void deleteByAppIdAndStageIdAndIsImport(String appId, String stageId, Integer isImport);

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    Long deleteByAppId(String appId);

}
