package cn.datax.service.data.market.api.feign.factory;

import cn.datax.service.data.market.api.feign.ApiMaskServiceFeign;
import cn.datax.service.data.market.api.feign.fallback.ApiMaskServiceFeignFallbackImpl;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ApiMaskServiceFeignFallbackFactory implements FallbackFactory<ApiMaskServiceFeign> {

    @Override
    public ApiMaskServiceFeign create(Throwable throwable) {
        ApiMaskServiceFeignFallbackImpl apiMaskServiceFeignFallbackImpl = new ApiMaskServiceFeignFallbackImpl();
        apiMaskServiceFeignFallbackImpl.setCause(throwable);
        return apiMaskServiceFeignFallbackImpl;
    }
}
