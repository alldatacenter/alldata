package cn.datax.service.data.quality.api.feign;


import cn.datax.service.data.quality.api.entity.CheckRuleEntity;
import cn.datax.service.data.quality.api.feign.factory.QualityServiceFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(contextId = "qualityServiceFeign", value = "service-data-quality", fallbackFactory = QualityServiceFeignFallbackFactory.class)
public interface QualityServiceFeign {

	@GetMapping("/checkRules/source/{sourceId}")
	CheckRuleEntity getBySourceId(@PathVariable String sourceId);
}
