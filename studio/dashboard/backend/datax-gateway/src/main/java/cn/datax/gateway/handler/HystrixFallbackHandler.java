package cn.datax.gateway.handler;

import cn.datax.common.core.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Optional;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR;

/**
 * Hystrix 降级处理 网关请求错误重定向到fallback 再到这里
 */
@Slf4j
@Component
public class HystrixFallbackHandler implements HandlerFunction<ServerResponse> {

    @Override
    public Mono<ServerResponse> handle(ServerRequest serverRequest) {
        //得到原始的请求的url
        Optional<Object> originalUris = serverRequest.attribute(GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
        //如果这个urls里面有东西
        originalUris.ifPresent(originalUri -> log.error("网关执行请求:{}失败，hystrix服务降级处理", originalUri));
        //返回空的response
        return ServerResponse
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(R.error("服务超时或者服务不可用,请稍后重试")));
    }
}
