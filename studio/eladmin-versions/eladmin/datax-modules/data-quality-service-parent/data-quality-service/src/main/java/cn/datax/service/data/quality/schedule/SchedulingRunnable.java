package cn.datax.service.data.quality.schedule;

import cn.datax.common.utils.SpringContextHolder;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class SchedulingRunnable implements Runnable {

    private String id;

    private String beanName;

    private String methodName;

    private String params;

    public SchedulingRunnable(String id, String beanName, String methodName, String params) {
        this.id = id;
        this.beanName = beanName;
        this.methodName = methodName;
        this.params = params;
    }

    @Override
    public void run() {
        log.info("定时任务开始执行 - id：{}，bean：{}，方法：{}，参数：{}", id, beanName, methodName, params);
        long startTime = System.currentTimeMillis();
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        String batch;
        try {
            Object target = SpringContextHolder.getBean(beanName);
            Method method = target.getClass().getDeclaredMethod(methodName, Map.class);
            if (StrUtil.isNotEmpty(params)) {
                map.putAll(new ObjectMapper().readValue(params, Map.class));
            }
            batch = DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_PATTERN);
            map.put("batch", batch);
            ReflectionUtils.makeAccessible(method);
            method.invoke(target, map);
        } catch (Exception ex) {
            log.error(String.format("定时任务执行异常 - id：%s，bean：%s，方法：%s，参数：%s ", id, beanName, methodName, params), ex);
        }
        long times = System.currentTimeMillis() - startTime;
        log.info("定时任务执行结束 - id：{}，bean：{}，方法：{}，参数：{}，耗时：{} 毫秒", id, beanName, methodName, params, times);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SchedulingRunnable that = (SchedulingRunnable) o;
        if (params == null) {
            return id.equals(that.id) &&
                    beanName.equals(that.beanName) &&
                    methodName.equals(that.methodName) &&
                    that.params == null;
        }
        return id.equals(that.id) &&
                beanName.equals(that.beanName) &&
                methodName.equals(that.methodName) &&
                params.equals(that.params);
    }

    @Override
    public int hashCode() {
        if (params == null) {
            return Objects.hash(id, beanName, methodName);
        }
        return Objects.hash(id, beanName, methodName, params);
    }
}
