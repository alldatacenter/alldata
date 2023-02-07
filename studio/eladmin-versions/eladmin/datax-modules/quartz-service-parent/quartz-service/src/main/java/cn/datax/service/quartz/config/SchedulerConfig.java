package cn.datax.service.quartz.config;

import cn.datax.service.quartz.quartz.utils.ScheduleUtil;
import org.quartz.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class SchedulerConfig {

    /**
     * 注入scheduler到spring
     * @param factoryBean
     * @return Scheduler
     * @throws Exception
     */
    @Bean(name = "scheduler")
    public Scheduler scheduler(SchedulerFactoryBean factoryBean) throws Exception {
        Scheduler scheduler = factoryBean.getScheduler();
        scheduler.start();
        // 设置ScheduleUtil的定时处理对象
        ScheduleUtil.setScheduler(scheduler);
        return scheduler;
    }
}
