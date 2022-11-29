package cn.datax.service.data.quality.api.feign.factory;

import cn.datax.service.data.quality.api.feign.QualityServiceFeign;
import cn.datax.service.data.quality.api.feign.fallback.QualityServiceFeignFallbackImpl;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class QualityServiceFeignFallbackFactory implements FallbackFactory<QualityServiceFeign> {

    @Override
    public QualityServiceFeign create(Throwable throwable) {
		QualityServiceFeignFallbackImpl feignFallback = new QualityServiceFeignFallbackImpl();
		feignFallback.setCause(throwable);
        return feignFallback;
    }
}
