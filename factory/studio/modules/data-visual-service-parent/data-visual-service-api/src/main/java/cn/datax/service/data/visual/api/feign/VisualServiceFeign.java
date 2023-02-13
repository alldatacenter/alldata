package cn.datax.service.data.visual.api.feign;


import cn.datax.service.data.visual.api.entity.DataSetEntity;
import cn.datax.service.data.visual.api.feign.factory.VisualServiceFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(contextId = "visualServiceFeign", value = "service-data-visual", fallbackFactory = VisualServiceFeignFallbackFactory.class)
public interface VisualServiceFeign {

	@GetMapping("/dataSets/source/{sourceId}")
	DataSetEntity getBySourceId(@PathVariable String sourceId);
}
