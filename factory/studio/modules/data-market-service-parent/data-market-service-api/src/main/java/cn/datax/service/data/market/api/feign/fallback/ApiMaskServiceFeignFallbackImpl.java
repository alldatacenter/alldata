package cn.datax.service.data.market.api.feign.fallback;

import cn.datax.service.data.market.api.entity.ApiMaskEntity;
import cn.datax.service.data.market.api.feign.ApiMaskServiceFeign;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApiMaskServiceFeignFallbackImpl implements ApiMaskServiceFeign {

    @Setter
    private Throwable cause;

    @Override
    public ApiMaskEntity getApiMaskByApiId(String id) {
        log.error("feign 调用{}出错", id, cause);
        return null;
    }
}
