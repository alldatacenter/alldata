package com.alibaba.tesla.appmanager.server.job;

import com.alibaba.tesla.appmanager.common.util.CommandUtil;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 清理 AppManager 本机存储镜像作业
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class CleanDockerImageJob {

    @Scheduled(cron = "${appmanager.cron-job.clean-docker-image:-}")
    @SchedulerLock(name = "cleanDockerImage")
    public void execute() {
        String command = "docker image prune -a --force --filter \"until=168h\"";
        String output = CommandUtil.runLocalCommand(command);
        log.info("docker images have pruned, output={}", output);
    }
}
