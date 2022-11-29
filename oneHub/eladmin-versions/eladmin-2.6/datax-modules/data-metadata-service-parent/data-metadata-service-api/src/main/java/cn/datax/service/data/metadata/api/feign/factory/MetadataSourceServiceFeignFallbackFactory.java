package cn.datax.service.data.metadata.api.feign.factory;

import cn.datax.service.data.metadata.api.feign.MetadataSourceServiceFeign;
import cn.datax.service.data.metadata.api.feign.fallback.MetadataSourceServiceFeignFallbackImpl;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class MetadataSourceServiceFeignFallbackFactory implements FallbackFactory<MetadataSourceServiceFeign> {

    @Override
    public MetadataSourceServiceFeign create(Throwable throwable) {
        MetadataSourceServiceFeignFallbackImpl metadataSourceServiceFeignFallback = new MetadataSourceServiceFeignFallbackImpl();
        metadataSourceServiceFeignFallback.setCause(throwable);
        return metadataSourceServiceFeignFallback;
    }
}
