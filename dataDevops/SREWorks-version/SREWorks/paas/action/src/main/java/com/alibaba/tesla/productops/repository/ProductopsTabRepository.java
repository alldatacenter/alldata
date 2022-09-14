package com.alibaba.tesla.productops.repository;

import java.util.List;

import javax.transaction.Transactional;

import com.alibaba.tesla.productops.DO.ProductopsTab;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;

/**
 * @author jinghua.yjh
 */
public interface ProductopsTabRepository
    extends JpaRepository<ProductopsTab, Long>, JpaSpecificationExecutor<ProductopsTab> {

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    void deleteByTabIdAndStageId(String tabId, String stageId);

    ProductopsTab findFirstByTabIdAndStageId(String tabId, String stageId);

    ProductopsTab findFirstByNodeTypePathAndStageId(String tabId, String stageId);

    List<ProductopsTab> findAllByNodeTypePathAndStageId(String nodeTypePath, String stageId);

    List<ProductopsTab> findAllByNodeTypePathLikeAndStageId(String s, String stageId);

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    void deleteByNodeTypePathLikeAndStageIdAndIsImport(String s, String stageId, Integer isImport);

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    Long deleteByAppId(String appId);

}
