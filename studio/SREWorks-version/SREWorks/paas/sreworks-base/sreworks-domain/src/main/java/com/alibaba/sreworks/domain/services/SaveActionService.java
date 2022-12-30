package com.alibaba.sreworks.domain.services;

import com.alibaba.sreworks.domain.DO.Action;
import com.alibaba.sreworks.domain.repository.ActionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SaveActionService {

    @Autowired
    ActionRepository actionRepository;

    public Action save(String operator, String targetType, String targetValue, String content) {
        return actionRepository.saveAndFlush(Action.builder()
            .gmtCreate(System.currentTimeMillis() / 1000)
            .gmtModified(System.currentTimeMillis() / 1000)
            .operator(operator)
            .targetType(targetType)
            .targetValue(targetValue)
            .content(content)
            .build());
    }

    public Action save(String operator, String targetType, Long targetValue, String content) {
        return save(operator, targetType, String.valueOf(targetValue), content);
    }

}
