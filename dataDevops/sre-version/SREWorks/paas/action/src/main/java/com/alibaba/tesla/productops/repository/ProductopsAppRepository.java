package com.alibaba.tesla.productops.repository;

import com.alibaba.tesla.productops.DO.ProductopsApp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;

import javax.transaction.Transactional;

/**
 * @author jinghua.yjh
 */
public interface ProductopsAppRepository
    extends JpaRepository<ProductopsApp, Long>, JpaSpecificationExecutor<ProductopsApp> {

    boolean existsByAppIdAndStageId(String appId, String stageId);

    ProductopsApp findFirstByAppId(String appId);

    ProductopsApp findFirstByAppIdAndStageId(String appId, String stageId);

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    Long deleteByAppId(String appId);

}
