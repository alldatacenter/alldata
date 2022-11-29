package cn.datax.service.data.metadata.api.feign.fallback;

import cn.datax.service.data.metadata.api.entity.MetadataSourceEntity;
import cn.datax.service.data.metadata.api.feign.MetadataSourceServiceFeign;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class MetadataSourceServiceFeignFallbackImpl implements MetadataSourceServiceFeign {

    @Setter
    private Throwable cause;

    @Override
    public MetadataSourceEntity getMetadataSourceById(String id) {
        log.error("feign 调用{}出错", id, cause);
        return null;
    }

    @Override
    public List<MetadataSourceEntity> getMetadataSourceList() {
        log.error("feign 调用出错", cause);
        return null;
    }
}
