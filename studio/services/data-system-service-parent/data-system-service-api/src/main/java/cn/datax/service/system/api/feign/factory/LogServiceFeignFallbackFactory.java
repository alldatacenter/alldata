package cn.datax.service.system.api.feign.factory;

import cn.datax.service.system.api.feign.LogServiceFeign;
import cn.datax.service.system.api.feign.fallback.LogServiceFeignFallbackImpl;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class LogServiceFeignFallbackFactory implements FallbackFactory<LogServiceFeign> {

    @Override
    public LogServiceFeign create(Throwable throwable) {
        LogServiceFeignFallbackImpl logServiceFeignFallback = new LogServiceFeignFallbackImpl();
        logServiceFeignFallback.setCause(throwable);
        return logServiceFeignFallback;
    }
}
