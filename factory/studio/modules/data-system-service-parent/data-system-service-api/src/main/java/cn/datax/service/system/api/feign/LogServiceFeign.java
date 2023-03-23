package cn.datax.service.system.api.feign;

import cn.datax.service.system.api.dto.LogDto;
import cn.datax.service.system.api.feign.factory.LogServiceFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(contextId = "logServiceFeign", value = "datax-service-system", fallbackFactory = LogServiceFeignFallbackFactory.class)
public interface LogServiceFeign {

    @PostMapping("/inner/logs")
    void saveLog(@RequestBody LogDto logDto);
}
