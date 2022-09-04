package com.alibaba.sreworks.health.services.cache;

import com.alibaba.sreworks.health.cache.HealthDomainCache;
import com.alibaba.sreworks.health.cache.IncidentDefCache;
import com.alibaba.sreworks.health.cache.IncidentTypeCache;
import com.alibaba.sreworks.health.cache.RiskTypeCache;
import com.alibaba.sreworks.health.common.constant.Constant;
import com.alibaba.sreworks.health.domain.CommonDefinitionMapper;
import com.alibaba.sreworks.health.domain.IncidentTypeMapper;
import com.alibaba.sreworks.health.domain.RiskTypeMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/03 19:49
 */
@Slf4j
@Service
public class HealthDomainCacheService implements InitializingBean {
    private transient Cache<String, HealthDomainCache> domainCaches;

    @Autowired
    IncidentTypeMapper incidentTypeMapper;

    @Autowired
    RiskTypeMapper riskTypeMapper;

    @Autowired
    CommonDefinitionMapper commonDefinitionMapper;

    @Override
    public void afterPropertiesSet() {
        domainCaches = CacheBuilder.newBuilder().expireAfterAccess(Constant.CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS).build();
    }

    public HealthDomainCache getDomainCache(String key) {
        HealthDomainCache domainCache = domainCaches.getIfPresent(key);

        // 需要重建
        if (domainCache == null) {
            switch (key) {
                case Constant.CACHE_INCIDENT_TYPE_KEY:
                    domainCache = new IncidentTypeCache();
                    domainCache.reconstructCache(incidentTypeMapper);
                    break;
                case Constant.CACHE_RISK_TYPE_KEY:
                    domainCache = new RiskTypeCache();
                    domainCache.reconstructCache(riskTypeMapper);
                    break;
                case Constant.CACHE_INCIDENT_DEF_KEY:
                    domainCache = new IncidentDefCache();
                    domainCache.reconstructCache(commonDefinitionMapper);
                    break;
                default:
                    log.warn(String.format("不支持的缓存key:%s", key));
            }
            domainCaches.put(key, domainCache);
        }

        return domainCache;
    }

    public void expireKey(String key) {
        domainCaches.invalidate(key);
    }
}
