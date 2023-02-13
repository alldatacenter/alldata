package com.alibaba.tesla.appmanager.server.service.appmarket.impl;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.server.repository.AppMarketRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppMarketQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;
import com.alibaba.tesla.appmanager.server.service.appmarket.AppMarketService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

/**
 * 应用市场服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class AppMarketServiceImpl implements AppMarketService {

    @Autowired
    private AppMarketRepository appMarketRepository;

    /**
     * 根据条件过滤应用包列表
     *
     * @param condition 过滤条件
     * @return List
     */
    @Override
    public Pagination<AppPackageDO> list(AppMarketQueryCondition condition) {
        String tag = condition.getTag();
        Integer start = Pagination.parseStart(condition.getPage(), condition.getPageSize());
        Integer limit = condition.getPageSize();
        String optionKey = condition.getOptionKey();
        String optionValue = condition.getOptionValue();
        List<AppPackageDO> packages;
        Integer count;
        if (StringUtils.isAnyEmpty(optionKey, optionValue)) {
            packages = appMarketRepository.queryAppPackage(tag, start, limit);
            count = appMarketRepository.countAppPackage(tag);
        } else {
            packages = appMarketRepository.queryAppPackageWithOption(tag, optionKey, optionValue, start, limit);
            count = appMarketRepository.countAppPackageWithOption(tag, optionKey, optionValue);
        }
        Pagination<AppPackageDO> result = Pagination.valueOf(packages, Function.identity());
        result.setTotal(count);
        return result;
    }
}
