package cn.datax.service.workflow.api.feign;

import cn.datax.service.workflow.api.dto.ProcessInstanceCreateRequest;
import cn.datax.service.workflow.api.feign.factory.FlowInstanceServiceFeignFallbackFactory;
import cn.datax.service.workflow.api.vo.FlowInstanceVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(contextId = "flowInstanceServiceFeign", value = "service-workflow", fallbackFactory = FlowInstanceServiceFeignFallbackFactory.class)
public interface FlowInstanceServiceFeign {

    @PostMapping("/instances/startById")
    FlowInstanceVo startById(@RequestBody ProcessInstanceCreateRequest request);
}
