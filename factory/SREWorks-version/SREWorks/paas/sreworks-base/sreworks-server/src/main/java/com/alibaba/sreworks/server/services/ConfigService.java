package com.alibaba.sreworks.server.services;

import java.util.List;
import java.util.Objects;

import com.alibaba.sreworks.domain.DO.Config;
import com.alibaba.sreworks.domain.repository.ConfigRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigService {

    @Autowired
    ConfigRepository configRepository;

    public void set(String name, String content, String operator) {
        Config config = configRepository.findFirstByName(name);
        if (config == null) {
            config = Config.builder()
                .gmtCreate(System.currentTimeMillis() / 1000)
                .name(name)
                .build();
        }
        if (!Objects.equals(content, config.getContent())) {
            config.setGmtModified(System.currentTimeMillis() / 1000);
            config.setOperator(operator);
            config.setContent(content);
            configRepository.saveAndFlush(config);
        }
    }

    public String get(String name) {
        Config config = configRepository.findFirstByName(name);
        return config == null ? "" : config.getContent();
    }

    public Integer del(String name){
        return configRepository.deleteByName(name);
    }

    public List<Config> getAll() {
        return configRepository.findAll();
    }

}
