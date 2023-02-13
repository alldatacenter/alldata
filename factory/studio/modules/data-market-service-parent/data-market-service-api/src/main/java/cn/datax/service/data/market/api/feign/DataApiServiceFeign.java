package cn.datax.service.data.market.api.feign;

import cn.datax.service.data.market.api.entity.DataApiEntity;
import cn.datax.service.data.market.api.feign.factory.DataApiServiceFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(contextId = "dataApiServiceFeign", value = "service-data-market", fallbackFactory = DataApiServiceFeignFallbackFactory.class)
public interface DataApiServiceFeign {

    @GetMapping("/inner/apis/{id}")
    DataApiEntity getDataApiById(@PathVariable("id") String id);

	@GetMapping("/dataApis/source/{sourceId}")
	DataApiEntity getBySourceId(@PathVariable String sourceId);

    @GetMapping("/inner/apis/release/list")
    List<DataApiEntity> getReleaseDataApiList();
}
