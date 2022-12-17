
package cn.datax.service.data.metadata.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author AllDataDC
 * @website https://eladmin.vip
 * @description
 * @date 2022-10-27
 **/
@Getter
@AllArgsConstructor
public enum RequestMethodEnum {

    /**
     * 搜寻 @AnonymousGetMapping
     */
    GET("GET"),

    /**
     * 搜寻 @AnonymousPostMapping
     */
    POST("POST"),

    /**
     * 搜寻 @AnonymousPutMapping
     */
    PUT("PUT"),

    /**
     * 搜寻 @AnonymousPatchMapping
     */
    PATCH("PATCH"),

    /**
     * 搜寻 @AnonymousDeleteMapping
     */
    DELETE("DELETE"),

    /**
     * 否则就是所有 Request 接口都放行
     */
    ALL("All");

    /**
     * Request 类型
     */
    private final String type;

    public static RequestMethodEnum find(String type) {
        for (RequestMethodEnum value : RequestMethodEnum.values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return ALL;
    }
}
