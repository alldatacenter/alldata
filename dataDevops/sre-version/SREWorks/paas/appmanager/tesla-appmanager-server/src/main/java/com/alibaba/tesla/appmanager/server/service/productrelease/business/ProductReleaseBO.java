package com.alibaba.tesla.appmanager.server.service.productrelease.business;

import com.alibaba.tesla.appmanager.server.repository.domain.ProductDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseAppRelDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseRelDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ReleaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 产品发布版本合集数据 BO
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReleaseBO {

    private ProductDO product;

    private ReleaseDO release;

    private ProductReleaseRelDO productReleaseRel;

    private List<ProductReleaseAppRelDO> appRelList;
}
