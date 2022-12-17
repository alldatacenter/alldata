package cn.datax.service.system.api.feign.fallback;

import cn.datax.service.system.api.dto.JwtUserDto;
import cn.datax.service.system.api.feign.UserServiceFeign;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserServiceFeignFallbackImpl implements UserServiceFeign {

    @Setter
    private Throwable cause;

    @Override
    public JwtUserDto loginByUsername(String username) {
        log.error("feign 调用{}出错", username, cause);
        return null;
    }
}
