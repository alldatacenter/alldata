package cn.datax.service.data.market.integration.config;

import cn.datax.common.core.DataConstant;
import cn.datax.common.utils.IPUtil;
import cn.datax.common.utils.MD5Util;
import cn.datax.common.utils.RequestHolder;
import cn.datax.service.data.market.api.dto.ServiceExecuteDto;
import cn.datax.service.data.market.api.dto.ServiceLogDto;
import cn.datax.service.data.market.integration.async.AsyncTask;
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

@Slf4j
@Aspect
@Component
public class ServiceLogAspect {

    @Autowired
    private AsyncTask asyncTask;

    @Pointcut("execution(* cn.datax.service.data.market.integration.controller.ServiceExecuteController.execute(..))")
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
        ServiceLogDto log = getLog();
        log.setTime(endTime - startTime);
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
        ServiceLogDto log = getLog();
        log.setStatus(DataConstant.EnableState.DISABLE.getKey());
        log.setMsg(e.getMessage());
        handleLog(joinPoint, log);
    }

    private ServiceLogDto getLog() {
        ServiceLogDto log = new ServiceLogDto();
        HttpServletRequest request = RequestHolder.getHttpServletRequest();
        String serviceKey = request.getHeader("service_key");
        String secretKey = request.getHeader("secret_key");
        MD5Util mt = null;
        try {
            mt = MD5Util.getInstance();
            String serviceId = mt.decode(serviceKey);
            String userId = mt.decode(secretKey);
            log.setCallerId(userId);
            log.setServiceId(serviceId);
        } catch (Exception e) {}
        String ipAddr = IPUtil.getIpAddr(request);
        log.setCallerIp(ipAddr);
        log.setCallerDate(LocalDateTime.now());
        log.setStatus(DataConstant.EnableState.ENABLE.getKey());
        return log;
    }

    protected void handleLog(final JoinPoint joinPoint, ServiceLogDto log) {
        ServiceExecuteDto arg = (ServiceExecuteDto) joinPoint.getArgs()[0];
        log.setCallerSoap(arg.getSoap());
        log.setCallerHeader(arg.getHeader());
        log.setCallerParam(arg.getParam());
        asyncTask.doTask(log);
    }
}
