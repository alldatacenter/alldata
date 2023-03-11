package cn.datax.service.system.api.feign;

import cn.datax.service.system.api.dto.JwtUserDto;
import cn.datax.service.system.api.feign.factory.UserServiceFeignFallbackFactory;
import cn.datax.service.system.api.vo.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(contextId = "userServiceFeign", value = "system", fallbackFactory = UserServiceFeignFallbackFactory.class)
public interface UserServiceFeign {
    @GetMapping("/api/users/{username}")
    JwtUserDto loginByUsername(@PathVariable("username") String username);
}
