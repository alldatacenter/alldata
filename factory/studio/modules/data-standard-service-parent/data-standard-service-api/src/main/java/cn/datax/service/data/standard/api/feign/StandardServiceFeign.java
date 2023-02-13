package cn.datax.service.data.standard.api.feign;

import cn.datax.service.data.standard.api.entity.ContrastEntity;
import cn.datax.service.data.standard.api.feign.factory.StandardServiceFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(contextId = "standardServiceFeign", value = "service-data-standard", fallbackFactory = StandardServiceFeignFallbackFactory.class)
public interface StandardServiceFeign {

	@GetMapping("/contrasts/source/{sourceId}")
	ContrastEntity getBySourceId(@PathVariable String sourceId);
}
