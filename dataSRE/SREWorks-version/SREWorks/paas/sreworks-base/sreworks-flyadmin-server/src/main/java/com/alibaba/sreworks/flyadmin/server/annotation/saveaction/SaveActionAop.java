//package com.alibaba.sreworks.flyadmin.server.annotation.saveaction;
//
//import java.lang.reflect.Method;
//import java.util.Map;
//
//import javax.servlet.http.HttpServletRequest;
//
//import com.alibaba.fastjson.JSONObject;
//import com.alibaba.sreworks.common.util.StringUtil;
//import com.alibaba.sreworks.domain.DO.Action;
//import com.alibaba.sreworks.domain.repository.ActionRepository;
//import com.alibaba.sreworks.flyadmin.server.annotation.BasicAop;
//import com.alibaba.tesla.web.constant.HttpHeaderNames;
//
//import lombok.extern.slf4j.Slf4j;
//import org.aspectj.lang.JoinPoint;
//import org.aspectj.lang.annotation.AfterReturning;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.annotation.Before;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.web.context.request.RequestAttributes;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//
///**
// * 计算方法调用时间切面
// *
// * @author yangjinghua
// */
//@Aspect
//@Component
//@Slf4j
//public class SaveActionAop extends BasicAop {
//
//    @Autowired
//    ActionRepository actionRepository;
//
//    private Action action;
//
//    private String getUserEmpId() {
//        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
//        ServletRequestAttributes sra = (ServletRequestAttributes)ra;
//        HttpServletRequest request = sra.getRequest();
//        return request.getHeader(HttpHeaderNames.X_EMPL_ID);
//    }
//
//    @Before("@annotation(com.alibaba.sreworks.flyadmin.server.annotation.saveaction.SaveAction)")
//    public void before(JoinPoint joinPoint) throws NoSuchMethodException, ClassNotFoundException {
//
//        Method method = getObjMethod(joinPoint);
//        SaveAction saveAction = method.getAnnotation(SaveAction.class);
//        Map<String, Object> params = getParams(joinPoint);
//        JSONObject paramsJson = JSONObject.parseObject(JSONObject.toJSONString(params));
//        action = new Action(getUserEmpId(), saveAction.targetType(), saveAction.content());
//        if (!StringUtil.isEmpty(saveAction.paramsTargetValue())) {
//            action.setTargetValue(paramsJson.getString(saveAction.paramsTargetValue()));
//        }
//        actionRepository.saveAndFlush(action);
//
//    }
//
//    @AfterReturning(
//        returning = "rvt",
//        pointcut = "@annotation(com.alibaba.sreworks.flyadmin.server.annotation.saveaction.SaveAction)"
//    )
//    public Object afterReturn(JoinPoint joinPoint, Object rvt) throws NoSuchMethodException, ClassNotFoundException {
//        Method method = getObjMethod(joinPoint);
//        SaveAction saveAction = method.getAnnotation(SaveAction.class);
//        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(rvt));
//        if (!StringUtil.isEmpty(saveAction.returnTargetValue())) {
//            action.setTargetValue(
//                jsonObject.getString(saveAction.returnTargetValue())
//            );
//            actionRepository.saveAndFlush(action);
//        }
//        return rvt;
//    }
//
//}
//
