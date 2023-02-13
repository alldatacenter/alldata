package cn.datax.service.data.metadata.api.feign;

import cn.datax.service.data.metadata.api.entity.MetadataSourceEntity;
import cn.datax.service.data.metadata.api.feign.factory.MetadataSourceServiceFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(contextId = "metadataSourceServiceFeign", value = "service-data-metadata", fallbackFactory = MetadataSourceServiceFeignFallbackFactory.class)
public interface MetadataSourceServiceFeign {

    @GetMapping("/inner/sources/{id}")
    MetadataSourceEntity getMetadataSourceById(@PathVariable("id") String id);

    @GetMapping("/inner/sources/list")
    List<MetadataSourceEntity> getMetadataSourceList();
}
