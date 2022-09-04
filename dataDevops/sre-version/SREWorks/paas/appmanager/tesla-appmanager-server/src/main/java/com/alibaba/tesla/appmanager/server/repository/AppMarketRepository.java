package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;

import java.util.List;

public interface AppMarketRepository {

    List<AppPackageDO> queryAppPackage(String tag, Integer start, Integer limit);

    Integer countAppPackage(String tag);

    List<AppPackageDO> queryAppPackageWithOption(
            String tag, String key, String value, Integer start, Integer limit);

    Integer countAppPackageWithOption(String tag, String key, String value);
}
