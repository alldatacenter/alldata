package com.alibaba.tesla.tkgone.server.services.config;

import com.alibaba.tesla.tkgone.server.common.Cache;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.domain.ConfigExample;
import com.alibaba.tesla.tkgone.server.domain.ConfigMapper;
import com.alibaba.tesla.tkgone.server.domain.dto.ConfigDto;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基础配置为一张表：category, type(type), id, name 四个字段 提供对基础配置的 增、删、改和查 功能 值为content 修改人为modifier
 *
 * @author yangjinghua
 */
@Service
@Log4j
public class ConfigDaemonService implements InitializingBean {

    @Autowired
    public ConfigMapper configMapper;

    @Override
    public void afterPropertiesSet() {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(this::cacheAllConfig, 0, 1, TimeUnit.SECONDS);
    }

    private void cacheAllConfig() {
        try {
            //缓存所有的配置
            Map<String, List<ConfigDto>> allConfigDto = new HashMap<>(8);
            List<ConfigDto> configDtoList = configMapper.selectByExample(new ConfigExample()).stream()
                .map(ConfigDto::new).collect(Collectors.toList());
            for (ConfigDto configDto : configDtoList) {
                for (String key : Tools.combination(Arrays.asList(
                    configDto.getCategory(), configDto.getNrType(), configDto.getNrId(), configDto.getName()))) {
                    if (!allConfigDto.containsKey(key)) {
                        allConfigDto.put(key, new ArrayList<>());
                    }
                    allConfigDto.get(key).add(configDto);
                }
            }
            Cache.allConfigDto = allConfigDto;
        } catch (Exception e) {
            log.error("cacheAllConfig ERROR: ", e);
        }
    }

}
