package cn.datax.service.data.market.api.feign;

import cn.datax.service.data.market.api.entity.ApiMaskEntity;
import cn.datax.service.data.market.api.feign.factory.ApiMaskServiceFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(contextId = "apiMaskServiceFeign", value = "service-data-market", fallbackFactory = ApiMaskServiceFeignFallbackFactory.class)
public interface ApiMaskServiceFeign {

    @GetMapping("/inner/apiMasks/api/{id}")
    ApiMaskEntity getApiMaskByApiId(@PathVariable("id") String id);
}
