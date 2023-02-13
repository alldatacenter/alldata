package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.server.repository.AppMarketRepository;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;
import com.alibaba.tesla.appmanager.server.repository.mapper.TagQueryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AppMarketRepositoryImpl implements AppMarketRepository {

    @Autowired
    private TagQueryMapper tagQueryMapper;

    @Override
    public List<AppPackageDO> queryAppPackage(String tag, Integer start, Integer limit) {
        return tagQueryMapper.queryAppPackageWithMaxVersion(tag, start, limit);
    }

    @Override
    public Integer countAppPackage(String tag) {
        return tagQueryMapper.countAppPackageWithMaxVersion(tag);
    }

    @Override
    public List<AppPackageDO> queryAppPackageWithOption(
            String tag, String key, String value, Integer start, Integer limit) {
        return tagQueryMapper.queryAppPackageWithMaxVersionAndOption(tag, key, value, start, limit);
    }

    @Override
    public Integer countAppPackageWithOption(String tag, String key, String value) {
        return tagQueryMapper.countAppPackageWithMaxVersionAndOption(tag, key, value);
    }
}
