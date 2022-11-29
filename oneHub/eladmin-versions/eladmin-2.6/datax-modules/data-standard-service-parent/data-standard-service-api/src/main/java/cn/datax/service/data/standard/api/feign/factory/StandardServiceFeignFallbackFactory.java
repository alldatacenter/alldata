package cn.datax.service.data.standard.api.feign.factory;

import cn.datax.service.data.standard.api.feign.StandardServiceFeign;
import cn.datax.service.data.standard.api.feign.fallback.StandardServiceFeignFallbackImpl;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class StandardServiceFeignFallbackFactory implements FallbackFactory<StandardServiceFeign> {

    @Override
    public StandardServiceFeign create(Throwable throwable) {
		StandardServiceFeignFallbackImpl feignFallback = new StandardServiceFeignFallbackImpl();
		feignFallback.setCause(throwable);
        return feignFallback;
    }
}
