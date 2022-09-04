package com.alibaba.tesla.authproxy.scheduler.elasticjob;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * ElasticJob 调度器选择加载
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class ElasticjobCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        return null != env && "elasticjob".equals(env.getProperty("tesla.schedulerEngine"));
    }

}
