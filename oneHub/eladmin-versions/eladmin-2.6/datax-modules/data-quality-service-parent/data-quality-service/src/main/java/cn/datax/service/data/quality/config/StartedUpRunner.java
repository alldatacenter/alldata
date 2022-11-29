package cn.datax.service.data.quality.config;

import cn.datax.common.core.DataConstant;
import cn.datax.service.data.quality.api.entity.ScheduleJobEntity;
import cn.datax.service.data.quality.service.ScheduleJobService;
import cn.datax.service.data.quality.schedule.CronTaskRegistrar;
import cn.datax.service.data.quality.schedule.SchedulingRunnable;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartedUpRunner implements ApplicationRunner {

    private final ConfigurableApplicationContext context;
    private final Environment environment;

    @Autowired
    private CronTaskRegistrar cronTaskRegistrar;

    @Autowired
    private ScheduleJobService scheduleJobService;

    @Override
    public void run(ApplicationArguments args) {
        if (context.isActive()) {
            String banner = "-----------------------------------------\n" +
                    "服务启动成功，时间：" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()) + "\n" +
                    "服务名称：" + environment.getProperty("spring.application.name") + "\n" +
                    "端口号：" + environment.getProperty("server.port") + "\n" +
                    "-----------------------------------------";
            System.out.println(banner);

            List<ScheduleJobEntity> list = scheduleJobService.list(Wrappers.<ScheduleJobEntity>lambdaQuery().eq(ScheduleJobEntity::getStatus, DataConstant.TrueOrFalse.TRUE.getKey()));
            if (CollUtil.isNotEmpty(list)) {
                list.forEach(job -> {
                    SchedulingRunnable task = new SchedulingRunnable(job.getId(), job.getBeanName(), job.getMethodName(), job.getMethodParams());
                    cronTaskRegistrar.addCronTask(task, job.getCronExpression());
                });
            }
            log.info("定时任务已加载完毕...");
        }
    }
}
