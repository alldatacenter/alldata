package datart.server.config;

import datart.core.common.RequestContext;
import datart.server.base.dto.ResponseData;
import datart.server.controller.BaseController;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.LinkedList;
import java.util.Map;

@ControllerAdvice
public class ControllerResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
//        Class<?> superclass = Objects.requireNonNull(methodParameter.getMethod()).getDeclaringClass().getSuperclass();
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object o, MethodParameter methodParameter, MediaType mediaType, Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        if (o instanceof ResponseData) {
            Map<String, Exception> warnings = RequestContext.getWarnings();
            if (warnings != null && warnings.size() > 0) {
                LinkedList<String> msg = new LinkedList<>();
                for (Exception value : warnings.values()) {
                    msg.add(value.toString());
                }
                ((ResponseData) o).setWarnings(msg);
            }
        }
        return o;
    }
}