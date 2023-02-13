package cn.datax.service.data.visual.api.feign.factory;


import cn.datax.service.data.visual.api.feign.VisualServiceFeign;
import cn.datax.service.data.visual.api.feign.fallback.VisualServiceFeignFallbackImpl;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class VisualServiceFeignFallbackFactory implements FallbackFactory<VisualServiceFeign> {

    @Override
    public VisualServiceFeign create(Throwable throwable) {
		VisualServiceFeignFallbackImpl feignFallback = new VisualServiceFeignFallbackImpl();
		feignFallback.setCause(throwable);
        return feignFallback;
    }
}
