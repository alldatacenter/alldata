package cn.datax.service.workflow.api.feign.factory;

import cn.datax.service.workflow.api.feign.FlowInstanceServiceFeign;
import cn.datax.service.workflow.api.feign.fallback.FlowInstanceServiceFeignFallbackImpl;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class FlowInstanceServiceFeignFallbackFactory implements FallbackFactory<FlowInstanceServiceFeign> {

    @Override
    public FlowInstanceServiceFeign create(Throwable throwable) {
        FlowInstanceServiceFeignFallbackImpl flowInstanceServiceFeignFallback = new FlowInstanceServiceFeignFallbackImpl();
        flowInstanceServiceFeignFallback.setCause(throwable);
        return flowInstanceServiceFeignFallback;
    }
}
