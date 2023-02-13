package cn.datax.service.data.market.mapping.handler;

import cn.datax.common.core.R;
import cn.datax.common.database.core.PageResult;
import cn.datax.service.data.market.api.entity.DataApiEntity;
import cn.datax.service.data.market.mapping.service.impl.ApiMappingEngine;
import cn.hutool.core.map.MapUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RequestHandler {

    private RequestInterceptor requestInterceptor;

    private ApiMappingEngine apiMappingEngine;

    private ObjectMapper objectMapper;

    public void setRequestInterceptor(RequestInterceptor requestInterceptor) {
        this.requestInterceptor = requestInterceptor;
    }

    public void setApiMappingEngine(ApiMappingEngine apiMappingEngine) {
        this.apiMappingEngine = apiMappingEngine;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @ResponseBody
    public Object invoke(HttpServletRequest request, HttpServletResponse response,
                         @PathVariable(required = false) Map<String, Object> pathVariables,
                         @RequestParam(required = false) Map<String, Object> requestParams,
                         @RequestBody(required = false) Map<String, Object> requestBodys) {
        DataApiEntity api;
        Map<String, Object> params = new HashMap<>();
        if (MapUtil.isNotEmpty(pathVariables)) {
            log.info("pathVariables:{}", pathVariables.toString());
            params.putAll(pathVariables);
        }
        if (MapUtil.isNotEmpty(requestParams)) {
            log.info("requestParams:{}", requestParams.toString());
            params.putAll(requestParams);
        }
        if (MapUtil.isNotEmpty(requestBodys)) {
            log.info("requestBodys:{}", requestBodys.toString());
            params.putAll(requestBodys);
        }
        api = MappingHandlerMapping.getMappingApiInfo(request);
        // 序列化
        api = objectMapper.readValue(objectMapper.writeValueAsString(api), DataApiEntity.class);
        // 执行前置拦截器
        requestInterceptor.preHandle(request, response, api, params);
        PageResult<Map<String, Object>> value = apiMappingEngine.execute(api, params);
        // 执行后置拦截器
        requestInterceptor.postHandle(request, response, api, params, value);
        return R.ok().setData(value);
    }
}
