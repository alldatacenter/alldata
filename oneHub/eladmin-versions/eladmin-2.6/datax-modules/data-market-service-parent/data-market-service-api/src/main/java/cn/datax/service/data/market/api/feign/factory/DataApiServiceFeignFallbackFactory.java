package cn.datax.service.data.market.api.feign.factory;

import cn.datax.service.data.market.api.feign.DataApiServiceFeign;
import cn.datax.service.data.market.api.feign.fallback.DataApiServiceFeignFallbackImpl;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class DataApiServiceFeignFallbackFactory implements FallbackFactory<DataApiServiceFeign> {

    @Override
    public DataApiServiceFeign create(Throwable throwable) {
        DataApiServiceFeignFallbackImpl dataApiServiceFeignFallback = new DataApiServiceFeignFallbackImpl();
        dataApiServiceFeignFallback.setCause(throwable);
        return dataApiServiceFeignFallback;
    }
}
