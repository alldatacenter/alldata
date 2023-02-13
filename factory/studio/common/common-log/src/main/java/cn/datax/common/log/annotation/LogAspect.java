package cn.datax.common.log.annotation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.platform.utils.RequestHolder;
import cn.datax.service.system.api.dto.LogDto;
import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.utils.SecurityUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
public class LogAspect {

//    @Autowired
//    private LogServiceFeign logServiceFeign;

    @Autowired
    private ObjectMapper objectMapper;

    // 配置织入点
    @Pointcut("@annotation(cn.datax.common.log.annotation.LogAop)")
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
        LogDto logDto = getLog();
        logDto.setTime(String.valueOf(endTime - startTime));
        handleLog(joinPoint, logDto);
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
        LogDto logDto = getLog();
        logDto.setExCode(e.getClass().getSimpleName()).setExMsg(e.getMessage());
        handleLog(joinPoint, logDto);
    }

    private LogDto getLog(){
        LogDto log = new LogDto();
        HttpServletRequest request = RequestHolder.getHttpServletRequest();
        log.setUserId(SecurityUtil.getUserId());
        log.setUserName(SecurityUtil.getUserName());
        log.setRemoteAddr(ServletUtil.getClientIP(request));
        log.setRequestUri(URLUtil.getPath(request.getRequestURI()));
        UserAgent ua = UserAgentUtil.parse(request.getHeader("User-Agent"));
        log.setBrowser(ua.getBrowser().toString());
        log.setOs(ua.getOs().toString());
        return log;
    }

    protected void handleLog(final JoinPoint joinPoint, LogDto logDto) {
        // 获得注解
        LogAop logAop = getAnnotationLog(joinPoint);
        if(null == logAop) {
            return;
        }
        // 设置方法名称
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        logDto.setModule(logAop.module()).setTitle(logAop.value())
                .setClassName(className).setMethodName(methodName);
        try {
            logDto.setParams(objectMapper.writeValueAsString(getRequestParams(joinPoint)));
        } catch (JsonProcessingException e) {}
        // 保存数据库
//        logServiceFeign.saveLog(logDto);
    }

    /**
     * 是否存在注解，如果存在就获取
     */
    private LogAop getAnnotationLog(JoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();
        return null;
    }

    /**
     * 获取入参
     * */
    private Map<String, Object> getRequestParams(JoinPoint joinPoint) {
        Map<String, Object> requestParams = new HashMap<>();
        // 参数名
        String[] paramNames = ((MethodSignature)joinPoint.getSignature()).getParameterNames();
        // 参数值
        Object[] paramValues = joinPoint.getArgs();
        for (int i = 0; i < paramNames.length; i++) {
            Object value = paramValues[i];
            // 如果是文件对象
            if (value instanceof MultipartFile) {
                MultipartFile file = (MultipartFile) value;
                // 获取文件名
                value = file.getOriginalFilename();
            }
            requestParams.put(paramNames[i], value);
        }
        return requestParams;
    }
}

