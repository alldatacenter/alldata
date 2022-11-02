package com.alibaba.tesla.gateway.server.scheduler;

import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import com.alibaba.tesla.gateway.server.config.properties.TeslaGatewayProperties;
import com.alibaba.tesla.gateway.server.domain.SwitchViewUser;
import com.alibaba.tesla.gateway.server.service.SwitchViewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时查询diamond中的白名单
 *
 * @author jaxon.hyj@alibaba-inc.com
 */
@Component
@Slf4j
public class SwitchViewScheduler {

    @Autowired
    private SwitchViewService switchViewService;

    /**
     * 白名单集合
     */
    private List<SwitchViewUser> whiteList;

    @Autowired
    private TeslaGatewayProperties teslaGatewayProperties;

    /**
     * 启动后执行一次初始化
     */
    @PostConstruct
    public void init() {
        run();
    }

    /**
     * 每 10 分钟执行一次
     */
    @Scheduled(cron = "0 0/10 * * * ? ")
    public void run() {
        try {
            if (this.teslaGatewayProperties.isEnableSwitchView()) {
                List<SwitchViewUser> users = switchViewService.getWhiteListUsers().toStream().collect(Collectors.toList());
                log.info("action=scheduler.whiteList||message=get whiteList from authproxy: {}", TeslaGsonUtil.toJson(users));
                whiteList = users;
            }
        }catch (Exception e){
            log.error("get witheList user from auth proxy failed", e);
        }

    }

    /**
     * 检查指定 用户 是否存在于白名单中
     *
     * @param user 用户信息
     * @return true or false
     */
    public boolean isInWhiteList(SwitchViewUser user) {
        return whiteList.contains(user);
    }
}