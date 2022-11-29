package cn.datax.service.workflow.api.feign.fallback;

import cn.datax.service.workflow.api.dto.ProcessInstanceCreateRequest;
import cn.datax.service.workflow.api.feign.FlowInstanceServiceFeign;
import cn.datax.service.workflow.api.vo.FlowInstanceVo;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FlowInstanceServiceFeignFallbackImpl implements FlowInstanceServiceFeign {

    @Setter
    private Throwable cause;

    @Override
    public FlowInstanceVo startById(ProcessInstanceCreateRequest request) {
        log.error("feign 调用{}出错", request, cause);
        return null;
    }
}
