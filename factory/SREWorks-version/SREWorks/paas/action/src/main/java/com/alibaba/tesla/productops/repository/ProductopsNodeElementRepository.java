package com.alibaba.tesla.productops.repository;

import java.util.List;

import javax.transaction.Transactional;

import com.alibaba.tesla.productops.DO.ProductopsElement;
import com.alibaba.tesla.productops.DO.ProductopsNode;
import com.alibaba.tesla.productops.DO.ProductopsNodeElement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;

/**
 * @author jinghua.yjh
 */
public interface ProductopsNodeElementRepository
    extends JpaRepository<ProductopsNodeElement, Long>, JpaSpecificationExecutor<ProductopsNodeElement> {

    List<ProductopsNodeElement> findAllByNodeTypePathAndStageId(String nodeTypePath, String stageId);

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    void deleteByAppIdAndStageIdAndIsImport(String appId, String stageId, Integer isImport);

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    void deleteByNodeTypePathAndElementIdAndStageId(String nodeTypePath, String elementId, String stageId);

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    void deleteByNodeTypePathAndStageId(String nodeTypePath, String stageId);

    ProductopsNodeElement findFirstByNodeTypePathAndElementIdAndStageId(
        String nodeTypePath, String elementId, String stageId);

    List<ProductopsNodeElement> findAllByNodeTypePathLikeAndStageId(String s, String stageId);

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    Long deleteByAppId(String appId);


}
