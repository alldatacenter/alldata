package com.platform.mall.service.admin;

import com.platform.mall.dto.admin.PmsBrandParam;
import com.platform.mall.entity.admin.PmsBrand;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品品牌Service
 * Created by wulinhao on 2019/9/26.
 */
public interface PmsBrandService {
    List<PmsBrand> listAllBrand();

    int createBrand(PmsBrandParam pmsBrandParam);

    @Transactional
    int updateBrand(Long id, PmsBrandParam pmsBrandParam);

    int deleteBrand(Long id);

    int deleteBrand(List<Long> ids);

    List<PmsBrand> listBrand(String keyword, int pageNum, int pageSize);

    PmsBrand getBrand(Long id);

    int updateShowStatus(List<Long> ids, Integer showStatus);

    int updateFactoryStatus(List<Long> ids, Integer factoryStatus);
}
