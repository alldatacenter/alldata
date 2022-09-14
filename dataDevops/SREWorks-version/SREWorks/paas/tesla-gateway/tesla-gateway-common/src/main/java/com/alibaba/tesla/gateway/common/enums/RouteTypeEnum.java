package com.alibaba.tesla.gateway.common.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * 路由类别
 * @author qiuqiang.qq@alibaba-inc.com
 */
public enum RouteTypeEnum {

    /**
     * path方式， 默认方式
     */
    PATH,

    /**
     * host方式，可指定域名来转发
     */
    HOST;

    /**
     * string -> enum
     * @param name type
     * @return type enum
     */
    public static RouteTypeEnum toTypeEnum(String name){
        if(StringUtils.isBlank(name)){
            return PATH;
        }
        return RouteTypeEnum.valueOf(name);
    }

    /**
     * enum to string
     * @param typeEnum enum
     * @return string
     */
    public static String toType(RouteTypeEnum typeEnum){
        if(typeEnum == null){
            return PATH.name();
        }
        return typeEnum.name();
    }
}
