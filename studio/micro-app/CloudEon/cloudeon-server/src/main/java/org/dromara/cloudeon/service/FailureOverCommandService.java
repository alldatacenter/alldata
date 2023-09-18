package org.dromara.cloudeon.service;

import lombok.extern.slf4j.Slf4j;
import org.dromara.cloudeon.dao.CommandRepository;
import org.dromara.cloudeon.dao.CommandTaskRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.transaction.Transactional;

/**
 * 重启后，将正在执行的指令都变成失败的
 */
@Component
@Slf4j
public class FailureOverCommandService implements ApplicationRunner {


    @Resource
    CommandRepository commandRepository;

    @Resource
    CommandTaskRepository commandTaskRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        commandRepository.updateRunningCommand2Error();
    }
}
