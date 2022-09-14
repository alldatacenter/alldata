package com.alibaba.tesla.productops.repository;

import com.alibaba.tesla.productops.DO.ProductopsComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;

import javax.transaction.Transactional;
import java.util.List;

public interface ProductopsComponentRepository
        extends JpaRepository<ProductopsComponent, Long>,JpaSpecificationExecutor<ProductopsComponent> {

    ProductopsComponent findFirstByComponentIdAndStageId(String componentId, String stageId);

    List<ProductopsComponent> findAllByStageId(String stageId);

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    int deleteByComponentIdAndStageId(String componentId, String stageId);

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    int deleteByStageIdAndIsImport(String stageId, Integer isImport);

}
