package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.model.example.TeslaServiceExtAppExample;
import com.alibaba.tesla.authproxy.model.example.TeslaServiceUserExample;

import org.springframework.scheduling.annotation.Async;

/**
 * @author cdx
 * @date 2019/10/10
 */
@Async
public interface SyncService {

    void syncAppByExample(TeslaServiceExtAppExample teslaServiceExtAppExample);

    void syncUserByExample(TeslaServiceUserExample example);
}
