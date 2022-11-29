package cn.datax.service.data.market.mapping.config;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.R;
import cn.datax.common.database.core.PageResult;
import cn.datax.common.utils.IPUtil;
import cn.datax.common.utils.MD5Util;
import cn.datax.common.utils.RequestHolder;
import cn.datax.service.data.market.api.dto.ApiLogDto;
import cn.datax.service.data.market.mapping.async.AsyncTask;
import cn.hutool.core.map.MapUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
public class ApiLogAspect {

    @Autowired
    private AsyncTask asyncTask;

    @Pointcut("execution(* cn.datax.service.data.market.mapping.handler.RequestHandler.invoke(..))")
    public void logPointCut() {}

    /**
     * 通知方法会将目标方法封装起来
     *
     * @param joinPoint 切点
     */
    @Around(value = "logPointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long endTime = System.currentTimeMillis();
        ApiLogDto log = getLog();
        log.setTime(endTime - startTime);
        if (result instanceof R) {
            if (((R) result).getData() instanceof PageResult) {
                log.setCallerSize(((PageResult) ((R) result).getData()).getData().size());
            }
        }
        handleLog(joinPoint, log);
        return result;
    }

    /**
     * 通知方法会在目标方法抛出异常后执行
     *
     * @param joinPoint
     * @param e
     */
    @AfterThrowing(value = "logPointCut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        log.error("出错{}", e.getMessage());
        ApiLogDto log = getLog();
        log.setStatus(DataConstant.EnableState.DISABLE.getKey());
        log.setMsg(e.getMessage());
        handleLog(joinPoint, log);
    }

    private ApiLogDto getLog() {
        ApiLogDto log = new ApiLogDto();
        HttpServletRequest request = RequestHolder.getHttpServletRequest();
        String apiKey = request.getHeader("api_key");
        String secretKey = request.getHeader("secret_key");
        try {
            MD5Util mt = MD5Util.getInstance();
            String apiId = mt.decode(apiKey);
            String userId = mt.decode(secretKey);
            log.setCallerId(userId);
            log.setApiId(apiId);
        } catch (Exception e) {}
        String uri = request.getRequestURI();
        log.setCallerUrl(uri);
        String ipAddr = IPUtil.getIpAddr(request);
        log.setCallerIp(ipAddr);
        log.setCallerDate(LocalDateTime.now());
        log.setStatus(DataConstant.EnableState.ENABLE.getKey());
        return log;
    }

    protected void handleLog(final JoinPoint joinPoint, ApiLogDto log) {
        Map<String, Object> pathVariables = (Map<String, Object>) joinPoint.getArgs()[2];
        Map<String, Object> requestParams = (Map<String, Object>) joinPoint.getArgs()[3];
        Map<String, Object> requestBodys = (Map<String, Object>) joinPoint.getArgs()[4];
        Map<String, Object> params = new HashMap<>();
        if (MapUtil.isNotEmpty(pathVariables)) {
            params.putAll(pathVariables);
        }
        if (MapUtil.isNotEmpty(requestParams)) {
            params.putAll(requestParams);
        }
        if (MapUtil.isNotEmpty(requestBodys)) {
            params.putAll(requestBodys);
        }
        try {
            log.setCallerParams(new ObjectMapper().writeValueAsString(params));
        } catch (JsonProcessingException e) {}
        asyncTask.doTask(log);
    }
}
