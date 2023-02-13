package com.alibaba.tesla.gateway.server.cache;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.gateway.domain.LoginUserInfo;
import com.alibaba.tesla.gateway.domain.dto.RouteInfoDTO;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 网关服务缓存
 * 1、登录用户信息缓存
 * 2、路由定义缓存
 * 3、实际路由缓存
 *
 * @author tandong.td@alibaba-inc.com
 */
@Slf4j
@Component
public class GatewayCache {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 用户信息缓存过期时间
     * 默认为1天
     */
    private static final Long USER_CACHE_TIMEOUT_SECONDS = TimeUnit.DAYS.toSeconds(1);

    /**
     * 内存路由信息
     */
    private Map<String , Route> routeMap = new ConcurrentHashMap<>();

    /**
     * 配置路由信息
     */
    private Map<String, RouteInfoDO> routeInfoMap = new ConcurrentHashMap<>();

    /**
     * 校验白名单，校验失败也让过
     */
    private Map<String, List<String>> authWhiteMap = new ConcurrentHashMap<>();

    public Map<String , Route> getRoutes(){
        return this.routeMap;
    }

    /**
     * 根据路由id获取当前路由的白名单
     * @param routeId routeId
     * @return white list
     */
    public List<String> getAuthWhiteListByRouteId(String routeId){
        return authWhiteMap.get(routeId);
    }

    /**
     * 更新白名单
     * @param whiteList
     */
    public void updateWhiteList(Map<String, List<String>>  whiteList){
        if(CollectionUtils.isEmpty(whiteList)){
            return;
        }
        List<String> oldWhiteList = new ArrayList<>(this.authWhiteMap.keySet());
        List<String> newWhiteList = new ArrayList<>(whiteList.keySet());
        for (Map.Entry<String, List<String>> entry : whiteList.entrySet()) {
            this.authWhiteMap.put(entry.getKey(), entry.getValue());
        }
        oldWhiteList.removeAll(newWhiteList);
        if(!CollectionUtils.isEmpty(oldWhiteList)){
            for (String routeId : oldWhiteList) {
                this.authWhiteMap.remove(routeId);
            }
        }
    }

    public Map<String, RouteInfoDO> getRouteInfos(){
        return this.routeInfoMap;
    }


    public Route getRouteByRouteId(String routeId){
        return routeMap.get(routeId);
    }

    public RouteInfoDO getRouteInfoByRouteId(String routeId){
        return routeInfoMap.get(routeId);
    }

    public void updateRouteInfoCache(List<RouteInfoDO> routeInfos){
        if(CollectionUtils.isEmpty(routeInfos)){
            return;
        }
        for (RouteInfoDO routeInfo : routeInfos) {
            this.routeInfoMap.put(routeInfo.getRouteId(), routeInfo);
        }
        List<String> oldRouteIds = new ArrayList<>(routeInfoMap.keySet());
        List<String> newRouteIds = routeInfos.stream().map(RouteInfoDO::getRouteId).collect(Collectors.toList());
        oldRouteIds.removeAll(newRouteIds);
        if(!CollectionUtils.isEmpty(oldRouteIds)){
            for (String oldRouteId : oldRouteIds) {
                this.routeInfoMap.remove(oldRouteId);
            }
        }

    }

    public void updateRouteCache(List<Route> routes){
        if(CollectionUtils.isEmpty(routes)){
            return;
        }
        for (Route route : routes) {
            this.routeMap.put(route.getId(), route);
        }
        List<String> oldRouteIds = new ArrayList<>(routeMap.keySet());
        List<String> newRouteIds = routes.stream().map(Route::getId).collect(Collectors.toList());
        oldRouteIds.removeAll(newRouteIds);
        if(!CollectionUtils.isEmpty(oldRouteIds)){
            for (String oldRouteId : oldRouteIds) {
                this.routeMap.remove(oldRouteId);
            }
        }
    }



    /**
     * 将登录用户信息保存在缓存中
     * @param key key
     * @param userInfo {@link LoginUserInfo}
     */
    public void storeLoginUser(String key, LoginUserInfo userInfo){
        if(StringUtils.isEmpty(key)){
            return;
        }
        this.stringRedisTemplate.opsForValue().set(key, JSONObject.toJSONString(userInfo), USER_CACHE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 从缓存中获取登录用户
     * @param key key
     * @return
     */
    public LoginUserInfo getLoginUser(String key){
        if(StringUtils.isEmpty(key)){
            return null;
        }
        //todo 方法级别本地缓存
        String loginUserJson = stringRedisTemplate.opsForValue().get(key);
        return JSONObject.parseObject(loginUserJson, LoginUserInfo.class);
    }
}
